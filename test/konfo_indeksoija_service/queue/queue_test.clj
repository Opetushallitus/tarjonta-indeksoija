(ns konfo-indeksoija-service.queue.queue-test
  (:require [amazonica.core :as amazonica]
            [amazonica.aws.sqs :as sqs]
            [cheshire.core :as json]
            [clojure.string :as str]
            [midje.sweet :refer :all]
            [cheshire.core :as json]
            [konfo-indeksoija-service.queue.localstack :as localstack]
            [konfo-indeksoija-service.util.collections :as coll]
            [konfo-indeksoija-service.util.conf :refer [env]]
            [konfo-indeksoija-service.queue.queue :refer :all]))


(defn- mock-receive [queue] {:messages (seq (reverse queue))})

(defn- mock-receive2 [queue] {:messages (seq (str/upper-case queue))})

(defn- sqs-message
  [body]
  (amazonica/get-fields
    (let [msg (new com.amazonaws.services.sqs.model.Message)]
      (.setBody msg body)
      msg)))

(defn- uuid [] (.toString (java.util.UUID/randomUUID)))

(defn- with-queues [f]
  (let [queues {:priority (str "priority-" (uuid))
                :fast (str "fast-" (uuid))
                :slow (str "slow-" (uuid))
                :dlq (str "dlq-"  (uuid))}]
    (with-redefs [env {:queue queues}] (f queues))))

(defn- message-bodies [response] (seq (map #(:body %) (:messages response))))

(defchecker at-least-one-of-only [expected-elements]
            (chatty-checker [actual]
                            (and
                              (not-empty actual)
                              (every? #(coll/in? expected-elements %) actual))))



(fact "'receive' response should have :queue and :messages keys"
      (set (keys (receive {:q "queue" :f mock-receive}))) => (set [:messages :queue]))

(fact "'receive' should call parameter ':f' with value ':q' and return response in ':messages'"
      (:messages (receive {:q "queue" :f mock-receive})) => (:messages (mock-receive "queue"))
      (:messages (receive {:q "foobar" :f mock-receive2})) => (:messages (mock-receive2 "foobar")))

(fact "'receive' should return input ':q' parameter as ':queue'"
      (:queue (receive {:q "queue" :f mock-receive})) => "queue"
      (:queue (receive {:q "foobar" :f mock-receive})) => "foobar")

(fact "'body-json->map' should return json string from message body as map"
      (let [content {:oid "123.123.123"
                     :foobar "bar"}
            msg (sqs-message (json/generate-string content))]
        (body-json->map msg) => content))

(fact "'body-json->map' should fail on non-json body"
      (let [msg (sqs-message "non-json-stuff")]
        (body-json->map msg) => (throws com.fasterxml.jackson.core.JsonParseException)))

(let [expected-messages [(json/generate-string {:oid "expected-123.123.123"})
                         (json/generate-string {:oid "expected-234.234.234"})]
      not-expected-messages [(json/generate-string {:oid "not-expected-321.321.321"})
                             (json/generate-string {:oid "not-expected-432.432.432"})]]
    (against-background
         [(around :contents (do
                              (localstack/start)
                              (amazonica/with-credential {:endpoint (localstack/sqs-endpoint)} ?form)
                              (localstack/stop)))
          (around :facts (with-queues
                           (fn [queues]
                             (doseq [q (vals queues)] (sqs/create-queue q))
                             ?form
                             (doseq [q (vals queues)] (sqs/delete-queue (sqs/find-queue q))))))]

         (let [base_sqs (str (localstack/sqs-endpoint) "/queue/")]
           (fact "'queues' should contain SQS queues"
                  (queue :priority) => (has-prefix (str base_sqs "priority-"))
                  (queue :fast) => (has-prefix (str base_sqs "fast-"))
                  (queue :slow) => (has-prefix (str base_sqs "slow-"))
                  (queue :dlq) => (has-prefix (str base_sqs "dlq-"))))

         (facts "'receive-messages-from-queues' should receive messages from queues in correct order"
                (fact "'receive-messages-from-queues' should receive messages from :priority queue first"
                      (message-bodies (receive-messages-from-queues)) => (at-least-one-of-only expected-messages)
                      (against-background
                        [(before :facts (do
                                          (doseq [msg expected-messages] (sqs/send-message (queue :priority) msg))
                                          (doseq [msg not-expected-messages] (sqs/send-message (queue :fast) msg))
                                          (doseq [msg not-expected-messages] (sqs/send-message (queue :slow) msg))))]))

                (fact "'receive-messages-from-queues' should receive messages from :priority even if they appear after started polling (use long poll)"
                      (let [receive (future (receive-messages-from-queues))]
                        (Thread/sleep 2000)
                        (doseq [msg expected-messages] (sqs/send-message (queue :priority) msg))
                        (message-bodies @receive)) => (at-least-one-of-only expected-messages)
                      (against-background
                        [(before :facts (do
                                          (doseq [msg not-expected-messages] (sqs/send-message (queue :fast) msg))
                                          (doseq [msg not-expected-messages] (sqs/send-message (queue :slow) msg))))]))

                (fact "'receive-messages-from-queues' should receive messages from :fast queue if there are no messages in :priority queue"
                      (message-bodies (receive-messages-from-queues)) => (at-least-one-of-only expected-messages)
                      (against-background
                        [(before :facts (do
                                          (doseq [msg expected-messages] (sqs/send-message (queue :fast) msg))
                                          (doseq [msg not-expected-messages] (sqs/send-message (queue :slow) msg))))]))

                (fact "'receive-messages-from-queues' should receive messages from :slow queue only if there are no messages in other queues"
                      (message-bodies (receive-messages-from-queues)) => (at-least-one-of-only expected-messages)
                      (against-background
                        [(before :facts (doseq [msg expected-messages] (sqs/send-message (queue :slow) msg)))]))
                (fact "'receive-messages-from-queues' should wait for at 20 seconds if there are no messages in any queue"
                      (let [start (System/currentTimeMillis)]
                        (receive-messages-from-queues)
                        (- (System/currentTimeMillis) start)) => (roughly 20000 2000)))

         (fact "'handle-messages-from-queues' should receive parsed messages from queues and call 'handler' function for them"
               (let [handled (atom ())
                     handler (fn [received] (swap! handled concat received))]
                 (handle-messages-from-queues handler)
                 @handled) => (at-least-one-of-only (map #(json/parse-string % true) expected-messages))
               (against-background
                 [(before :facts (doseq [msg expected-messages] (sqs/send-message (queue :priority) msg)))]))

         (future-fact "'handle-messages-from-queues' should delete messages after successful handling"
               ()) ;; TODO how to check messages are deleted or not?
         (future-fact "'handle-messages-from-queues' should not delete messages before successful handling"
               ()))) ;; TODO how to check messages are deleted or not?

(future-fact "'index-from-queue!' should receive messages from queue and index them"
      ()) ;; TODO would require actually indexing messages
(future-fact "'index-from-queue!' should return future"
      ()) ;; TODO would require actually indexing messages

(future-fact "'handle-failed' should receive messages from DLQ and update their state to failed"
      ()) ;; TODO






