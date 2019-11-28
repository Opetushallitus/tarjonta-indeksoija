(ns kouta-indeksoija-service.elastic.tools
  (:require [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.util.tools :refer [get-id]]
            [clj-elasticsearch.elastic-connect :as e]
            [clj-elasticsearch.elastic-utils :as u]
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

(defn- upsert-operation
  [doc index type]
  {"index" {:_index (index-name index) :_type (index-name type) :_id (get-id doc)}})

(defn- upsert-doc
  [doc type now]
  (assoc (dissoc doc :_index :_type) :timestamp now))

(defn ->bulk-upsert-data
  [index type documents]
  (let [operations (map #(upsert-operation % index type) documents)
        now (System/currentTimeMillis)
        documents (map #(upsert-doc % type now) documents)]
    (interleave operations documents)))

(defn- execute-bulk-upsert
  [index type documents]
  (let [data   (->bulk-upsert-data index type documents)
        res    (e/bulk index type data)
        failed (filter #(true? (:errors %)) res)]
    (if-not (empty? failed)
      (let [errors (group-by :error (map :index (mapcat (fn [x] (filter #(-> % (:index) (:status) (> 299)) (:items x))) failed)))]
        (doseq [error (keys errors)]
          (log/error "Bulk upsert to index " index " failed with error: " error " for ids: " (vec (map :_id (get errors error)))))
        errors)
      [])))

(defn bulk-upsert
  [index type documents]
  (try
    (execute-bulk-upsert index type documents)
    (catch Exception e
      (handle-exception e)
      (vec (map get-id documents)))))

(defn upsert-docs
  [type docs]
  (bulk-upsert type type docs))

(defn get-doc
  [type id]
  (get-by-id type type id))
