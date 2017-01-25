(ns tarjonta-indeksoija-service.indexer
  (:require [tarjonta-indeksoija-service.conf :refer [job-pool]]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta-client]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [tarjonta-indeksoija-service.converter.koulutus-converter :as converter]
            [taoensso.timbre :as log]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.jobs :as j :refer [defjob]]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]]))

(def running? (atom 0
                :error-handler #(log/error %)
                :validator #(or (= 1 %) (zero? %))))

(defn index-object
  [obj]
  (log/info "Indexing" (:type obj) (:oid obj))
  (let [doc (tarjonta-client/get-doc obj)]
    (elastic-client/bulk-upsert (:type obj) (:type obj)
                                [(if (.contains (:type obj) "koulutus")
                                   (converter/convert doc)
                                   doc)
                                 ])
    (log/info (str (clojure.string/capitalize (:type obj)) " " (:oid obj) " indexed succesfully."))))

(defn end-indexing
  [last-timestamp]
  (log/info "The indexing queue was empty, stopping indexing and deleting indexed items from queue.")
  (elastic-client/delete-handled-queue last-timestamp)
  (elastic-client/refresh-index "indexdata"))

(defn do-index
  []
  (let [to-be-indexed (elastic-client/get-queue)]
    (if (empty? to-be-indexed)
      (log/debug "Nothing to index.")
      (do
        (log/info (str "Starting indexing of " (count to-be-indexed) " items."))
        (loop [jobs to-be-indexed]
          (if (empty? jobs)
            (end-indexing (apply max (map :timestamp to-be-indexed)))
            (do
              (try
                (index-object (first jobs))
                (Thread/sleep 1000)
                ( catch Exception e (log/error e))) ;; TODO: move or remove object causing trouble
              (recur (rest jobs)))))))))

(defn start-indexing
  []
  (if (pos? @running?)
    (log/debug "Indexing already running.")
    (do
      (reset! running? 1)
      (do-index)
      (reset! running? 0))))

(defjob indexing-job
  [ctx]
  (start-indexing))

(defn start-indexer-job
  []
  (let [job (j/build
              (j/of-type indexing-job)
              (j/with-identity "jobs.index.1"))
        trigger (t/build
                  (t/with-identity (t/key "crontirgger"))
                  (t/start-now)
                  (t/with-schedule
                    (schedule
                      (cron-schedule "*/5 * * ? * *"))))]
    (qs/schedule job-pool job trigger)))

(defn reset-jobs
  []
  (qs/clear! job-pool))

(defn start-stop-indexer
  [start?]
  (if start?
    (do
      (start-indexer-job)
      "Started indexer job")
    (do
      (reset-jobs)
      "Stopped all jobs and reseted pool.")))