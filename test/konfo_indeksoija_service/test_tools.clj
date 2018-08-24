(ns konfo-indeksoija-service.test-tools
  (:require [cheshire.core :as cheshire]
            [konfo-indeksoija-service.elastic.elastic-client :as elastic-client]
            [konfo-indeksoija-service.indexer :as indexer]))

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
