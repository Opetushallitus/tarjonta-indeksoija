(ns kouta-indeksoija-service.queue.sqs
  (:require [amazonica.core :as amazonica]
            [amazonica.aws.sqs :as sqs]
            [kouta-indeksoija-service.queue.conf :as conf]
            [clojure.string :refer [blank?]]
            [cheshire.core :refer [generate-string]])
  (:import (com.amazonaws.services.sqs.model QueueDoesNotExistException)))

(def long-poll-wait-time    20)
(def max-number-of-messages 10)

(defn- with-endpoint
  [f]
  (if (not (blank? conf/sqs-endpoint))
    (amazonica/with-credential {:endpoint conf/sqs-endpoint} (f))
    (f)))

(defn find-queue
  [name]
  (if-let [q (with-endpoint #(sqs/find-queue name))]
    q
    (throw (QueueDoesNotExistException. (str "No queue '" name "' found")))))

(defn queue
  [priority]
  (find-queue (conf/name priority)))

(defn delete-message
  [& {:keys [queue-url receipt-handle]}]
  (with-endpoint #(sqs/delete-message
                    :queue-url queue-url
                    :receipt-handle receipt-handle)))

(defn long-poll
  [queue]
  (with-endpoint #(sqs/receive-message
                    :queue-url queue
                    :max-number-of-messages max-number-of-messages
                    :delete false
                    :wait-time-seconds long-poll-wait-time)))

(defn short-poll
  [queue]
  (with-endpoint #(sqs/receive-message
                    :queue-url queue
                    :max-number-of-messages max-number-of-messages
                    :delete false)))

(defn send-message
  [queue message]
  (let [true-message (if (string? message) message (generate-string message))]
    (with-endpoint #(sqs/send-message
                     :queue-url queue
                     :message-body true-message))))

(defn get-queue-attributes
  [priority & attr]
  (with-endpoint #(sqs/get-queue-attributes (queue priority) attr)))