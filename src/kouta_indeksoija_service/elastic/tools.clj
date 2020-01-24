(ns kouta-indeksoija-service.elastic.tools
  (:require [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.util.tools :refer [get-id]]
            [clj-elasticsearch.elastic-connect :as e]
            [clj-elasticsearch.elastic-utils :as u]
            [cheshire.core :as cheshire]
            [environ.core]
            [clojure.tools.logging :as log]))

(defn init-elastic-client []
  (intern 'clj-elasticsearch.elastic-utils 'elastic-host (:elastic-url env)))

(defn index-name
  [name]
  (u/index-name name (Boolean/valueOf (:test environ.core/env))))

(defn handle-exception
  [e]
  (if-let [ex-d (ex-data e)]
    (log/error "Got status " (:status ex-d) " from " (:trace-redirects ex-d) " with body " (:body ex-d))
    (log/error "Got exception " e))
  nil)

(defn refresh-index
  [index]
  (try
    (e/refresh-index (index-name index))
    (catch Exception e
      (handle-exception e))))

(defn delete-index
  [index]
  (e/delete-index (index-name index)))

(defn get-by-id
  [index type id]
  (try
    (-> (e/get-document (index-name index) (index-name type) id)
        (:_source))
    (catch Exception e
      (handle-exception e))))

(defn get-doc
  [type id]
  (get-by-id type type id))

(defrecord BulkAction [action id doc])

(defn ->index-action
  [id doc]
  (map->BulkAction {:action "index" :id id :doc doc}))

(defn ->delete-action
  [id]
  (map->BulkAction {:action "delete" :id id :doc nil}))

(defn- bulk-action
  [index action]
  {(keyword (:action action)) {:_index index :_type index :_id (:id action)}})

(defn- bulk-doc
  [doc time]
  (assoc (dissoc doc :_index :_type) :timestamp time))

(defn ->bulk-actions
  [index actions]
  (let [true-index (index-name index)
        time       (System/currentTimeMillis)]
    (-> (for [action actions]
          (let [bulk-action (bulk-action true-index action)
                bulk-doc    (when-let [doc (:doc action)] (bulk-doc doc time))]
            (remove nil? [bulk-action bulk-doc])))
        (flatten))))

(defn log-and-get-bulk-errors
  [response]
  (let [simplify (fn [x] (let [map-entry (first x)]
                           (assoc (val map-entry) :action (name (key map-entry)))))
        not-ok-results (->> response
                            (mapcat :items)
                            (map simplify)
                            (filter #(> (:status %) 299)))
        errors (group-by #(select-keys % [:action :status :error :result :_index]) not-ok-results)]
    (doseq [error (keys errors)]
      (log/error (str "Bulk action '" (:action error) "' to index '" (:_index error) "' failed with status '" (:status error) "' and error '" (or (:error error) (:result error)) "' for (o)ids " (vec (map :_id (get errors error))))))
    not-ok-results))

(defn- execute-bulk-actions
  [index actions]
  (let [data   (->bulk-actions index actions)
        res    (e/bulk index index data)]
    (log-and-get-bulk-errors res)))

(defn bulk
  [index actions]
  (try
    (execute-bulk-actions index actions)
    (catch Exception e
      (handle-exception e)
      (vec (map :id actions)))))