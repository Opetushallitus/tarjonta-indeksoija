(ns tarjonta-indeksoija-service.indexer
  (:require [tarjonta-indeksoija-service.conf :refer [recur-pool]]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta-client]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [taoensso.timbre :as log]
            [overtone.at-at :as at]))

(defn index-hakukohde
  [oid & {:keys [index type]
         :or {index "hakukohde"
              type "hakukohde"}}]
  (let [hakukohde (tarjonta-client/get-hakukohde oid)]
    (elastic-client/upsert index type oid hakukohde)
    (log/info (str "Hakukohde " oid " indexed succesfully."))))

(defn do-index
  []
  (log/info "Starting indexing")
  (loop [objs (elastic-client/get-queue)]
    (if (empty? objs)
      (log/info "The indexing queue was empty, stopping indexing.")
      (do
        (let [obj (first objs)]
          (log/info "Indexing" (:type obj) (:oid obj))
          (condp = (:type obj)
            "hakukohde" (index-hakukohde (:oid obj))
            (log/error (str "Unknown type for indexing: " obj))) ;; TODO: move or remove object causing trouble
          (recur (rest objs)))))))

(defn start-indexer-job
  []
  (at/every 10000 do-index recur-pool))

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