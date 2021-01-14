(ns kouta-indeksoija-service.indexer.kouta-hakukohde-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer :all]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.test-tools :refer [parse compare-json debug-pretty]]
            [kouta-indeksoija-service.indexer.kouta.hakukohde :as hakukohde]
            [kouta-indeksoija-service.indexer.kouta.haku :as haku]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus-search]
            [kouta-indeksoija-service.indexer.kouta.toteutus :as toteutus]
            [kouta-indeksoija-service.indexer.kouta.koulutus :as koulutus]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos-search]
            [kouta-indeksoija-service.fixture.external-services :as mocks]))

(use-fixtures :each fixture/indices-fixture)
(use-fixtures :each common-indexer-fixture)

(deftest index-hakukohde-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index hakukohde to hakukohde index and update related indexes"
     (check-all-nil)
     (i/index-hakukohteet [hakukohde-oid])
     (compare-json (no-timestamp (json "kouta-hakukohde-result"))
                   (no-timestamp (get-doc hakukohde/index-name hakukohde-oid)))
     (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
     (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (nil? (get-doc koulutus/index-name koulutus-oid)))
     (is (nil? (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1)))))))

(deftest index-hakukohde-hakulomakelinkki-test
  (fixture/with-mocked-indexing
   (testing "Indexer should create hakulomakeLinkki from haku oid"
     (check-all-nil)
     (fixture/update-hakukohde-mock hakukohde-oid :hakulomaketyyppi "ataru")
     (i/index-hakukohteet [hakukohde-oid])
     (compare-json (:hakulomakeLinkki (get-doc hakukohde/index-name hakukohde-oid))
                   {:fi (str "http://localhost/hakemus/haku/" haku-oid "?lang=fi")
                    :sv (str "http://localhost/hakemus/haku/" haku-oid "?lang=sv")
                    :en (str "http://localhost/hakemus/haku/" haku-oid "?lang=en")}))))

(deftest index-hakukohde-haun-hakulomakelinkki-test
  (fixture/with-mocked-indexing
   (testing "Indexer should create hakulomakeLinkki from haku oid"
     (check-all-nil)
     (fixture/update-haku-mock haku-oid :hakulomaketyyppi "ataru")
     (fixture/update-hakukohde-mock hakukohde-oid :hakulomaketyyppi "ataru" :kaytetaanHaunHakulomaketta "true")
     (i/index-hakukohteet [hakukohde-oid])
     (compare-json (:hakulomakeLinkki (get-doc haku/index-name haku-oid))
                   {:fi (str "http://localhost/hakemus/haku/" haku-oid "?lang=fi")
                    :sv (str "http://localhost/hakemus/haku/" haku-oid "?lang=sv")
                    :en (str "http://localhost/hakemus/haku/" haku-oid "?lang=en")}))))