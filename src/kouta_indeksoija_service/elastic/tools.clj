(ns kouta-indeksoija-service.elastic.tools
  (:require [kouta-indeksoija-service.util.conf :refer [env]]
            [clj-log.error-log :refer [with-error-logging with-error-logging-value]]
            [clj-elasticsearch.elastic-connect :as e]
            [clj-elasticsearch.elastic-utils :as u]
            [environ.core]
            [cheshire.core :refer [generate-string]]))

(defn init-elastic-client []
  (intern 'clj-elasticsearch.elastic-utils 'elastic-host (:elastic-url env)))

(defn index-name
  [name]
  (u/index-name name (Boolean/valueOf (:test environ.core/env))))

(defn refresh-index
  [index]
  (with-error-logging
    (e/refresh-index (index-name index))))

(defn delete-index
  [index]
  (e/delete-index (index-name index)))

(defn get-by-id
  [index type id]
  (with-error-logging
    (-> (e/get-document (index-name index) (index-name type) id)
        (:_source))))

(defn- get-id
  [doc]
  (or (:oid doc) (:id doc)))

(defn- upsert-operation
  [doc index type]
  {"update" {:_index (index-name index) :_type (index-name type) :_id (get-id doc)}})

(defn- update-operation
  [doc index type]
  {"update" {:_index (index-name index) :_type (index-name type) :_id (get-id doc)}})

(defn- upsert-doc
  [doc type now]
  {:doc (assoc (dissoc doc :_index :_type) :timestamp now)
   :doc_as_upsert true})

(defn- update-doc
  [doc type now]
  {:doc (assoc (dissoc doc :_index :_type) :timestamp now)})

(defn- script-retrycounter
  []
  {"script" {:inline "if (ctx._source.containsKey(\"retrycount\")) { ctx._source.retrycount += 1 } else {ctx._source.retrycount = 1}"}})

(defn bulk-upsert-data
  [index type documents]
  (let [operations (map #(upsert-operation % index type) documents)
        now (System/currentTimeMillis)
        documents (map #(upsert-doc % type now) documents)]
    (interleave operations documents)))

(defn bulk-update-failed-data
  [index type documents]
  (let [operations (map #(update-operation % index type) documents)
        now (System/currentTimeMillis)
        documents (map #(update-doc % type now) documents)
        scripts (repeat (count documents) (script-retrycounter))]
    (interleave operations documents operations scripts)))

(defn bulk-upsert
  [index type documents]
  (with-error-logging
    (let [data (bulk-upsert-data index type documents)
          res (e/bulk index type data)]
      {:errors (not (every? false? (:errors res)))})))

(defn bulk-update-failed
  [index type documents]
  (with-error-logging
   (let [data (bulk-update-failed-data index type documents)
         res (e/bulk index type data)]
     {:errors (not (every? false? (:errors res)))})))
