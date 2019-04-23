(ns konfo-indeksoija-service.indexer.job
  (:require [konfo-indeksoija-service.indexer.index :refer [do-index]]
            [konfo-indeksoija-service.util.conf :refer [env job-pool]]
            [konfo-indeksoija-service.rest.tarjonta :as tarjonta]
            [konfo-indeksoija-service.rest.organisaatio :as organisaatio]
            [konfo-indeksoija-service.rest.eperuste :as eperuste]
            [konfo-indeksoija-service.elastic.queue :refer [set-last-index-time get-last-index-time upsert-to-queue]]
            [clj-log.error-log :refer [with-error-logging]]
            [konfo-indeksoija-service.util.logging :refer [to-date-string]]
            [clojure.tools.logging :as log]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.jobs :as j :refer [defjob]]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]])
  (:import (org.quartz ObjectAlreadyExistsException)))

(def elastic-lock? (atom false :error-handler #(log/error %)))

(defmacro wait-for-elastic-lock
  [& body]
  `(if-not (compare-and-set! elastic-lock? false true)
     (log/debug "Indexing job already running, skipping job.")
     (try
       (do ~@body)
       (finally (reset! elastic-lock? false)))))

(defjob indexing-job [ctx]
  (with-error-logging
   (wait-for-elastic-lock
    (let [now (System/currentTimeMillis)
          last-modified (get-last-index-time)
          ;tarjonta-changes (tarjonta/get-last-modified last-modified)
          organisaatio-changes (organisaatio/find-last-changes last-modified)
          eperuste-changes (eperuste/find-changes last-modified)
          changes-since (clojure.set/union organisaatio-changes eperuste-changes)] ;tarjonta-changes
      (when-not (nil? changes-since)
        (log/info "Fetched last-modified since" (to-date-string last-modified)", containing" (count changes-since) "changes.")
        (let [related-koulutus (flatten (pmap tarjonta/get-related-koulutus changes-since))
              last-modified-with-related-koulutus (remove nil? (clojure.set/union changes-since related-koulutus))]
          (if-not (empty? related-koulutus)
            (log/info "Fetched" (count related-koulutus) "related koulutukses for previous changes"))
          (upsert-to-queue last-modified-with-related-koulutus)
          (set-last-index-time now)
          (do-index)))))))

(defn start-indexer-job
  ([] (start-indexer-job (:cron-string env)))
  ([cron-string]
   (log/info "Starting indexer job!")
   (let [job (j/build
              (j/of-type indexing-job)
              (j/with-identity "jobs.index.1"))
         trigger (t/build
                  (t/with-identity (t/key "cron-trigger"))
                  (t/start-now)
                  (t/with-schedule
                   (schedule (cron-schedule cron-string))))]
     (log/info (str "Starting indexer with cron schedule " cron-string)
               (qs/schedule job-pool job trigger)))))

(defn reset-jobs
  []
  (reset! elastic-lock? false)
  (qs/clear! job-pool))

(defn start-stop-indexer
  [start?]
  (try
    (if start?
      (do
        (log/info "Starting indexer job")
        (start-indexer-job))
      (do
        (log/info "Stopping all jobs and clearing job pool.")
        (reset-jobs)))

    (catch ObjectAlreadyExistsException e "Indexer already running.")
    (catch Exception e)))