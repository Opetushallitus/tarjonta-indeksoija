(ns konfo-indeksoija-service.queue.queue
  (:require [amazonica.aws.sqs :as sqs]
            [clojure.tools.logging :as log]
            [clj-log.error-log :refer [with-error-logging]]
            [cheshire.core :as json]
            [konfo-indeksoija-service.util.conf :refer [env]]
            [konfo-indeksoija-service.queue.sqs :as queue-sqs]
            [konfo-indeksoija-service.queue.state :as state]
            [konfo-indeksoija-service.util.seq :as seq]))


(defn- handle-index
  [messages]
  ;; TODO Do indexing
  )

;; TODO queue names from config
(def queues {:priority (sqs/find-queue "indexing-priority")
             :fast (sqs/find-queue "indexing-fast")
             :slow (sqs/find-queue "indexing-slow")})

(def dead-letter-queue (sqs/find-queue "dlq"))              ;; TODO

(defn receive [{:keys [q f]}] { :queue q :messages (f q) })

(defn- receive-messages-from-queues
  []
  (seq/collect-first
    receive
    #(seq (:messages %))
    [{:q (:priority queues) :f queue-sqs/long-poll}
     {:q (:fast queues) :f queue-sqs/short-poll}
     {:q (:slow queues) :f queue-sqs/short-poll}]))


(defn body-json->map
  [msg]
  (json/parse-string (queue-sqs/body msg) true))

(defn- handle-messages-from-queues
  ([handler] (handle-messages-from-queues handler body-json->map))
  ([handler unwrap]
   (let [received receive-messages-from-queues
         messages (:messages received)]
     (when (seq messages)
       (handler (map unwrap messages))
       (doseq
         [msg (map queue-sqs/receipt-handle messages)]
         (sqs/delete-message :queue-url (:queue received)
                             :receipt-handle msg))))))


(defn- receive-indexing-messages
  []
  (try
    (handle-messages-from-queues
      (fn
        [messages]
        (doseq [step [#(state/set-all ::state/started %)
                      handle-index
                      #(state/set-all ::state/indexed %)]]
                (step messages))))
    (catch Exception e (log/error e "Error in receiving indexing messages. Continuing polling."))))


(defn index-from-queue!
  []
  (future
    (loop [] (do (receive-indexing-messages) (recur)))
    (log/warn "Stopped listening on queues.")))


;; TODO add to scheduling
(defn handle-failed
  []
  (let [failed (queue-sqs/short-poll dead-letter-queue)]
    (state/set-all ::state/failed failed)))