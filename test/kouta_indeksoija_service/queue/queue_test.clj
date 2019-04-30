(ns kouta-indeksoija-service.queue.queue-test
  (:require [amazonica.core :as amazonica]
            [amazonica.aws.sqs :as sqs]
            [cheshire.core :as json]
            [clojure.string :as str]
            [midje.sweet :refer :all]
            [cheshire.core :as json]
            [kouta-indeksoija-service.kouta.indexer :as indexer]
            [kouta-indeksoija-service.queue.state :as state]
            [kouta-indeksoija-service.queue.localstack :as localstack]
            [kouta-indeksoija-service.util.collections :as coll]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.queue.queue :refer :all]))


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


(fact "'combine-messages' should combine categorized oids from messages into one map"
      (combine-messages
        [{:koulutukset ["k.123.1" "k.234.1" "k.456.1"] :toteutukset ["t.123.1" "t.234.1" "t.456.1"] :haut ["h.123.1" "h.234.1" "h.456.1"]}
         {:koulutukset ["k.123.2" "k.234.2"] :toteutukset ["t.123.2" "t.234.2" "t.456.2" "t.567.2"] :haut ["h.123.2" "h.234.2"]}
         {:hakukohteet ["hk.123.3"]}])
      => {:koulutukset ["k.123.1" "k.234.1" "k.456.1" "k.123.2" "k.234.2"]
          :toteutukset ["t.123.1" "t.234.1" "t.456.1" "t.123.2" "t.234.2" "t.456.2" "t.567.2"]
          :haut ["h.123.1" "h.234.1" "h.456.1" "h.123.2" "h.234.2"]
          :hakukohteet ["hk.123.3"]})

(fact "'combine-messages' should remove duplicates"
      (combine-messages
        [{:koulutukset ["k.123.1" "k.234.1" "k.456.1"] :toteutukset ["t.123.1" "t.234.1" "t.456.1"] :haut ["h.123.1" "h.234.1" "h.456.1"]}
         {:koulutukset ["k.123.1" "k.234.1" "k.456.1"] :toteutukset ["t.123.1" "t.234.1" "t.456.1"] :haut ["h.123.1" "h.234.1" "h.456.1"]}])
      => {:koulutukset ["k.123.1" "k.234.1" "k.456.1"] :toteutukset ["t.123.1" "t.234.1" "t.456.1"] :haut ["h.123.1" "h.234.1" "h.456.1"]})

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

(facts "SQS related tests that need Docker" :docker
  (let [expected-messages [(json/generate-string {:oid ["expected-123.123.123" "expected-123.123.231"] :boid ["expected-123.123"]})
                           (json/generate-string {:oid ["expected-234.234.234" "expected-234-234-123"] :droid ["expected-234.234"]})]
        not-expected-messages [(json/generate-string {:oid ["not-expected-321.321.321"] :noid ["not-expected-321.321"]})
                               (json/generate-string {:oid ["not-expected-432.432.432"] :boid ["not-expected-423.423"]})]]
    (against-background
      [(around :contents
               (do
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
            (let [handled (atom ())]
              (handle-messages-from-queues (fn [received] (swap! handled concat received)))
              @handled) => (at-least-one-of-only (map #(json/parse-string % true) expected-messages))
              (against-background
                [(before :facts (doseq [msg expected-messages] (sqs/send-message (queue :priority) msg)))]))

      (fact "'handle-messages-from-queues' should delete messages after successful handling"
            (let [deleted (atom [])]
              (with-redefs
                [sqs/delete-message (fn [& {:keys [queue-url]}] (swap! deleted conj queue-url))]
                (handle-messages-from-queues (constantly ()))
                @deleted)) => (n-of (queue :priority) (count expected-messages))
            (against-background
              [(before :facts (doseq [msg expected-messages] (sqs/send-message (queue :priority) msg)))]))

      (fact "'handle-messages-from-queues' should not delete messages before successful handling"
            (let [deleted (atom [])]
              (with-redefs
                [sqs/delete-message (fn [& {:keys [queue-url]}] (swap! deleted conj queue-url))]
                (try
                  (handle-messages-from-queues (fn [messages] (throw (new Exception))))
                  (catch Exception e))
                @deleted)) => []
            (against-background
              [(before :facts (doseq [msg expected-messages] (sqs/send-message (queue :priority) msg)))]))

      (fact "'handle-failed' should receive messages from DLQ and update their state to failed"
            (let [state-changes (atom [])]
              (with-redefs
                [state/set-state! (fn [state msg] (swap! state-changes conj [state (:body msg)]))]
                (handle-failed)
                @state-changes)) => (contains (map (fn [a] [::state/failed a]) expected-messages) :in-any-order)
            (against-background
              [(before :facts (doseq [msg expected-messages] (sqs/send-message (queue :dlq) msg)))]))

      (fact "'handle-failed' should receive messages from DLQ and delete them"
            (let [deleted (atom [])]queue
              (with-redefs
                [sqs/delete-message (fn [& {:keys [queue-url]}] (swap! deleted conj queue-url))]
                (handle-failed)
                @deleted)) => (n-of (queue :dlq) (count expected-messages))
            (against-background
              [(before :facts (doseq [msg expected-messages] (sqs/send-message (queue :dlq) msg)))]))

      (fact "'index-from-queue!' should return future"
            (let [f (index-from-queue!)
                  is-future (future? f)]
              (future-cancel f)
              is-future)) => truthy

      (fact "'index-from-queue!' should start listening on queue, receive messages and index them"
            (let [handled (atom [])]
              (with-redefs [indexer/index-oids (fn [oids] (swap! handled conj oids))]
                (let [f (index-from-queue!)]
                  (doseq [msg expected-messages] (sqs/send-message (queue :priority) msg))
                  (Thread/sleep 1000)
                  (future-cancel f)
                  (combine-messages @handled))))
            => (contains [{:oid ["expected-123.123.123" "expected-123.123.231" "expected-234.234.234" "expected-234-234-123"]
                           :boid ["expected-123.123"]
                           :droid ["expected-234.234"]}])))))
