(ns kouta-indeksoija-service.scheduled.scheduled-jobs-test
  (:require [clojure.test :refer :all]
            [mount.core :as mount]
            [kouta-indeksoija-service.scheduled.jobs :as jobs]
            [kouta-indeksoija-service.util.conf :refer [env]]))

(deftest scheduled-jobs-test
  (mount/start)
  (let [dlq-called (atom false)
        sqs-called (atom false)
        que-called (atom false)]
    (with-redefs [env {:dlq-cron-string "*/1 * * ? * *" :queueing-cron-string "*/1 * * ? * *"}
                  kouta-indeksoija-service.queue.queue/clean-dlq (fn [] (reset! dlq-called true))
                  kouta-indeksoija-service.queue.queue/index-from-sqs (fn [] (do (reset! sqs-called true) (loop [] (recur))))
                  kouta-indeksoija-service.indexer.queue/queue-changes (fn [] (reset! que-called true))]

    (testing "Jobs should"
      (testing "schedule all jobs"
        (is (= 0 (count (jobs/get-jobs-info))))
        (is (false? (or @dlq-called @que-called @sqs-called)))
        (jobs/schedule-jobs)
        (is (= 3 (count (jobs/get-jobs-info))))
        (Thread/sleep 1000)
        (is (and @dlq-called @que-called @sqs-called)))

      (testing "reset all jobs"
        (is (= 3 (count (jobs/get-jobs-info))))
        (jobs/reset-jobs)
        (is (= 0 (count (jobs/get-jobs-info))))))

      (testing "pause and resume jobs"
        (jobs/schedule-jobs)
        (let [job-state (fn [name] (first (map :state (:triggers (first (filter #(= (str "DEFAULT.jobs." name ) (:job %)) (jobs/get-jobs-info)))))))]
          (is (= "NORMAL" (job-state jobs/dlq-job-name)))
          (is (= "NORMAL" (job-state jobs/sqs-job-name)))
          (is (= "NORMAL" (job-state jobs/queueing-job-name)))
          (jobs/pause-dlq-job)
          (is (= "PAUSED" (job-state jobs/dlq-job-name)))
          (is (= "NORMAL" (job-state jobs/sqs-job-name)))
          (is (= "NORMAL" (job-state jobs/queueing-job-name)))
          (jobs/pause-sqs-job)
          (is (= "PAUSED" (job-state jobs/dlq-job-name)))
          (is (= "PAUSED" (job-state jobs/sqs-job-name)))
          (is (= "NORMAL" (job-state jobs/queueing-job-name)))
          (jobs/pause-queueing-job)
          (is (= "PAUSED" (job-state jobs/dlq-job-name)))
          (is (= "PAUSED" (job-state jobs/sqs-job-name)))
          (is (= "PAUSED" (job-state jobs/queueing-job-name)))
          (jobs/resume-dlq-job)
          (is (= "NORMAL" (job-state jobs/dlq-job-name)))
          (is (= "PAUSED" (job-state jobs/sqs-job-name)))
          (is (= "PAUSED" (job-state jobs/queueing-job-name)))
          (jobs/resume-sqs-job)
          (is (= "NORMAL" (job-state jobs/dlq-job-name)))
          (is (= "NORMAL" (job-state jobs/sqs-job-name)))
          (is (= "PAUSED" (job-state jobs/queueing-job-name)))
          (jobs/resume-queueing-job)
          (is (= "NORMAL" (job-state jobs/dlq-job-name)))
          (is (= "NORMAL" (job-state jobs/sqs-job-name)))
          (is (= "NORMAL" (job-state jobs/queueing-job-name))))))))