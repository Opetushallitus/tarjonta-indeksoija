(ns tarjonta-indeksoija-service.test-tools
  (:require [cheshire.core :as cheshire]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]))

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