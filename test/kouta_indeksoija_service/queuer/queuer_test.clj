(ns kouta-indeksoija-service.queuer.queuer-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.queuer.queuer :as q]
            [kouta-indeksoija-service.queue.sqs :refer [send-message]]
            [kouta-indeksoija-service.test-tools :refer [contains-same-elements-in-any-order?]]))

(def test-queue (atom []))
(defonce last-modified (System/currentTimeMillis))

(defn mock-queue-name
  [q]
  (if (= :fast q) "fast-queue" nil))

(defn mock-send-message
  [q m]
  (is (= "fast-queue" q))
  (swap! test-queue conj m))

(defn mock-eperuste-oids
  []
  (vec (map (fn [x] {:oid (str x)}) (range 21))))

(defn mock-organisaatio-oids
  []
  (vec (map (fn [x] (str x)) (range 21))))

(defn mock-eperuste-changes
  [l]
  [{:oid "111"} {:oid "222"} {:oid "333"}])

(defn mock-organisaatio-changes
  [l]
  [{:oid "555"} {:oid "666"} {:oid "777"}])

(deftest queuer-test
  (testing "Queuer should"
    (with-redefs [kouta-indeksoija-service.queue.queue/queue mock-queue-name
                  kouta-indeksoija-service.queue.sqs/send-message mock-send-message
                  kouta-indeksoija-service.rest.eperuste/find-all mock-eperuste-oids
                  kouta-indeksoija-service.rest.organisaatio/get-all-oids mock-organisaatio-oids
                  kouta-indeksoija-service.rest.eperuste/find-changes mock-eperuste-changes
                  kouta-indeksoija-service.rest.organisaatio/find-last-changes mock-organisaatio-changes
                  kouta-indeksoija-service.queuer.last-queued/get-last-queued-time (fn [] last-modified)
                  kouta-indeksoija-service.queuer.last-queued/set-last-queued-time (fn [t] (is (< last-modified t)))]

      (testing "queue eperuste"
        (q/queue-eperuste "123")
        (is (= [{:eperusteet ["123"]}] @test-queue))
        (reset! test-queue []))

      (testing "queue all eperusteet"
        (q/queue-all-eperusteet)
        (is (= [{:eperusteet (vec (map str (range 20)))} {:eperusteet ["20"]}] @test-queue))
        (reset! test-queue []))

      (testing "queue organisaatio"
        (q/queue-organisaatio "123")
        (is (= [{:organisaatiot ["123"]}] @test-queue))
        (reset! test-queue []))

      (testing "queue all organisaatiot"
        (q/queue-all-organisaatiot)
        (is (= [{:organisaatiot (vec (map str (range 20)))} {:organisaatiot ["20"]}] @test-queue))
        (reset! test-queue []))

      (testing "queue changes"
        (q/queue-changes)
        (is (= 1 (count @test-queue)))
        (is (contains-same-elements-in-any-order? ["111" "222" "333"] (-> @test-queue (first) :eperusteet)))
        (is (contains-same-elements-in-any-order? ["555" "666" "777"] (-> @test-queue (first) :organisaatiot)))
        (reset! test-queue [])))))