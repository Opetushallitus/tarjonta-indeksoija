(ns konfo-indeksoija-service.queue.job
  (:require [clojure.tools.logging :as log]
            [clojurewerkz.quartzite.jobs :as j :refer [defjob]]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]]
            [konfo-indeksoija-service.util.conf :refer [env job-pool]]
            [konfo-indeksoija-service.queue.queue :as queue]))

(defjob handle-dlq-job [ctx] (queue/handle-failed))

(defn start-handle-dlq-job
  ([] (start-handle-dlq-job (:dlq-cron-string env)))
  ([cron-string]
   (log/info "Starting DLQ handling job!")
   (let [job (j/build
               (j/of-type handle-dlq-job)
               (j/with-identity "jobs.DLQ.1"))
         trigger (t/build
                   (t/with-identity (t/key "dlq-cron-trigger"))
                   (t/start-now)
                   (t/with-schedule
                      (schedule (cron-schedule cron-string))))]
     (log/info (str "Starting DLQ handling with cron schedule " cron-string)
               (qs/schedule job-pool job trigger)))))
