(ns kouta-indeksoija-service.queue.sqs-queue-test
  (:require [clojure.test :refer :all]
            [amazonica.core :as amazonica]
            [amazonica.aws.sqs :as sqs]
            [cheshire.core :as json]
            [cheshire.core :as json]
            [kouta-indeksoija-service.indexer.indexer :as indexer]
            [kouta-indeksoija-service.queue.state :as state]
            [kouta-indeksoija-service.queue.localstack :as localstack]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.queue.queue :refer :all]
            [kouta-indeksoija-service.queue.sqs :refer [queue]]
            [kouta-indeksoija-service.test-tools :refer [contains-same-elements-in-any-order? contains-elements-in-any-order?]]
            [kouta-indeksoija-service.queue.notification-queue :as notification-queue]))

(defonce test-long-poll-time 5)

(defn- uuid [] (.toString (java.util.UUID/randomUUID)))

(defonce queues {:priority (str "priority-" (uuid))
                 :fast (str "fast-" (uuid))
                 :slow (str "slow-" (uuid))
                 :dlq (str "dlq-"  (uuid))
                 :notifications (str "notifications-"  (uuid))
                 :notifications-dlq (str "notifications-dlq-"  (uuid))})

(defn- message-bodies [response] (seq (map #(:body %) (:messages response))))

(defn docker-test-fixture
  [tests]
  (localstack/start)
  (amazonica/with-credential {:endpoint (localstack/sqs-endpoint)} (tests))
  (localstack/stop))

(defn mount-fixture
  [tests]
  (mount.core/start)
  (tests)
  (mount.core/stop))

(defn call-for-queue-urls
  [f]
  (let [endpoint (localstack/sqs-endpoint)]
    (doseq [q (vals queues)] (f (str endpoint "/queue/" q)))))

(defn queues-fixture
  [test]
  (doseq [q (vals queues)] (sqs/create-queue q))
  (test)
  (call-for-queue-urls sqs/delete-queue))

(use-fixtures :once mount-fixture docker-test-fixture)
(use-fixtures :each queues-fixture)

(defn empty-queues
  []
  (call-for-queue-urls #(sqs/receive-message
                          :queue-url %
                          :max-number-of-messages 10
                          :delete true )))

(defmacro testing-with-queues-fixture
  [name & test]
  `(testing ~name
     (do
       ~@test
       (empty-queues))))

(deftest docker-sqs-test
  (with-redefs [env {:queue queues}
                kouta-indeksoija-service.queue.sqs/long-poll-wait-time test-long-poll-time]
  (let [expected-messages     [(json/generate-string {:oid ["expected-123.123.123" "expected-123.123.231"] :boid ["expected-123.123"]})
                               (json/generate-string {:oid ["expected-234.234.234" "expected-234-234-123"] :droid ["expected-234.234"]})]
        not-expected-messages [(json/generate-string {:oid ["not-expected-321.321.321"] :noid ["not-expected-321.321"]})
                               (json/generate-string {:oid ["not-expected-432.432.432"] :boid ["not-expected-423.423"]})]]

    (testing-with-queues-fixture "Queue should contain SQS queues"
      (let [base_sqs (str (localstack/sqs-endpoint) "/queue/")]
        (is (clojure.string/starts-with? (queue :priority)          (str base_sqs "priority-")))
        (is (clojure.string/starts-with? (queue :fast)              (str base_sqs "fast-")))
        (is (clojure.string/starts-with? (queue :slow)              (str base_sqs "slow-")))
        (is (clojure.string/starts-with? (queue :dlq)               (str base_sqs "dlq-")))
        (is (clojure.string/starts-with? (queue :notifications)     (str base_sqs "notifications-")))
        (is (clojure.string/starts-with? (queue :notifications-dlq) (str base_sqs "notifications-dlq-")))))

    (testing "Receive-messages-from-queues should"
      (testing-with-queues-fixture "receive messages from :priority queue first"
        (doseq [msg expected-messages] (sqs/send-message (queue :priority) msg))
        (doseq [msg not-expected-messages] (sqs/send-message (queue :fast) msg))
        (doseq [msg not-expected-messages] (sqs/send-message (queue :slow) msg))
        (let [received-message-bodies (message-bodies (receive-messages-from-queues))]
          (is (contains-same-elements-in-any-order? expected-messages received-message-bodies))))

      (testing-with-queues-fixture "receive messages from :priority even if they appear after started polling (use long poll)"
        (doseq [msg not-expected-messages] (sqs/send-message (queue :fast) msg))
        (doseq [msg not-expected-messages] (sqs/send-message (queue :slow) msg))
        (let [receive (future (receive-messages-from-queues))]
          (doseq [msg expected-messages] (sqs/send-message (queue :priority) msg))
          (is (contains-elements-in-any-order? expected-messages (message-bodies @receive))))) ;TODO Miksi ei contains-same-elements-in-any-order? toimi?

      (testing-with-queues-fixture "receive messages from :fast queue if there are no messages in :priority queue"
        (doseq [msg expected-messages] (sqs/send-message (queue :fast) msg))
        (doseq [msg not-expected-messages] (sqs/send-message (queue :slow) msg))
        (is (contains-same-elements-in-any-order? expected-messages (message-bodies (receive-messages-from-queues)))))

      (testing-with-queues-fixture "receive messages from :slow queue only if there are no messages in other queues"
        (doseq [msg expected-messages] (sqs/send-message (queue :slow) msg))
        (is (contains-same-elements-in-any-order? expected-messages (message-bodies (receive-messages-from-queues)))))

      (testing-with-queues-fixture "long poll if there are no messages in any queue"
        (let [start (System/currentTimeMillis)]
          (message-bodies (receive-messages-from-queues))
          (is (< (- (* 1000 test-long-poll-time) 500)
                 (- (System/currentTimeMillis) start)
                 (+ (* 1000 test-long-poll-time) 500))))))

    (testing "Handle-messages-from-queues should"

      (testing-with-queues-fixture "receive parsed messages from queues and call 'handler' function for them"
        (doseq [msg expected-messages] (sqs/send-message (queue :priority) msg))
        (is (= (contains-same-elements-in-any-order? (map #(json/parse-string % true) expected-messages)
                                                     (let [handled (atom ())]
                                                       (handle-messages-from-queues (fn [received] (swap! handled concat received)))
                                                       @handled)))))

      (testing-with-queues-fixture "delete messages after successful handling"
        (doseq [msg expected-messages] (sqs/send-message (queue :priority) msg))
        (let [priority-queue (queue :priority)
              deleted        (atom [])]
          (with-redefs [sqs/delete-message (fn [& {:keys [queue-url]}] (swap! deleted conj queue-url))]
            (handle-messages-from-queues (constantly ()))
            (is (= (count expected-messages) (count (filter #(= priority-queue %) @deleted)))))))

      (testing-with-queues-fixture "not delete messages before successful handling"
        (doseq [msg expected-messages] (sqs/send-message (queue :priority) msg))
        (let [deleted (atom [])]
          (with-redefs [sqs/delete-message (fn [& {:keys [queue-url]}] (swap! deleted conj queue-url))]
            (try
              (handle-messages-from-queues (fn [messages] (throw (new Exception))))
              (catch Exception e)))
          (is (= [] @deleted)))))

    (testing "Notification-queue handle-messages-from-queues should"

      (testing-with-queues-fixture "receive parsed messages from queues and call 'handler' function for them"
        (doseq [msg expected-messages] (sqs/send-message (queue :notifications) msg))
        (is (= (contains-same-elements-in-any-order? (map #(json/parse-string % true) expected-messages)
                                                     (let [handled (atom ())]
                                                       (notification-queue/handle-messages-from-queues (fn [received] (swap! handled concat received)))
                                                       @handled)))))

      (testing-with-queues-fixture "delete messages after successful handling"
        (doseq [msg expected-messages] (sqs/send-message (queue :notifications) msg))
        (let [notifications (queue :notifications)
              deleted        (atom [])]
          (with-redefs [sqs/delete-message (fn [& {:keys [queue-url]}] (swap! deleted conj queue-url))]
            (notification-queue/handle-messages-from-queues (constantly ()))
            (is (= (count expected-messages) (count (filter #(= notifications %) @deleted)))))))

      (testing-with-queues-fixture "not delete messages before successful handling"
        (doseq [msg expected-messages] (sqs/send-message (queue :notifications) msg))
        (let [deleted (atom [])]
          (with-redefs [sqs/delete-message (fn [& {:keys [queue-url]}] (swap! deleted conj queue-url))]
            (try
              (notification-queue/handle-messages-from-queues (fn [messages] (throw (new Exception))))
              (catch Exception e)))
          (is (= [] @deleted)))))

    (comment testing "Handle-failed should"

      (testing-with-queues-fixture "receive messages from DLQ and update their state to failed"
        (doseq [msg expected-messages] (sqs/send-message (queue :dlq) msg))
        (let [state-changes (atom [])]
          (with-redefs [state/set-state! (fn [state msg] (swap! state-changes conj [state (:body msg)]))]
            (handle-failed))
        (is (contains-same-elements-in-any-order? (map (fn [a] [::state/failed a]) expected-messages) @state-changes))))

      (testing-with-queues-fixture "receive messages from DLQ and delete them"
        (doseq [msg expected-messages] (sqs/send-message (queue :dlq) msg))
        (let [deleted   (atom [])
              dlq-queue (queue :dlq)]
          (with-redefs [sqs/delete-message (fn [& {:keys [queue-url]}] (swap! deleted conj queue-url))]
            (handle-failed)
            (is (= (count expected-messages) (count (filter #(= dlq-queue %) @deleted))))))))

    (comment testing "Index-from-queue! should"

      (testing-with-queues-fixture "start listening on queue, receive messages and index them"
        (let [handled (atom [])]
          (with-redefs [indexer/index-oids (fn [oids] (swap! handled conj oids))]
            (let [f (index-from-queue!)]
              (is future? f)
              (doseq [msg expected-messages] (sqs/send-message (queue :priority) msg))
              (Thread/sleep 1000)
              (future-cancel f)
              (is (= {:oid ["expected-123.123.123" "expected-123.123.231" "expected-234.234.234" "expected-234-234-123"]
                      :boid ["expected-123.123"]
                      :droid ["expected-234.234"]}
                     (combine-messages @handled)))))))))))
