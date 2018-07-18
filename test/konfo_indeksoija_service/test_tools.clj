(ns konfo-indeksoija-service.test-tools
  (:require [cheshire.core :as cheshire]
            [konfo-indeksoija-service.elastic-client :as elastic-client]
            [konfo-indeksoija-service.indexer :as indexer])
  (:import (pl.allegro.tech.embeddedelasticsearch EmbeddedElastic PopularProperties)))

(def embedded-elastic (atom nil))

(defn start-embedded-elasticsearch []
    (reset! embedded-elastic (-> (EmbeddedElastic/builder)
                                 (.withElasticVersion "6.0.0")
                                 (.withSetting PopularProperties/TRANSPORT_TCP_PORT 6666)
                                 (.withSetting PopularProperties/CLUSTER_NAME "my_cluster")
                                 (.build)))
    (.start @embedded-elastic))

(defn stop-elastic-test []
  (.stop @embedded-elastic))


(defn init-test-logging []
  (intern 'clj-log.error-log 'test true)
  (intern 'clj-log.error-log 'verbose false))

(defn init-elastic-test []
  (init-test-logging)
  (intern 'clj-elasticsearch.elastic-utils 'elastic-host "http://localhost:9200")
  (start-embedded-elasticsearch))

(defn parse
  [body]
  (try
    (cheshire/parse-string (slurp body) true)
    (catch Exception e nil)))

(defn parse-body
  [body]
  (-> body
    parse
    :result))

(defn block-until-indexed
  [timeout]
  (let [start (System/currentTimeMillis)]
    (elastic-client/refresh-index "indexdata")
    (while (and (> timeout (- (System/currentTimeMillis) start))
                (not (empty? (elastic-client/get-queue))))
      (Thread/sleep 1000))))

(defn block-until-latest-in-queue
  [timeout]
  (let [start (System/currentTimeMillis)]
    (elastic-client/refresh-index "indexdata")
    (while (and (> timeout (- (System/currentTimeMillis) start))
             (empty? (elastic-client/get-queue)))
      (Thread/sleep 1000))))

(defn refresh-and-wait
  [indexname timeout]
  (elastic-client/refresh-index indexname)
  (Thread/sleep timeout))

(defn reset-test-data
  []
  (indexer/reset-jobs)
  (elastic-client/delete-index "hakukohde")
  (elastic-client/delete-index "haku")
  (elastic-client/delete-index "koulutus")
  (elastic-client/delete-index "indexdata")
  (elastic-client/delete-index "organisaatio")
  (elastic-client/delete-index "indexing_perf")
  (elastic-client/delete-index "query_perf")
  (elastic-client/delete-index "lastindex"))

(defn parse-args
  [& args]
  (let [aps (partition-all 2 args)
        [opts-and-vals ps] (split-with #(keyword? (first %)) aps)
        options (into {} (map vec opts-and-vals))
        positionals (reduce into [] ps)]
    [options positionals]))
