(ns kouta-indeksoija-service.elastic.tools
  (:require [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.util.tools :refer [get-id]]
            [clj-elasticsearch.elastic-connect :as e]
            [clj-elasticsearch.elastic-utils :as u]
            [kouta-indeksoija-service.util.time :as time]
            [clj-time.format :as format]
            [environ.core]
            [clojure.string :as c-str]
            [clojure.tools.logging :as log]))

(defn init-elastic-client []
  (intern 'clj-elasticsearch.elastic-utils 'elastic-host (:elastic-url env)))

(defn ->virkailija-alias
  [index-name]
  (str index-name "-virkailija"))

(defn ->oppija-alias
  [index-name]
  index-name)

(defn virkailija-alias?
  [alias]
  (c-str/ends-with? alias "-virkailija"))

(defn oppija-alias?
  [alias]
  (not (virkailija-alias? alias)))

(defonce index-time-postfix-formatter (format/formatter "dd-MM-yyyy-'at'-HH.mm.ss.SSS"))

(defn ->raw-index-name
  ([index-name postfix]
   (str index-name "-" postfix))
  ([index-name]
   (->raw-index-name index-name (time/long->date-time-string (System/currentTimeMillis) index-time-postfix-formatter))))

(defn raw-index-name->index-name
  [raw-index-name]
  (let [length (count (name raw-index-name))]
    (subs (name raw-index-name) 0 (- length 27))))

(defn handle-exception
  [e]
  (if-let [ex-d (ex-data e)]
    (log/error "Got status " (:status ex-d) " from " (:trace-redirects ex-d) " with body " (:body ex-d))
    (log/error "Got exception " e))
  nil)

(defn refresh-index
  [index]
  (try
    (e/refresh-index (->virkailija-alias index))
    (catch Exception e
      (handle-exception e))))

(defn delete-index
  [index]
  (e/delete-index index))

(defn get-by-id
  [index id & query-params]
  (try
    (-> (apply e/get-document (->virkailija-alias index) id query-params)
        (:_source))
    (catch Exception e
      (handle-exception e))))

(defn get-doc
  [index id & query-params]
  (apply get-by-id index id query-params))

(defrecord BulkAction [action id doc])

(defn ->index-action
  [id doc]
  (map->BulkAction {:action "index" :id id :doc doc}))

(defn ->delete-action
  [id]
  (map->BulkAction {:action "delete" :id id :doc nil}))

(defn- bulk-action
  [index action]
  {(keyword (:action action)) {:_index index :_type "_doc" :_id (:id action)}})

(defn- bulk-doc
  [doc time]
  (assoc (dissoc doc :_index :_type) :timestamp time))

(defn ->bulk-actions
  [index actions]
  (let [true-index index
        time       (System/currentTimeMillis)]
    (-> (for [action actions]
          (let [bulk-action (bulk-action true-index action)
                bulk-doc    (when-let [doc (:doc action)] (bulk-doc doc time))]
            (remove nil? [bulk-action bulk-doc])))
        (flatten))))

(defn- get-error-message
  [errors error]
  (str "Bulk action '" (:action error) "' to index '" (:_index error) "' failed with status '" (:status error) "' and error '" (or (:error error) (:result error)) "' for (o)ids " (vec (map :_id (get errors error)))))

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
      (let [error-message (get-error-message errors error)]
        (if (= (:action error) "delete")
          (log/warn error-message)
          (log/error error-message))))
    not-ok-results))

(defn- execute-bulk-actions
  [index actions]
  (let [data   (->bulk-actions index actions)
        res    (e/bulk index data)]
    (log-and-get-bulk-errors res)))

(defn bulk
  [index actions]
  (try
    (execute-bulk-actions index actions)
    (catch Exception e
      (handle-exception e)
      (vec (map :id actions)))))
