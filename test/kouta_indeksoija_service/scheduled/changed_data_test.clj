(ns kouta-indeksoija-service.scheduled.changed-data-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.scheduled.jobs :as jobs]
            [kouta-indeksoija-service.test-tools :as tools]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.test-tools :refer [contains-same-elements-in-any-order?]]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos :as oppilaitos]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos-search]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]))

(def test-queue (atom []))
(defonce last-modified (System/currentTimeMillis))
(def muuttunut-oppilaitos-oid1 "1.2.246.562.10.54453921329")
(def muuttunut-oppilaitos-oid2 "1.2.246.562.10.197113642410")

(defn mock-queue-name
  [q]
  (if (= :fast q) "fast-queue" nil))

(defn mock-send-message
  [q m]
  (is (= "fast-queue" q))
  (swap! test-queue conj m))

(defn mock-organisaatio-changes [l]
  (tools/parse (str "test/resources/organisaatiot/last-modified.json")))

(defn mock-eperuste-changes
  [l]
  ["111" "222" "333"])


(deftest changed-data-test
  (kouta-indeksoija-service.elastic.admin/initialize-indices)
  (fixture/with-mocked-indexing
   (with-redefs [kouta-indeksoija-service.queue.sqs/queue mock-queue-name
                 kouta-indeksoija-service.queue.sqs/send-message mock-send-message
                 kouta-indeksoija-service.queuer.last-queued/get-last-queued-time (fn [] last-modified)
                 kouta-indeksoija-service.queuer.last-queued/set-last-queued-time (fn [t] (is (< last-modified t)))]
    (testing "Changed data update should"
      (testing "handle changes correctly when both eperuste- and organisation -changes"
        (with-redefs [kouta-indeksoija-service.rest.organisaatio/find-last-changes mock-organisaatio-changes
                      kouta-indeksoija-service.rest.eperuste/find-changes mock-eperuste-changes]
          (fixture/delete-all-elastic-data)
          (reset! test-queue [])
          (jobs/handle-and-queue-changed-data)
          (is (= muuttunut-oppilaitos-oid1 (:oid (get-doc oppilaitos/index-name muuttunut-oppilaitos-oid1))))
          (is (= muuttunut-oppilaitos-oid2 (:oid (get-doc oppilaitos/index-name muuttunut-oppilaitos-oid2))))
          (is (nil? (:oid (get-doc oppilaitos-search/index-name muuttunut-oppilaitos-oid1)))) ;; ei löydy koulutuksia
          (is (nil? (:oid (get-doc oppilaitos-search/index-name muuttunut-oppilaitos-oid2)))) ;; ei löydy koulutuksia
          (is (contains-same-elements-in-any-order? ["111" "222" "333"] (-> @test-queue (first) :eperusteet)))))

      (testing "handle changes correctly when organisation-changes only"
        (with-redefs [kouta-indeksoija-service.rest.organisaatio/find-last-changes mock-organisaatio-changes
                      kouta-indeksoija-service.rest.eperuste/find-changes (fn [l] [])]
          (fixture/delete-all-elastic-data)
          (reset! test-queue [])
          (jobs/handle-and-queue-changed-data)
          (is (= muuttunut-oppilaitos-oid1 (:oid (get-doc oppilaitos/index-name muuttunut-oppilaitos-oid1))))
          (is (= muuttunut-oppilaitos-oid2 (:oid (get-doc oppilaitos/index-name muuttunut-oppilaitos-oid2))))
          (is (= [] @test-queue))))

      (testing "handle changes correctly when eperuste-changes only"
        (with-redefs [kouta-indeksoija-service.rest.organisaatio/find-last-changes (fn [l] [])
                      kouta-indeksoija-service.rest.eperuste/find-changes mock-eperuste-changes]
          (fixture/delete-all-elastic-data)
          (reset! test-queue [])
          (jobs/handle-and-queue-changed-data)
          (is (nil? (:oid (get-doc oppilaitos/index-name muuttunut-oppilaitos-oid1))))
          (is (nil? (:oid (get-doc oppilaitos/index-name muuttunut-oppilaitos-oid2))))
          (is (contains-same-elements-in-any-order? ["111" "222" "333"] (-> @test-queue (first) :eperusteet)))))

      (testing "cope with situation when no changes"
        (with-redefs [kouta-indeksoija-service.rest.organisaatio/find-last-changes (fn [l] [])
                      kouta-indeksoija-service.rest.eperuste/find-changes (fn [l] [])]
          (fixture/delete-all-elastic-data)
          (reset! test-queue [])
          (jobs/handle-and-queue-changed-data)
          (is (nil? (:oid (get-doc oppilaitos/index-name muuttunut-oppilaitos-oid1))))
          (is (nil? (:oid (get-doc oppilaitos/index-name muuttunut-oppilaitos-oid2))))
          (is (= [] @test-queue))))))))

