(ns kouta-indeksoija-service.scheduled.jobs
  (:require [clojurewerkz.quartzite.jobs :refer [defjob]]
            [kouta-indeksoija-service.scheduled.scheduler :as scheduler]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.queue.queue :as queue]
            [kouta-indeksoija-service.queue.notification-queue :as notification-queue]
            [kouta-indeksoija-service.queuer.queuer :as queuer]
            [kouta-indeksoija-service.indexer.indexer :as indexer]))

(defjob sqs-job [ctx] (queue/index-from-sqs))

(defonce sqs-job-name "sqs")

(defn schedule-sqs-job
  []
  (scheduler/schedule-one-time-job sqs-job-name sqs-job))

(defn pause-sqs-job
  []
  (scheduler/pause-job sqs-job-name))

(defn resume-sqs-job
  []
  (scheduler/resume-job sqs-job-name))

(defjob queueing-job [ctx] (queuer/queue-changes))

(defonce queueing-job-name "queueing")

(defn schedule-queueing-job
  []
  (scheduler/schedule-cron-job queueing-job-name queueing-job (:queueing-cron-string env)))

(defn pause-queueing-job
  []
  (scheduler/pause-job queueing-job-name))

(defn resume-queueing-job
  []
  (scheduler/resume-job queueing-job-name))

(defjob notification-job [ctx] (notification-queue/read-and-send-notifications))

(defonce notification-job-name "notification")

(defn schedule-notification-job
  []
  (scheduler/schedule-one-time-job notification-job-name notification-job))

(defn pause-notification-job
  []
  (scheduler/pause-job notification-job-name))

(defn resume-notification-job
  []
  (scheduler/resume-job notification-job-name))

(defjob lokalisaatio-indexing-job [ctx] (indexer/index-all-lokalisoinnit))

(defonce lokalisaatio-indexing-job-name "lokalisaatio-indexing")

(defn schedule-lokalisaatio-indexing-job
  []
  (scheduler/schedule-cron-job lokalisaatio-indexing-job-name lokalisaatio-indexing-job (:lokalisaatio-indexing-cron-string env)))

(defn pause-lokalisaatio-indexing-job
  []
  (scheduler/pause-job lokalisaatio-indexing-job-name))

(defn resume-lokalisaatio-indexing-job
  []
  (scheduler/resume-job lokalisaatio-indexing-job-name))

(defjob organisaatio-indexing-job [ctx] (indexer/index-all-oppilaitokset))

(defonce organisaatio-indexing-job-name "organisaatio-indexing")

(defn schedule-organisaatio-indexing-job
  []
  (scheduler/schedule-cron-job organisaatio-indexing-job-name organisaatio-indexing-job (:organisaatio-indexing-cron-string env)))

(defn pause-organisaatio-indexing-job
  []
  (scheduler/pause-job organisaatio-indexing-job-name))

(defn resume-organisaatio-indexing-job
  []
  (scheduler/resume-job organisaatio-indexing-job-name))

(defn schedule-jobs
  []
  (schedule-sqs-job)
  (schedule-queueing-job)
  (schedule-notification-job)
  (schedule-lokalisaatio-indexing-job)
  (schedule-organisaatio-indexing-job))

(defn get-jobs-info
  []
  (scheduler/list-scheduled-jobs))

(defn reset-jobs
  []
  (scheduler/reset-scheduler))
