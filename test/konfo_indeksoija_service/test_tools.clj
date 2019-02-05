(ns konfo-indeksoija-service.test-tools
  (:require [cheshire.core :as cheshire]
            [konfo-indeksoija-service.elastic.tools :as tools]
            [konfo-indeksoija-service.elastic.queue :as queue]
            [konfo-indeksoija-service.indexer.job :as j]
            [konfo-indeksoija-service.kouta.koulutus :as koulutus]
            [konfo-indeksoija-service.kouta.toteutus :as toteutus]
            [konfo-indeksoija-service.kouta.haku :as haku]
            [konfo-indeksoija-service.kouta.hakukohde :as hakukohde]
            [konfo-indeksoija-service.kouta.valintaperuste :as valintaperuste]
            [konfo-indeksoija-service.kouta.koulutus-search :as koulutus-search]))

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

(defn reset-kouta-test-data
  []
  (tools/delete-index koulutus/index-name)
  (tools/delete-index toteutus/index-name)
  (tools/delete-index haku/index-name)
  (tools/delete-index hakukohde/index-name)
  (tools/delete-index valintaperuste/index-name)
  (tools/delete-index koulutus-search/index-name))