(ns tarjonta-indeksoija-service.indexer
  (:require [tarjonta-indeksoija-service.conf :refer [recur-pool]]
            [overtone.at-at :as at]))

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