(ns konfo-indeksoija-service.elastic.queue
  (:require [konfo-indeksoija-service.elastic.admin :refer [initialize-indices]]
            [konfo-indeksoija-service.elastic.tools :as t]
            [clj-log.error-log :refer [with-error-logging with-error-logging-value]]
            [clj-elasticsearch.elastic-connect :as e]
            [clj-elasticsearch.elastic-utils :as u]
            [cheshire.core :refer [generate-string]]))

(def ^:const queue-index "indexdata")
(def ^:const index-time-index "lastindex")

(defn- ->opts
  "Coerces arguments to a map"
  [args]
  (let [x (first args)]
    (if (map? x)
      x
      (apply array-map args))))

(defn- url-with-path [& segments]
  (str u/elastic-host "/" (clojure.string/join "/" segments)))

(defn- delete-by-query-url*
  "Remove and fix delete-by-query-url* and delete-by-query* IF elastisch fixes its delete-by-query API"
  ([]
   (url-with-path "/_all/_delete_by_query"))
  ([^String index-name]
   (url-with-path index-name "_delete_by_query"))
  ([^String index-name ^String mapping-type]
   (url-with-path index-name mapping-type "_delete_by_query")))

(defn- delete-by-query*
  "Remove and fix delete-by-query-url* and delete-by-query* IF elastisch fixes its delete-by-query API"
  ([index mapping-type query]
   (u/elastic-post (delete-by-query-url* (u/join-names index) (u/join-names mapping-type)) {:query query}))

  ([index mapping-type query & args]
   (u/elastic-post (delete-by-query-url* (u/join-names index) (u/join-names mapping-type))
                   {:query-params (select-keys (->opts args)
                                               (conj [:df :analyzer :default_operator :consistency] :ignore_unavailable))
                    :body {:query query} :content-type :json})))

(defn delete-handled-queue
  [oids max-timestamp]
  (delete-by-query* (t/index-name queue-index)
                    (t/index-name queue-index)
                    {:bool {:must   {:ids {:values (map str oids)}}
                            :filter {:range {:timestamp {:lte max-timestamp}}}}}))

(defn- update-failed-oids [failed-oids]
  (t/bulk-update-failed queue-index queue-index (map (fn [x] {:oid x}) (seq failed-oids))))

(defn refresh-queue []
  (t/refresh-index queue-index))

(defn update-queue [max-timestamp success-oids failed-oids]
  (update-failed-oids failed-oids)
  (delete-handled-queue success-oids max-timestamp)
  (refresh-queue))

(defn get-queue
  []
  (with-error-logging
   (->>
    (e/search
     (t/index-name queue-index)
     (t/index-name queue-index)
     :query {
             :bool {
                    :should [
                             { :bool { :must_not { :exists { :field :retrycount } } } },
                             { :bool { :must { :range { :retrycount { :lt 3 } } } } }
                             ]
                    }
             }
     :sort {:timestamp "asc"}
     :size 1000)
    :hits
    :hits
    (map :_source))))

(defn reset-queue []
  (let [delete-res (t/delete-index queue-index)
        init-res (initialize-indices)]
    { :delete-queue delete-res
     :init-indices init-res }))

(defmacro upsert-to-queue
  [docs]
  `(t/bulk-upsert queue-index queue-index ~docs))

(defn set-last-index-time
  [timestamp]
  (let [url (u/elastic-url (t/index-name index-time-index) (t/index-name index-time-index) "1/_update")
        query {:doc {:timestamp timestamp} :doc_as_upsert true}]
    (u/elastic-post url query)))

(defn get-last-index-time
  []
  (with-error-logging-value (System/currentTimeMillis)
                            (let [res (e/get-document (t/index-name index-time-index) (t/index-name index-time-index) "1")]
                              (if (:found res)
                                (get-in res [:_source :timestamp])
                                (System/currentTimeMillis)))))