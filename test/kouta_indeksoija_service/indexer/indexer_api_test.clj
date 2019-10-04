(ns kouta-indeksoija-service.indexer.indexer-api-test
  (:require [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [clojure.test :refer :all]
            [clojure.data :refer [diff]]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer :all]
            [kouta-indeksoija-service.indexer.indexer-api :as i]
            [kouta-indeksoija-service.indexer.kouta.haku :as haku]
            [kouta-indeksoija-service.indexer.kouta.hakukohde :as hakukohde]
            [kouta-indeksoija-service.indexer.kouta.koulutus :as koulutus]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as search]
            [kouta-indeksoija-service.indexer.kouta.toteutus :as toteutus]
            [kouta-indeksoija-service.indexer.kouta.valintaperuste :as valintaperuste]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.test-tools :refer [parse compare-json]]
            [mocks.notifier-target-mock :as notifier-target-mock]))

(use-fixtures :each
              notifier-target-mock/notifier-mock-fixture
              fixture/indices-fixture)

(use-fixtures :once common-indexer-fixture)

(defmacro with-mocked-notifications
  [& body]
  ;TODO: with-redefs is not thread safe and may cause unexpected behaviour.
  ;It can be temporarily fixed by using locked in mocking functions, but better solution would be superb!
  `(with-redefs [kouta-indeksoija-service.notifier.notifier/send-notification-messages
                 mocks.notifier-target-mock/add]
     (do ~@body)))

(deftest koulutus-notification-test
  (with-mocked-notifications
   (fixture/with-mocked-indexing
    (testing "Notifications about the koulutus shouldn't be sent if not enabled"
      (i/index-koulutus koulutus-oid false)
      (is (empty? (get @notifier-target-mock/received koulutus-oid))))
    (testing "Notifications about the koulutus should be sent after indexing if enabled"
      (i/index-koulutus koulutus-oid true)
      (compare-json (json "kouta-koulutus-notification")
                    (get @notifier-target-mock/received koulutus-oid))))))

(deftest toteutus-notification-test
  (with-mocked-notifications
   (fixture/with-mocked-indexing
    (testing "Notifications about the toteutus shouldn't be sent if not enabled"
      (i/index-toteutus toteutus-oid false)
      (is (empty? (get @notifier-target-mock/received toteutus-oid))))
    (testing "Notifications about the toteutus should be sent after indexing if enabled"
      (i/index-toteutus toteutus-oid true)
      (compare-json (json "kouta-toteutus-notification")
                    (get @notifier-target-mock/received toteutus-oid))))))

(deftest haku-notification-test
  (with-mocked-notifications
   (fixture/with-mocked-indexing
    (testing "Notifications about the haku shouldn't be sent if not enabled"
      (i/index-haku haku-oid false)
      (is (empty? (get @notifier-target-mock/received haku-oid))))
    (testing "Notifications about the haku should be sent after indexing"
      (i/index-haku haku-oid true)
      (compare-json (json "kouta-haku-notification")
                    (get @notifier-target-mock/received haku-oid))))))

(deftest hakukohde-notification-test
  (with-mocked-notifications
   (fixture/with-mocked-indexing
    (testing "Notifications about the hakukohde shouldn't be sent if not enabled"
      (i/index-hakukohde hakukohde-oid false)
      (is (empty? (get @notifier-target-mock/received hakukohde-oid))))
    (testing "Notifications about the hakukohde should be sent after indexing"
      (i/index-hakukohde hakukohde-oid true)
      (compare-json (json "kouta-hakukohde-notification")
                    (get @notifier-target-mock/received hakukohde-oid))))))

(deftest valintaperuste-notification-test
  (with-mocked-notifications
   (fixture/with-mocked-indexing
    (testing "Notifications about the valintaperuste shouldn't be sent if not enabled"
      (i/index-valintaperusteet [valintaperuste-id] false)
      (is (empty? (get @notifier-target-mock/received valintaperuste-id))))
    (testing "Notifications about the valintaperuste should be sent after indexing"
      (i/index-valintaperusteet [valintaperuste-id] true)
      (compare-json (json "kouta-valintaperuste-notification")
                    (get @notifier-target-mock/received valintaperuste-id))))))

(deftest oids-notification-test
  (with-mocked-notifications
   (fixture/with-mocked-indexing
    (testing "Notifications about all relevant objects shouldn't be sent if not enabled"
      (i/index-oids {:koulutukset [koulutus-oid]
                     :haut [haku-oid]
                     :hakukohteet [hakukohde-oid]
                     :toteutukset [toteutus-oid]
                     :valintaperusteet [valintaperuste-id]
                     :sorakuvaukset [sorakuvaus-id]
                     :oppilaitokset [oppilaitos-oid]} false)
      (are [id] (empty? (get @notifier-target-mock/received id))
                sorakuvaus-id
                oppilaitos-oid
                koulutus-oid
                haku-oid
                hakukohde-oid
                toteutus-oid
                valintaperuste-id))
    (testing "Notifications about all relevant objects should be sent if enabled"
      (i/index-oids {:koulutukset [koulutus-oid]
                     :haut [haku-oid]
                     :hakukohteet [hakukohde-oid]
                     :toteutukset [toteutus-oid]
                     :valintaperusteet [valintaperuste-id]
                     :sorakuvaukset [sorakuvaus-id]
                     :oppilaitokset [oppilaitos-oid]} true)
      (are [id] (contains? @notifier-target-mock/received id)
                koulutus-oid
                haku-oid
                hakukohde-oid
                toteutus-oid
                valintaperuste-id)
      (are [id] (empty? (get @notifier-target-mock/received id))
                sorakuvaus-id
                oppilaitos-oid)))))


(deftest index-all-koulutukset-test
  (with-mocked-notifications
   (fixture/with-mocked-indexing
    (testing "Indexer should index all koulutukset"
      (check-all-nil)
      (i/index-all-koulutukset false)
      (is (= nil (get-doc haku/index-name haku-oid)))
      (is (= nil (get-doc hakukohde/index-name hakukohde-oid)))
      (is (= nil (get-doc toteutus/index-name toteutus-oid)))
      (is (= koulutus-oid (:oid (get-doc koulutus/index-name koulutus-oid))))
      (is (= koulutus-oid (:oid (get-doc search/index-name koulutus-oid))))
      (is (= nil (get-doc valintaperuste/index-name valintaperuste-id)))
      (is (empty? @notifier-target-mock/received)))
    (testing "Notifications should be sent about all koulutukset if enabled"
      (i/index-all-koulutukset true)
      (compare-json (json "kouta-koulutus-notification")
                    (get @notifier-target-mock/received koulutus-oid))))))

(deftest index-all-toteutukset-test
  (with-mocked-notifications
   (fixture/with-mocked-indexing
    (testing "Indexer should index all toteutukset"
      (check-all-nil)
      (i/index-all-toteutukset false)
      (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
      (is (= nil (get-doc hakukohde/index-name hakukohde-oid)))
      (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
      (is (= koulutus-oid (:oid (get-doc koulutus/index-name koulutus-oid))))
      (is (= koulutus-oid (:oid (get-doc search/index-name koulutus-oid))))
      (is (= nil (get-doc valintaperuste/index-name valintaperuste-id))))
    (testing "Notifications should be sent about all toteutukset if enabled"
      (i/index-all-toteutukset true)
      (compare-json (json "kouta-toteutus-notification")
                    (get @notifier-target-mock/received toteutus-oid))))))

(deftest index-all-hakukohteet-test
  (with-mocked-notifications
   (fixture/with-mocked-indexing
    (testing "Indexer should index all hakukohteet"
      (check-all-nil)
      (i/index-all-hakukohteet false)
      (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
      (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
      (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
      (is (= nil (get-doc koulutus/index-name koulutus-oid)))
      (is (= koulutus-oid (:oid (get-doc search/index-name koulutus-oid))))
      (is (= nil (get-doc valintaperuste/index-name valintaperuste-id)))
      (is (empty? @notifier-target-mock/received)))
    (testing "Notifications should be sent about all hakukohteet if enabled"
      (i/index-all-hakukohteet true)
      (compare-json (json "kouta-hakukohde-notification")
                    (get @notifier-target-mock/received hakukohde-oid))))))

(deftest index-all-haut-test
  (with-mocked-notifications
   (fixture/with-mocked-indexing
    (testing "Indexer should index all haut"
      (check-all-nil)
      (i/index-all-haut false)
      (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
      (is (= nil (get-doc hakukohde/index-name hakukohde-oid)))
      (is (= nil (get-doc toteutus/index-name toteutus-oid)))
      (is (= nil (get-doc koulutus/index-name koulutus-oid)))
      (is (= koulutus-oid (:oid (get-doc search/index-name koulutus-oid))))
      (is (= nil (get-doc valintaperuste/index-name valintaperuste-id)))
      (is (empty? @notifier-target-mock/received)))
    (testing "Notifications should be sent about all haut if enabled"
      (i/index-all-haut true)
      (compare-json (json "kouta-haku-notification")
                    (get @notifier-target-mock/received haku-oid))))))

(deftest index-all-valintaperusteet-test
  (with-mocked-notifications
   (fixture/with-mocked-indexing
    (testing "Indexer should index all valintaperusteet"
      (check-all-nil)
      (i/index-all-valintaperusteet false)
      (is (= nil (get-doc haku/index-name haku-oid)))
      (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
      (is (= nil (get-doc toteutus/index-name toteutus-oid)))
      (is (= nil (get-doc koulutus/index-name koulutus-oid)))
      (is (= nil (get-doc search/index-name koulutus-oid)))
      (is (= valintaperuste-id (:id (get-doc valintaperuste/index-name valintaperuste-id))))
      (is (empty? @notifier-target-mock/received)))
    (testing "Notifications should be sent about all valintaperusteet if enabled"
      (i/index-all-valintaperusteet true)
      (compare-json (json "kouta-valintaperuste-notification")
                    (get @notifier-target-mock/received valintaperuste-id))))))
