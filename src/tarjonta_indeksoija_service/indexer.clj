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
  (let [doc (tarjonta-client/get-doc obj)
        res (elastic-client/bulk-upsert (:type obj) (:type obj)
              [(if (.contains (:type obj) "koulutus")
                 (converter/convert doc)
                 doc)])
        errors (:errors res)
        status (:result (:update (first (:items res))))]
    (if errors
      (log/error (str "Indexing failed for  "
                      (clojure.string/capitalize (:type obj)) " " (:oid obj)
                      "\n" errors))
      (log/info (str (clojure.string/capitalize (:type obj)) " " (:oid obj) " " status " succesfully."))))
  obj)

(defn end-indexing
  [last-timestamp]
  (log/info "The indexing queue was empty, stopping indexing and deleting indexed items from queue.")
  (elastic-client/delete-handled-queue last-timestamp)
  (elastic-client/refresh-index (elastic-client/index-name "indexdata")))

(defn do-index
  []
  (let [to-be-indexed (elastic-client/get-queue)
        agents (map agent to-be-indexed)]
    (if (empty? to-be-indexed)
      (log/info "Nothing to index.")
      (do
        (doseq [a agents]
          (send-off a index-object))
        (apply await agents)
        (end-indexing (->> (map deref agents)
                           (map :timestamp)
                           (apply max)))))))

(defn start-indexing
  []
  (try
    (if (pos? @running?)
      (log/debug "Indexing already running.")
      (do
        (reset! running? 1)
        (do-index)))
    (catch Exception e (log/error e))
    (finally (reset! running? 0))))

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
                      (cron-schedule "*/1 * * ? * *"))))] ;; TODO: Should be changed to something less/parameterized
    (qs/schedule job-pool job trigger)))

(defn reset-jobs
  []
  (reset! running? 0)
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