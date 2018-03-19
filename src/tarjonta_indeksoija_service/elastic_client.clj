(ns tarjonta-indeksoija-service.elastic-client
  (:require [tarjonta-indeksoija-service.conf :as conf :refer [env boost-values]]
            [tarjonta-indeksoija-service.util.tools :refer [with-error-logging with-error-logging-value]]
            [tarjonta-indeksoija-service.elastic-connect :refer :all]
            [environ.core]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [taoensso.timbre :as log]
            [cheshire.core :refer [generate-string]]))

(defn get-cluster-health
  []
  (with-error-logging
    (-> (:elastic-url env)
        (str "/_cluster/health")
        (http/get {:as :json}))))

(defn check-elastic-status
  []
  (with-error-logging
    (-> (get-cluster-health)
        :status
        (= 200))))

(defn get-indices-info
  []
  (with-error-logging
    (-> (:elastic-url env)
        (str "/_cat/indices?v&format=json")
        (http/get {:as :json})
        :body)))

(defn get-elastic-status
  []
  {:cluster_health (:body (get-cluster-health))
   :indices-info (get-indices-info)})

(defn get-perf
  [type since]
  (let [res (search (index-name type)
                    (index-name type)
                        :query {:range {:created {:gte since}}}
                        :sort [{:started "desc"} {:created "desc"}]
                        :aggs {:max_avg_mills_per_object {:max {:field "avg_mills_per_object"}}
                               :avg_mills_per_object {:avg {:field "avg_mills_per_object"}}
                               :max_time {:max {:field "duration_mills"}}
                               :avg_time {:avg {:field "duration_mills"}}}
                        :size 10000)]
    (merge (:aggregations res)
           {:results (map :_source (get-in res [:hits :hits]))})))

(defn get-elastic-performance-info
  [since]
  {:indexing_performance (get-perf "indexing_perf" since)
   :query_performance (get-perf "query_perf" since)})

(defn refresh-index
  [index]
  (with-error-logging
    (http/post (str (:elastic-url env) "/" (index-name index) "/_refresh") {:content-type :json})))

(defn delete-index ; TODO -connection timeouts
  [index]
  (try (http/delete (str (:elastic-url env) "/" (index-name index)) {:content-type :json :body "{}"})
       (catch Exception e (if (not (= 404((ex-data e) :status))) (throw e)))))

(defn initialize-index-settings []
  (let [index-names ["hakukohde" "koulutus" "organisaatio" "haku" "indexdata" "lastindex" "indexing_perf" "query_perf"]
        new-indexes (filter #(not (index-exists %)) (map index-name index-names))
        results (map #(create-index % conf/index-settings) new-indexes)
        ack (map #(:acknowledged %) results)]
    (every? true? ack)))

(defn- ->opts
  "Coerces arguments to a map"
  [args]
  (let [x (first args)]
    (if (map? x)
      x
      (apply array-map args))))

(defn- update-index-mappings
  [index type settings]
  (log/info "Creating mappings for" index type)
  (let [url (str (:elastic-url env) "/" (index-name index) "/_mappings/" (index-name type))]
    (with-error-logging
      (-> url
          (http/put {:body (generate-string settings) :as :json :content-type :json})
          :body
          :acknowledged))))

(defn initialize-index-mappings []
  (let [index-names ["hakukohde" "koulutus" "organisaatio" "haku"]]
    (every? true? (doall (map #(update-index-mappings % % conf/stemmer-settings) index-names)))))

(defn initialize-indices []
  (and (initialize-index-settings)
       (initialize-index-mappings)
    (update-index-mappings "indexdata" "indexdata" conf/indexdata-mappings)))

(defn get-by-id
  [index type id]
  (with-error-logging
    (-> (get-document (index-name index) (index-name type) id)
        (:_source))))

(defmacro get-hakukohde [oid]
  `(get-by-id "hakukohde" "hakukohde" ~oid))

(defmacro get-koulutus [oid]
  `(dissoc (get-by-id "koulutus" "koulutus" ~oid) :searchData))

(defmacro get-haku [oid]
  `(get-by-id "haku" "haku" ~oid))

(defmacro get-organisaatio [oid]
  `(get-by-id "organisaatio" "organisaatio" ~oid))

(defn get-queue
  []
  (with-error-logging
      (->>
        (search
          (index-name "indexdata")
          (index-name "indexdata")
          :query {:match_all {}}
          :sort {:timestamp "asc"}
          :size 1000)
        :hits
        :hits
        (map :_source))))

(defn get-hakukohteet-by-koulutus
  [koulutus-oid]
  (let [res (search (index-name "hakukohde") (index-name "hakukohde") :query {:match {:koulutukset koulutus-oid}})]
    ;; TODO: error handling
    (map :_source (get-in res [:hits :hits]))))

(defn get-haut-by-oids
  [oids]
  (let [res (search (index-name "haku") (index-name "haku") :query {:constant_score {:filter {:terms {:oid (map str oids)}}}})]
    ;; TODO: error handling
    (map :_source (get-in res [:hits :hits]))))

;; TODO refactor with get-haut-by-oids
(defn get-organisaatios-by-oids
  [oids]
  (let [query {:constant_score {:filter {:terms {:oid (map str oids)}}}}
        res (search (index-name "organisaatio") (index-name "organisaatio") :query query)]
    ;; TODO: error handling
    (map :_source (get-in res [:hits :hits]))))

(defn- upsert-operation
  [doc index type]
  {"update" {:_index (index-name index) :_type (index-name type) :_id (:oid doc)}})

(defn- upsert-doc
  [doc type now]
  {:doc (assoc (dissoc doc :_index :_type) :timestamp now)
   :doc_as_upsert true})

(defn bulk-upsert-data
  [index type documents]
  (let [operations (map #(upsert-operation % index type) documents)
        now (System/currentTimeMillis)
        documents (map #(upsert-doc % type now) documents)]
    (interleave operations documents)))

(defn bulk-upsert
  [index type documents]
  (with-error-logging
    (let [data (bulk-upsert-data index type documents)
          res (bulk index type data)]
      {:errors (not (every? false? (:errors res)))})))

(defmacro upsert-indexdata
  [docs]
  `(bulk-upsert "indexdata" "indexdata" ~docs))

(defn set-last-index-time
  [timestamp]
  (elastic-post (elastic-url (index-name "lastindex") (index-name "lastindex") "1/_update") {:doc {:timestamp timestamp} :doc_as_upsert true}))

(defn get-last-index-time
  []
  (with-error-logging-value (System/currentTimeMillis)
    (let [res (get-document (index-name "lastindex") (index-name "lastindex") "1")]
      (if (:found res)
        (get-in res [:_source :timestamp])
        (System/currentTimeMillis)))))

(defn insert-indexing-perf
  [indexed-amount duration started]
  (with-error-logging
      (create
        (index-name "indexing_perf")
        (index-name "indexing_perf")
        {:created              (System/currentTimeMillis)
         :started              started
         :duration_mills       duration
         :indexed_amount       indexed-amount
         :avg_mills_per_object (if (= 0 indexed-amount) 0 (/ duration indexed-amount))})))

(defn insert-query-perf
  [query duration started res-size]
  (with-error-logging
      (create
              (index-name "query_perf")
              (index-name "query_perf")
              {:created        (System/currentTimeMillis)
               :started        started
               :duration_mills duration
               :query          query
               :response_size  res-size})))

(defn url-with-path [& segments]
  (str (:elastic-url env) "/" (clojure.string/join "/" segments)))

(defn delete-by-query-url*
  "Remove and fix delete-by-query-url* and delete-by-query* IF elastisch fixes its delete-by-query API"
  ([]
   (url-with-path "/_all/_delete_by_query"))
  ([^String index-name]
   (url-with-path index-name "_delete_by_query"))
  ([^String index-name ^String mapping-type]
   (url-with-path index-name mapping-type "_delete_by_query")))

(defn- post [uri {:keys [body] :as options}]
  (json/decode (:body (http/post uri (merge {:accept :json}
                                            ;(.http-opts conn)
                                            options
                                            {:body (json/encode body)})))
               true))

(defn delete-by-query*
  "Remove and fix delete-by-query-url* and delete-by-query* IF elastisch fixes its delete-by-query API"
  ([index mapping-type query]
   (post (delete-by-query-url* (join-names index) (join-names mapping-type))
              {:body {:query query} :content-type :json}))

  ([index mapping-type query & args]
   (post (delete-by-query-url* (join-names index) (join-names mapping-type))
              {:query-params (select-keys (->opts args)
                                          (conj [:df :analyzer :default_operator :consistency] :ignore_unavailable))
               :body {:query query} :content-type :json})))

(defn delete-handled-queue
  [oids max-timestamp]
  (delete-by-query* (index-name "indexdata")
                    (index-name "indexdata")
                    {:bool {:must   {:ids {:values (map str oids)}}
                            :filter {:range {:timestamp {:lte max-timestamp}}}}}))

(defn- create-hakutulos [koulutushakutulos]
  (let [koulutus (:_source koulutushakutulos)
        score (:_score koulutushakutulos)]
    {:score score
     :oid (:oid koulutus)
     :nimi (get-in koulutus [:koulutuskoodi :nimi])
     :tarjoaja (get-in koulutus [:organisaatio :nimi])}))

(defn text-search
  [query]
  (with-error-logging
    (let [start (System/currentTimeMillis)
          res (->> (search
                               (index-name "koulutus")
                               (index-name "koulutus")
                               :query {:multi_match {:query query :fields boost-values}})
                   :hits
                   :hits
                   (map create-hakutulos))]
      (insert-query-perf query (- (System/currentTimeMillis) start) start (count res))
      res)))