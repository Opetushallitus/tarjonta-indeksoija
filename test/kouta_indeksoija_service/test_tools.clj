(ns kouta-indeksoija-service.test-tools
  (:require [cheshire.core :as cheshire]
            [kouta-indeksoija-service.elastic.tools :as tools]
            [kouta-indeksoija-service.elastic.queue :as queue]
            [kouta-indeksoija-service.indexer.job :as j]
            [kouta-indeksoija-service.kouta.koulutus :as koulutus]
            [kouta-indeksoija-service.kouta.toteutus :as toteutus]
            [kouta-indeksoija-service.kouta.haku :as haku]
            [kouta-indeksoija-service.kouta.hakukohde :as hakukohde]
            [kouta-indeksoija-service.kouta.valintaperuste :as valintaperuste]
            [kouta-indeksoija-service.kouta.koulutus-search :as koulutus-search]))

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
    (queue/refresh-queue)
    (while (and (> timeout (- (System/currentTimeMillis) start))
                (not (empty? (queue/get-queue))))
      (Thread/sleep 1000))))

(defn block-until-latest-in-queue
  [timeout]
  (let [start (System/currentTimeMillis)]
    (queue/refresh-queue)
    (while (and (> timeout (- (System/currentTimeMillis) start))
             (empty? (queue/get-queue)))
      (Thread/sleep 1000))))

(defn refresh-and-wait
  [indexname timeout]
  (tools/refresh-index indexname)
  (Thread/sleep timeout))

(defn reset-test-data
  []
  (j/reset-jobs)
  (tools/delete-index "hakukohde")
  (tools/delete-index "haku")
  (tools/delete-index "koulutus")
  (tools/delete-index "indexdata")
  (tools/delete-index "koulutusmoduuli")
  (tools/delete-index "eperuste")
  (tools/delete-index "organisaatio")
  (tools/delete-index "indexing_perf")
  (tools/delete-index "query_perf")
  (tools/delete-index "lastindex")
  (Thread/sleep 1000))
