(ns konfo-indeksoija-service.queue.sqs
  (:require [amazonica.aws.sqs :as sqs]))

(defn long-poll
  [queue]
  (sqs/receive-message
    :queue-url queue
    :max-number-of-messages 10
    :delete false
    :wait-time-seconds 20))

(defn short-poll
  [queue]
  (sqs/receive-message
    :queue-url queue
    :max-number-of-messages 10
    :delete false))