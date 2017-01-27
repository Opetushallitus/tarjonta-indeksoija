(ns tarjonta-indeksoija-service.elastic-client
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [environ.core]
            [clj-http.client :as client]
            [taoensso.timbre :as log]
            [clojurewerkz.elastisch.rest.bulk :as bulk]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.utils :refer [join-names]]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest.response :as esrsp]
            [clojurewerkz.elastisch.rest :as rest]
            [clojurewerkz.elastisch.arguments :as ar])
  (:import (clojurewerkz.elastisch.rest Connection)))

(defn check-elastic-status
  []
  (try
    (-> (:elastic-url env)
        esr/connect
        esr/cluster-state-url
        client/get
        :status
        (= 200))
    (catch Exception e
      (log/error (.getMessage e))
      false)))

(defn index-name
  [name]
  (str name (when (Boolean/valueOf (:test environ.core/env)) "_test")))

(defn refresh-index
  [index]
  (client/post (str (:elastic-url env) "/" (index-name index) "/_refresh")))

(defn delete-index
  [index]
  (let [conn (esr/connect (:elastic-url env))]
    (esi/delete conn (index-name index))))

(defn get-by-id
  [index type id]
  (let [conn (esr/connect (:elastic-url env))
        res (esd/get conn (index-name index) (index-name type) id)]
    (:_source res)))

(defn get-queue
  []
  (let [conn (esr/connect (:elastic-url env))]
    (try
      (->> (esd/search conn (index-name "indexdata") (index-name "indexdata") :query (q/match-all) :sort {:timestamp "asc"} :size 10000)
          :hits
          :hits
          (map :_source))
      (catch Exception e [])))) ;; TODO: fixme

(defn- upsert-operation
  [doc index type]
  {"update" {:_index (index-name index) :_type (index-name type) :_id (:oid doc)}})

(defn- upsert-doc
  [doc now]
  {:doc (assoc (dissoc doc :_index :_type) :timestamp now)
   :doc_as_upsert true})

(defn- bulk-upsert-data
  [index type documents]
  (let [operations (map #(upsert-operation % index type) documents)
        now (System/currentTimeMillis)
        documents  (map #(upsert-doc % now) documents)]
   (interleave operations documents)))


(defn bulk-upsert
  [index type documents]
  (let [conn (esr/connect (:elastic-url env))
        data (bulk-upsert-data index type documents)]
    (bulk/bulk conn data)))

(defn delete-by-query-url*
  "Remove and fix delete-by-query-url* and delete-by-query* IF elastisch fixes its delete-by-query API"
  ([conn]
   (esr/url-with-path conn "/_all/_delete_by_query"))
  ([conn ^String index-name]
   (esr/url-with-path conn index-name "_delete_by_query"))
  ([conn ^String index-name ^String mapping-type]
   (esr/url-with-path conn index-name mapping-type "_delete_by_query")))

(defn delete-by-query*
  "Remove and fix delete-by-query-url* and delete-by-query* IF elastisch fixes its delete-by-query API"
  ([^Connection conn index mapping-type query]
   (rest/post conn (delete-by-query-url*
                       conn
                       (join-names index) (join-names mapping-type)) {:body {:query query}}))
  ([^Connection conn index mapping-type query & args]
   (rest/post conn (delete-by-query-url* conn
                                               (join-names index) (join-names mapping-type))
              {:query-params (select-keys (ar/->opts args) (conj esd/optional-delete-query-parameters :ignore_unavailable))
               :body         {:query query}})))

(defn delete-handled-queue
  [max-timestamp]
  (let [conn (esr/connect (:elastic-url env))]
    (delete-by-query* conn (index-name "indexdata") (index-name "indexdata") {:range {:timestamp {:lte max-timestamp}}})))
