(ns kouta-indeksoija-service.indexer.kouta-search-index-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer [no-timestamp json read-json-as-string]]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.test-tools :refer [debug-pretty]]
            [kouta-indeksoija-service.elastic.admin :as admin]
            [kouta-indeksoija-service.test-tools :refer [parse compare-json]]
            [clj-test-utils.elasticsearch-mock-utils :refer :all])
  (:import (fi.oph.kouta.external KoutaFixtureTool$)))

(defonce KoutaFixtureTool KoutaFixtureTool$/MODULE$)

(defonce json-path "test/resources/search/")

(let [oppilaitos-oid1 "1.2.246.562.10.10101010199"
      oppilaitos-oid2 "1.2.246.562.10.77777777799"
      koulutus-oid1   "1.2.246.562.13.00000000000000000099"
      koulutus-oid2   "1.2.246.562.13.00000000000000000098"
      koulutus-oid3   "1.2.246.562.13.00000000000000000097"
      koulutus-oid4   "1.2.246.562.13.00000000000000000096"
      toteutus-oid1   "1.2.246.562.17.00000000000000000099"
      toteutus-oid2   "1.2.246.562.17.00000000000000000098"
      toteutus-oid3   "1.2.246.562.17.00000000000000000097"
      toteutus-oid4   "1.2.246.562.17.00000000000000000096"]

  (defn- test-data-fixture
    [tests]
    (admin/initialize-indices)
    (fixture/add-oppilaitos-mock oppilaitos-oid1 :tila "julkaistu")
    (fixture/add-oppilaitos-mock oppilaitos-oid2 :tila "julkaistu")

    (fixture/add-koulutus-mock koulutus-oid1
                               :tila "julkaistu"
                               :nimi "Autoalan perustutkinto 0"
                               :koulutusKoodiUri "koulutus_351301#1"
                               :tarjoajat (str oppilaitos-oid2 "2")
                               :metadata (read-json-as-string "test/resources/search/" "koulutus-metadata"))

    (fixture/add-koulutus-mock koulutus-oid2
                               :tila "julkaistu"
                               :nimi "Hevosalan perustutkinto 0"
                               :koulutusKoodiUri "koulutus_361104#1"
                               :tarjoajat oppilaitos-oid2
                               :metadata (read-json-as-string "test/resources/search/" "koulutus-metadata"))

    (fixture/add-koulutus-mock koulutus-oid3
                               :tila "julkaistu"
                               :nimi "Hevosalan osaamisala"
                               :koulutustyyppi "amm-osaamisala"
                               :johtaaTutkintoon "false"
                               :tarjoajat oppilaitos-oid2
                               :metadata (.ammOsaamisalaKoulutusMetadata KoutaFixtureTool))

    (fixture/add-koulutus-mock koulutus-oid4
                               :tila "julkaistu"
                               :nimi "Hevosalan tutkinnon osat"
                               :koulutustyyppi "amm-tutkinnon-osa"
                               :johtaaTutkintoon "false"
                               :ePerusteId nil
                               :koulutusKoodiUri nil
                               :tarjoajat oppilaitos-oid2
                               :metadata (.ammTutkinnonOsaKoulutusMetadata KoutaFixtureTool))

    (fixture/add-toteutus-mock toteutus-oid1
                               koulutus-oid2
                               :tila "julkaistu"
                               :nimi "Hevostoteutus 1"
                               :tarjoajat (str oppilaitos-oid2 "1" "," oppilaitos-oid2 "3")
                               :metadata (read-json-as-string "test/resources/search/" "toteutus-metadata"))

    (fixture/add-toteutus-mock toteutus-oid2
                               koulutus-oid2
                               :tila "julkaistu"
                               :nimi "Hevostoteutus 2"
                               :tarjoajat (str oppilaitos-oid2 "1"))

    (tests)
    (fixture/teardown))

  (use-fixtures :each fixture/indices-fixture)
  (use-fixtures :each test-data-fixture)

  (defn mock-get-eperuste-by-koulutuskoodi
    [x]
    (kouta-indeksoija-service.fixture.external-services/mock-get-eperuste 6942140))

  (deftest index-oppilaitos-search-items-test-1
    (fixture/with-mocked-indexing
      (with-redefs [kouta-indeksoija-service.rest.eperuste/get-by-koulutuskoodi mock-get-eperuste-by-koulutuskoodi]
         (testing "Create correct search item when oppilaitos has no koulutukset"
        (is (nil? (get-doc oppilaitos/index-name oppilaitos-oid1)))
        (i/index-oppilaitos oppilaitos-oid1)
        (compare-json (no-timestamp (json json-path "oppilaitos-search-item-no-koulutukset"))
                      (no-timestamp (get-doc oppilaitos/index-name oppilaitos-oid1)))))))

  (deftest index-oppilaitos-search-items-test-2
    (fixture/with-mocked-indexing
     (with-redefs [kouta-indeksoija-service.rest.eperuste/get-by-koulutuskoodi mock-get-eperuste-by-koulutuskoodi]
       (testing "Create correct search item when oppilaitos has koulutukset and toteutukset"
         (is (nil? (get-doc oppilaitos/index-name oppilaitos-oid2)))
         (i/index-oppilaitos oppilaitos-oid2)
         (compare-json (no-timestamp (json json-path "oppilaitos-search-item-koulutus-and-toteutukset"))
                       (no-timestamp (get-doc oppilaitos/index-name oppilaitos-oid2)))))))

  (defn organisaatio-hierarkia-mock-for-toimipiste2
    [x & {:as params}]
    (kouta-indeksoija-service.fixture.external-services/mock-organisaatio-hierarkia oppilaitos-oid2))

  (deftest index-koulutus-search-items-test-1
    (fixture/with-mocked-indexing
      (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 organisaatio-hierarkia-mock-for-toimipiste2]
        (testing "Create correct search item when koulutus has no toteutukset"
          (is (nil? (get-doc koulutus/index-name koulutus-oid1)))
          (i/index-koulutus koulutus-oid1)
          (i/index-oppilaitos oppilaitos-oid2)
          (compare-json (no-timestamp (json json-path "koulutus-search-item-no-toteutukset"))
                        (no-timestamp (get-doc koulutus/index-name koulutus-oid1)))))))

  (deftest index-koulutus-search-items-test-2
    (fixture/with-mocked-indexing
     (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 organisaatio-hierarkia-mock-for-toimipiste2]
       (testing "Create correct search item when koulutus has toteutukset"
         (is (nil? (get-doc koulutus/index-name koulutus-oid2)))
         (i/index-koulutus koulutus-oid2)
         (i/index-oppilaitos oppilaitos-oid2)
         (compare-json (no-timestamp (json json-path "koulutus-search-item-toteutukset"))
                       (no-timestamp (get-doc koulutus/index-name koulutus-oid2)))))))

  (deftest index-koulutus-search-items-test-3
    (fixture/with-mocked-indexing
     (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 organisaatio-hierarkia-mock-for-toimipiste2]
       (testing "Create correct search item when amm-osaamisala"
         (is (nil? (get-doc koulutus/index-name koulutus-oid3)))
         (i/index-koulutus koulutus-oid3)
         (i/index-oppilaitos oppilaitos-oid2)
         ;(debug-pretty (get-doc koulutus/index-name koulutus-oid3))
         (compare-json (no-timestamp (json json-path "koulutus-search-item-osaamisala"))
                       (no-timestamp (get-doc koulutus/index-name koulutus-oid3)))))))

  (deftest index-koulutus-search-items-test-4
    (fixture/with-mocked-indexing
     (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 organisaatio-hierarkia-mock-for-toimipiste2
                   kouta-indeksoija-service.rest.eperuste/get-by-koulutuskoodi mock-get-eperuste-by-koulutuskoodi]
       (testing "Create correct search item when amm-tutkinnon-osa"
         (is (nil? (get-doc koulutus/index-name koulutus-oid4)))
         (i/index-koulutus koulutus-oid4)
         (i/index-oppilaitos oppilaitos-oid2)
         ;(debug-pretty (get-doc koulutus/index-name koulutus-oid4))
         (compare-json (no-timestamp (json json-path "koulutus-search-item-tutkinnon-osa"))
                       (no-timestamp (get-doc koulutus/index-name koulutus-oid4))))))))


