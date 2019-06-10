(ns kouta-indeksoija-service.scheduled.jobs
  (:require [clojurewerkz.quartzite.jobs :refer [defjob]]
            [kouta-indeksoija-service.scheduled.scheduler :as scheduler]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.queue.queue :as queue]
            [kouta-indeksoija-service.indexer.queue :as indexer-queue]))

(defjob dlq-job [ctx] (queue/clean-dlq))

(defonce dlq-job-name "dlq")

(defn schedule-dlg-job
  []
  (scheduler/schedule-cron-job dlq-job-name dlq-job (:dlq-cron-string env)))

(defn pause-dlq-job
  []
  (scheduler/pause-job dlq-job-name))

(defn resume-dlq-job
  []
  (scheduler/resume-job dlq-job-name))

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

(defjob queueing-job [ctx] (indexer-queue/queue-changes))

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

(defn schedule-jobs
  []
  (schedule-sqs-job)
  (schedule-dlg-job)
  (schedule-queueing-job))

(defn get-jobs-info
  []
  (scheduler/list-scheduled-jobs))

(defn reset-jobs
  []
  (scheduler/reset-scheduler))