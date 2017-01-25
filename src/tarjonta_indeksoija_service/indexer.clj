(ns tarjonta-indeksoija-service.indexer
  (:require [tarjonta-indeksoija-service.conf :refer [recur-pool]]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta-client]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [taoensso.timbre :as log]
            [overtone.at-at :as at]))

(defn index-object
  [obj]
  (log/info "Indexing" (:type obj) (:oid obj))
  (let [doc (tarjonta-client/get-doc obj)]
    (elastic-client/bulk-upsert (:type obj) (:type obj) [doc])
    (log/info (str (clojure.string/capitalize (:type obj)) " " (:oid obj) " indexed succesfully."))))

(defn end-indexing
  [last-timestamp]
  (log/info "The indexing queue was empty, stopping indexing and deleting indexed items from queue.")
  (elastic-client/delete-handled-queue last-timestamp))

(defn do-index
  []
  (let [to-be-indexed (elastic-client/get-queue)
        last-timestamp (apply max (map :timestamp to-be-indexed))]
    (log/info (str "Starting indexing of " (count to-be-indexed) " items."))
    (loop [jobs to-be-indexed]
      (if (empty? jobs)
        (end-indexing last-timestamp)
        (do
          (try
            (index-object (first jobs))
          ( catch Exception e (log/error e))) ;; TODO: move or remove object causing trouble
          (recur (rest jobs)))))))

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