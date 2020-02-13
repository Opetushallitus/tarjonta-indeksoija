(ns kouta-indeksoija-service.queue.admin
  (:require [kouta-indeksoija-service.queue.sqs :as sqs]
            [kouta-indeksoija-service.queue.conf :as conf]
            [clojure.tools.logging :as log]))

(defn status
  []
  (->> (for [priority (conf/priorities)]
         {(keyword priority) (sqs/get-queue-attributes priority)})
       (into {})))

(defn healthcheck
  []
  (let [status (atom 200)
        body   (try
                 (->> (for [priority (conf/priorities)
                            :let [health-threshold  (conf/health-threshold priority)
                                  queue-attributes (sqs/get-queue-attributes priority "ApproximateNumberOfMessages" "QueueArn")
                                  apprx-messages   (some-> queue-attributes :ApproximateNumberOfMessages)
                                  nr-of-messages   (or (Integer/parseInt apprx-messages) -1)
                                  healthy?         (<= nr-of-messages health-threshold)]]
                        (do
                          (when (not healthy?)
                            (reset! status 500))
                          {(keyword priority) {:QueueArn (some-> queue-attributes :QueueArn)
                                               :ApproximateNumberOfMessages nr-of-messages
                                               :healthy healthy?
                                               :health-threshold health-threshold}}))
                      (into {}))
                    (catch Exception e
                      (reset! status 500)
                      (log/error e)
                      {:error (.getMessage e)}))]
    [@status body]))