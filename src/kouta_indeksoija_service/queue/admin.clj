(ns kouta-indeksoija-service.queue.admin
  (:require [kouta-indeksoija-service.queue.sqs :as sqs]
            [kouta-indeksoija-service.queue.conf :as conf]
            [clojure.tools.logging :as log]))

(defn status
  []
  (->> (for [priority (conf/priorities)]
         {(keyword priority) (sqs/get-queue-attributes priority)})
       (into {})))

(defn- parse-int
  [x]
  (if (not (number? x))
    (try
      (Integer/parseInt x)
      (catch Exception e
        nil))
    x))

(defn- healthy?
  [apprx-messages health-threshold]
  (if-let [nr-of-messages (parse-int apprx-messages)]
    (<= nr-of-messages health-threshold)
    false))

(defn healthcheck
  []
  (try
    (let [result (for [priority (conf/priorities)
                       :let [health-threshold (or (parse-int (conf/health-threshold priority)) 20)
                             queue-attributes (sqs/get-queue-attributes priority "ApproximateNumberOfMessages" "QueueArn")
                             apprx-messages   (some-> queue-attributes :ApproximateNumberOfMessages)
                             health           (healthy? apprx-messages health-threshold)]]
                   [health {(keyword priority) {:QueueArn (some-> queue-attributes :QueueArn)
                                                :ApproximateNumberOfMessages apprx-messages
                                                :healthy health
                                                :health-threshold health-threshold}}])]
      [(not-any? false? (map first result)) (into {} (map second result))])
    (catch Exception e
      (log/error e)
      [false {:error (.getMessage e)}])))