(ns tarjonta-indeksoija-service.test-tools
  (:require [cheshire.core :as cheshire]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [tarjonta-indeksoija-service.indexer :as indexer]))

(defn parse-body
  [body]
  (:result (cheshire/parse-string (slurp body) true)))

(defn block-until-indexed
  [timeout]
  (let [start (System/currentTimeMillis)]
    (elastic-client/refresh-index "indexdata")
    (while (and (> timeout (- (System/currentTimeMillis) start))
             (not (empty? (elastic-client/get-queue))))
      (Thread/sleep 1000))))

(defn refresh-and-wait
  [indexname timeout]
  (elastic-client/refresh-index indexname)
  (Thread/sleep timeout))

(defn after-tests
  []
  (indexer/reset-jobs)
  (elastic-client/delete-index "hakukohde")
  (elastic-client/delete-index "haku")
  (elastic-client/delete-index "koulutus")
  (elastic-client/delete-index "indexdata"))