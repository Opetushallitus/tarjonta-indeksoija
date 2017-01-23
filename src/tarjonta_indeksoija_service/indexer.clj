(ns tarjonta-indeksoija-service.indexer
  (:require [tarjonta-indeksoija-service.conf :refer [recur-pool]]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta-client]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [overtone.at-at :as at]))

(defn index-hakukohde
  [oid & {:keys [index type]
         :or {index "hakukohde"
              type "hakukohde"}}]
  (let [hakukohde (tarjonta-client/get-hakukohde oid)]
    (elastic-client/upsert index type oid hakukohde)))

(defn start-indexer-job
  []
  (at/every 1000 #(println "tsers") recur-pool))

(defn reset-jobs
  []
  (at/stop-and-reset-pool! recur-pool))

(defn start-stop-indexer
  [start?]
  (if start?
    (do
      (start-indexer-job)
      "Started indexer job")
    (do
      (reset-jobs)
      "Stopped all jobs and reseted pool.")))