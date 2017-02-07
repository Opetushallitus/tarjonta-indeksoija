(ns tarjonta-indeksoija-service.elastic-client
  (:require [tarjonta-indeksoija-service.conf :as conf :refer [env]]
            [environ.core]
            [clj-http.client :as client]
            [taoensso.timbre :as log]
            [cheshire.core :refer [generate-string]]
            [clojurewerkz.elastisch.rest.bulk :as bulk]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.utils :refer [join-names]]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest.response :as esrsp]
            [clojurewerkz.elastisch.rest :as rest]
            [clojurewerkz.elastisch.arguments :as ar]
            [clj-http.client :as http])
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
      (log/error (str "Elastic search error: "(.getMessage e)))
      false)))

(defn index-name
  [name]
  (str name (when (Boolean/valueOf (:test environ.core/env)) "_test")))

(defn refresh-index
  [index]
  (try
    (client/post (str (:elastic-url env) "/" (index-name index) "/_refresh"))
    (catch Exception e
      (if (Boolean/valueOf (:test environ.core/env))
        (log/info (str "Refreshing index " index " failed, continuing test."))
        (log/error e)))))

(defn delete-index
  [index]
  (let [conn (esr/connect (:elastic-url env))]
    (esi/delete conn (index-name index))))

(defn- create-index [index]
  (log/info "Creating index" index)
  (http/put (str (:elastic-url env) "/" (index-name index) "/" (index-name index) "/init") {:body "{}"})
  (http/delete (str (:elastic-url env) "/" (index-name index) "/" (index-name index) "/init")))

(defn- create-indices [index-names]
  (doall (map create-index index-names)))

(defn initialize-index-settings
  []
  (let [conn (esr/connect (:elastic-url env))
        index-names ["hakukohde" "koulutus" "organisaatio" "haku" "indexdata" "lastindex"]
        index-names-joined (clojure.string/join "," (map #(index-name %) index-names))]
    (create-indices index-names)
    (esi/close conn index-names-joined)
    (let [res (esi/update-settings conn index-names-joined conf/analyzer-settings)]
      (esi/open conn index-names-joined)
      (:acknowledged res))))

(defn- update-index-mappings
  [index type]
  (log/info "Creating mappings for" index type)
  (let [url (str (:elastic-url env) "/" (index-name index) "/_mappings/" (index-name type))]
    (try
      (-> url
          (client/put {:body (generate-string conf/stemmer-settings) :as :json})
          :body
          :acknowledged)
      (catch Exception e
        (log/error (str "Elastic search error: "(.getMessage e)))
        false))))

(defn initialize-index-mappings []
  (let [index-names ["hakukohde" "koulutus" "organisaatio" "haku" "indexdata"]]
    (every? true? (doall (map #(update-index-mappings % %) index-names)))))

(defn initialize-indices []
  (and (initialize-index-settings)
       (initialize-index-mappings)))

(defn get-by-id
  [index type id]
  (let [conn (esr/connect (:elastic-url env))
        res (esd/get conn (index-name index) (index-name type) id)]
    (:_source res)))

(defmacro get-hakukohde [oid]
  `(get-by-id "hakukohde" "hakukohde" ~oid))

(defmacro get-koulutus [oid]
  `(get-by-id "koulutus" "koulutus" ~oid))

(defmacro get-haku [oid]
  `(get-by-id "haku" "haku" ~oid))

(defmacro get-organisaatio [oid]
  `(get-by-id "organisaatio" "organisaatio" ~oid))

(defn get-queue
  []
  (let [conn (esr/connect (:elastic-url env))]
    (try
      (->> (esd/search conn (index-name "indexdata") (index-name "indexdata") :query (q/match-all) :sort {:timestamp "asc"} :size 10000)
          :hits
          :hits
          (map :_source))
      (catch Exception e [])))) ;; TODO: fixme

(defn get-hakukohteet-by-koulutus
  [koulutus-oid]
  (let [conn (esr/connect (:elastic-url env))
        res (esd/search conn (index-name "hakukohde") (index-name "hakukohde") :query {:match {:koulutukset koulutus-oid}})]
    ;; TODO: error handling
    (map :_source (get-in res [:hits :hits]))))

(defn get-haut-by-oids
  [oids]
  (let [conn (esr/connect (:elastic-url env))
        res (esd/search conn (index-name "haku") (index-name "haku") :query {:constant_score {:filter {:terms {:oid (map str oids)}}}})]
    ;; TODO: error handling
    (map :_source (get-in res [:hits :hits]))))

;; TODO refactor with get-haut-by-oids
(defn get-organisaatios-by-oids
  [oids]
  (let [conn (esr/connect (:elastic-url env))
        query {:constant_score {:filter {:terms {:oid (map str oids)}}}}
        res (esd/search conn (index-name "organisaatio") (index-name "organisaatio") :query query)]
    ;; TODO: error handling
    (map :_source (get-in res [:hits :hits]))))

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

(defmacro upsert-indexdata
  [docs]
  `(bulk-upsert "indexdata" "indexdata" ~docs))

(defn set-last-index-time
  [timestamp]
  (let [conn (esr/connect (:elastic-url env))]
    (esd/upsert conn (index-name "lastindex") (index-name "lastindex") "1" {:timestamp timestamp})))

(defn get-last-index-time
  []
  (try
    (let [conn (esr/connect (:elastic-url env))
          res (esd/search conn (index-name "lastindex") (index-name "lastindex"))]
      (-> (get-in res [:hits :hits])
          first
          :_source
          :timestamp))
    (catch Exception e
      (if (Boolean/valueOf (:test environ.core/env))
        (log/info "Couldn't get latest indexing timestamp, continuing test.")
        (log/error e))
      (System/currentTimeMillis))))

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
   (rest/post conn (delete-by-query-url* conn
                                         (join-names index)
                                         (join-names mapping-type))
              {:body {:query query}}))

  ([^Connection conn index mapping-type query & args]
   (rest/post conn (delete-by-query-url* conn
                                         (join-names index)
                                         (join-names mapping-type))
              {:query-params (select-keys (ar/->opts args)
                                          (conj esd/optional-delete-query-parameters :ignore_unavailable))
               :body         {:query query}})))

(defn delete-handled-queue
  [oids max-timestamp]
  (let [conn (esr/connect (:elastic-url env))]
    (delete-by-query* conn
                      (index-name "indexdata")
                      (index-name "indexdata")
                      {:bool {:must {:ids {:values (map str oids)}}
                              :filter {:range {:timestamp {:lte max-timestamp}}}}})))

