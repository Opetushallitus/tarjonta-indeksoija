(ns kouta-indeksoija-service.scheduled.scheduler
  (:require [clojure.tools.logging :as log]
            [mount.core :refer [defstate]]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.triggers :as t]
            [clojure.string :refer [lower-case]]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]]
            [kouta-indeksoija-service.util.tools :refer [uuid]])
  (:import (org.quartz.impl.matchers GroupMatcher)
           (org.quartz Scheduler)))

(defstate job-pool :start (qs/start (qs/initialize)))

(defn- job-key
  [name]
  (j/key (lower-case (str "jobs." name))))

(defn- cron-trigger-key
  [name]
  (t/key (lower-case (str name "-cron-trigger"))))

(defn- one-time-trigger-key
  [name]
  (t/key (lower-case (str name "-one-time-trigger"))))

(defn- create-job
  [name job]
  (j/build
    (j/of-type job)
    (j/with-identity (job-key name))))

(defn- create-cron-trigger
  [name cron-string]
  (t/build
    (t/with-identity (cron-trigger-key name))
    (t/start-now)
    (t/with-schedule
      (schedule (cron-schedule cron-string)))))

(defn- create-one-time-trigger
  [name]
  (t/build
   (t/with-identity (one-time-trigger-key name))
   (t/start-now)))

(defn schedule-cron-job
  [name job cron-string]
  (if (qs/scheduled? job-pool (job-key name))
    (log/info "Job " name " already scheduled! Skipping...")
    (let [jobb  (create-job name job)
          trigger (create-cron-trigger name cron-string)]
      (log/info "Starting " name " handling with cron schedule " cron-string (qs/schedule job-pool jobb trigger)))))

(defn schedule-one-time-job
  [name job]
  (if (qs/scheduled? job-pool (job-key name))
    (log/info "Job " name " already scheduled! Skipping...")
    (let [jobb  (create-job name job)
          trigger (create-one-time-trigger name)]
      (log/info "Starting " name " as one time job " (qs/schedule job-pool jobb trigger)))))

(defn- if-scheduled
  [name f op-name]
  (let [key (job-key name)]
    (if (qs/scheduled? job-pool key)
      (do
        (log/info "Executing " op-name " for " name " job!")
        (f job-pool key))
      (log/error "Cannot execute " op-name " for job " name " because job is not scheduled!"))))

(defn pause-job
  [name]
  (if-scheduled name qs/pause-job "pause"))

(defn resume-job
  [name]
  (if-scheduled name qs/resume-job "resume"))

(defn delete-job
  [name]
  (if-scheduled name qs/delete-job "delete"))

(defn reset-scheduler
  []
  (log/warn "Resetting scheduler. All jobs are deleted!")
  (qs/clear! job-pool))

(defn list-scheduled-jobs
  []
  (vec (->> (GroupMatcher/jobGroupEquals (Scheduler/DEFAULT_GROUP))
            (qs/get-job-keys job-pool)
            (qs/get-jobs job-pool)
            (map (fn [j] (let [key (.getKey j)]
                           {:job (.toString key)
                            :scheduled (qs/scheduled? job-pool key)
                            :triggers (vec (->> (.getKey j)
                                                (qs/get-triggers-of-job job-pool)
                                                (map (fn [t] (let [tkey (.getKey t)]
                                                               {:trigger (.toString tkey)
                                                                :priority (.getPriority t)
                                                                :state (.toString (.getTriggerState job-pool tkey))
                                                                :clazz   (.toString (.getClass t))
                                                                :start-time (.getStartTime t)
                                                                :next-fire-time (.getNextFireTime t)
                                                                :previous-fire-time (.getPreviousFireTime t)})))))}))))))