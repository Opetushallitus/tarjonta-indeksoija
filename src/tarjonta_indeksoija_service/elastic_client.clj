(ns tarjonta-indeksoija-service.elastic-client
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [clj-http.client :as client]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.utils :refer [join-names]]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest.response :as esrsp]
            [clojurewerkz.elastisch.rest :as rest]
            [clojurewerkz.elastisch.arguments :as ar])
  (:import (clojurewerkz.elastisch.rest Connection)))

(defn refresh-index
  [index]
  (client/post (str (:elastic-url env) "/" index "/_refresh")))

(defn delete-index
  [index-name]
  (let [conn (esr/connect (:elastic-url env))]
    (esi/delete conn index-name)))

(defn get-by-id
  [index type id]
  (let [conn (esr/connect (:elastic-url env))
        res (esd/get conn index type id)]
    (:_source res)))

(defn upsert
  [index type id doc]
  (let [conn (esr/connect (:elastic-url env))]
    (esd/upsert conn index type id (assoc doc :timestamp (System/currentTimeMillis)))))

(defn delete-by-id
  [index type id]
  (let [conn (esr/connect (:elastic-url env))]
    (esd/delete conn index type id)))

(defn get-queue
  [& {:keys [index type]
      :or {index "indexdata"
           type "indexdata"}}]
  (let [conn (esr/connect (:elastic-url env))]
    (try
      (->> (esd/search conn index type :query (q/match-all) :sort {:timestamp "asc"})
          :hits
          :hits
          (map #(:_source %)))
      (catch Exception e [])))) ;; TODO: fixme

(defn push-to-indexing-queue
  [oid object-type & {:keys [index type]
                      :or {index "indexdata"
                           type "indexdata"}}]
  (upsert index type oid {:oid oid :type object-type}))

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
  [max-timestamp & {:keys [index type]
                    :or {index "indexdata"
                         type "indexdata"}}]
  (let [conn (esr/connect (:elastic-url env))]
    (delete-by-query* conn index type {:range {:timestamp {:lte max-timestamp}}})))
