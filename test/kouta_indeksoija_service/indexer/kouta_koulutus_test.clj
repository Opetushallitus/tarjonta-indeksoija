(ns kouta-indeksoija-service.indexer.kouta-koulutus-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus-search]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos-search]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.test-tools :refer [compare-json]]
            [kouta-indeksoija-service.indexer.kouta.koulutus :as koulutus]
            [kouta-indeksoija-service.indexer.eperuste.eperuste :as eperuste]))

(use-fixtures :each fixture/indices-fixture)
(use-fixtures :each common-indexer-fixture)

(deftest index-tallennettu-koulutus-test
  (fixture/with-mocked-indexing
    (testing "Indexer should index tallennettu koulutus only to koulutus index"
      (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu")
      (check-all-nil)
      (i/index-koulutukset [koulutus-oid])
      (is (nil? (get-doc koulutus-search/index-name koulutus-oid)))
      (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
      (compare-json (no-timestamp (merge (json "kouta-koulutus-result") {:tila "tallennettu"}))
                    (no-timestamp (get-doc koulutus/index-name koulutus-oid)))
      (fixture/update-koulutus-mock koulutus-oid :tila "julkaistu"))))

(deftest index-julkaistu-koulutus-test
  (fixture/with-mocked-indexing
    (testing "Indexer should index julkaistu koulutus also to search index"
      (check-all-nil)
      (i/index-koulutukset [koulutus-oid])
      (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
      (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
      (compare-json (no-timestamp (merge (json "kouta-koulutus-result") {:tila "julkaistu"}))
                    (no-timestamp (get-doc koulutus/index-name koulutus-oid))))))

(deftest index-eperuste-with-koulutus-test
  (fixture/with-mocked-indexing
    (testing "Indexer should index eperuste with koulutus"
      (let [eperuste-id 12345]
        (fixture/update-koulutus-mock koulutus-oid :ePerusteId (str eperuste-id))
        (check-all-nil)
        (is (nil? (eperuste/get eperuste-id)))
        (i/index-koulutukset [koulutus-oid])
        (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
        (is (= eperuste-id (:id (eperuste/get eperuste-id))))
        (fixture/update-koulutus-mock koulutus-oid :ePerusteId nil)))))

(deftest index-arkistoitu-koulutus-test
  (fixture/with-mocked-indexing
    (testing "Indexer should delete koulutus from search indexes when it's arkistoitu"
      (check-all-nil)
      (i/index-koulutukset [koulutus-oid])
      (is (= "julkaistu" (:tila (get-doc koulutus/index-name koulutus-oid))))
      (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
      (is (< 0 (count-hits-by-key oppilaitos-search/index-name mocks/Oppilaitos1 :koulutusOid koulutus-oid)))
      (fixture/update-koulutus-mock koulutus-oid :tila "arkistoitu")
      (i/index-koulutukset [koulutus-oid])
      (is (= "arkistoitu" (:tila (get-doc koulutus/index-name koulutus-oid))))
      (is (nil? (get-doc koulutus-search/index-name koulutus-oid)))
      (is (= 0 (count-hits-by-key oppilaitos-search/index-name mocks/Oppilaitos1 :koulutusOid koulutus-oid)))
      (fixture/update-koulutus-mock koulutus-oid :tila "julkaistu"))))

(def tutkinnon-osa-koulutusala1
  {:koodiUri "kansallinenkoulutusluokitus2016koulutusalataso1_07"
   :nimi {:fi "Tekniikan alat"
          :en "Engineering, manufacturing and construction"
          :sv "De tekniska områdena"}})

(def tutkinnon-osa-koulutusala2
  {:koodiUri "kansallinenkoulutusluokitus2016koulutusalataso1_08"
   :nimi {:fi "Palvelualat"}})

(def osaamisala-koulutusala
  {:koodiUri "kansallinenkoulutusluokitus2016koulutusalataso1_09"
   :nimi {:fi "Maa- ja metsätalousalat"}})

(defn- mock-koulutusalat-taso1 [koulutusKoodiUri]
  (cond
    (= koulutusKoodiUri "koulutus_123123#1") [tutkinnon-osa-koulutusala1]
    (= koulutusKoodiUri "koulutus_123125#1") [tutkinnon-osa-koulutusala1]
    (= koulutusKoodiUri "koulutus_123444#1") [tutkinnon-osa-koulutusala2]
    (= koulutusKoodiUri "koulutus_222333#1") [osaamisala-koulutusala]))

(deftest index-amm-tutkinnon-osa-koulutus
  (fixture/with-mocked-indexing
    (testing "Indexer should index distinct koulutusalat for every tutkinnon osa"
      (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/koulutusalat-taso1 mock-koulutusalat-taso1]
        (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu" :koulutustyyppi "amm-tutkinnon-osa" :metadata fixture/amk-tutkinnon-osa-koulutus-metadata)
        (check-all-nil)
        (i/index-koulutukset [koulutus-oid])
        (let [koulutus (get-doc koulutus/index-name koulutus-oid)
              koulutusalat (get-in koulutus [:metadata :koulutusala])]
          (is (= (count koulutusalat) 2))
          (is (-> koulutusalat first :nimi :fi) "Tekniikan alat")
          (is (-> koulutusalat last :nimi :fi) "Palvelualat"))))))

(deftest tutkinnon-osa-enrichment
  (fixture/with-mocked-indexing
    (testing "Indexer should enrich tutkinnon osat from eperusteet"
      (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/koulutusalat-taso1 mock-koulutusalat-taso1]
        (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu" :koulutustyyppi "amm-tutkinnon-osa" :metadata fixture/amk-tutkinnon-osa-koulutus-metadata)
        (check-all-nil)
        (i/index-koulutukset [koulutus-oid])
        (let [koulutus (get-doc koulutus/index-name koulutus-oid)
              tutkinnon-osat (get-in koulutus [:metadata :tutkinnonOsat])
              tutkinnon-osa (->> tutkinnon-osat
                                 (filter #(= (:tutkinnonosaId %) 1234))
                                 (first))]
          (is (= (:opintojenLaajuusNumero tutkinnon-osa) 50))
          (is (= (get-in tutkinnon-osa [:opintojenLaajuus :nimi :fi]) "opintojenlaajuus_50 nimi fi"))
          (is (= (get-in tutkinnon-osa [:opintojenLaajuusyksikko :nimi :fi]) "opintojenlaajuusyksikko_6 nimi fi")))))))


(deftest index-osaamisala-koulutus
  (fixture/with-mocked-indexing
    (testing "Should index koulutusala for osaamisala"
      (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/koulutusalat-taso1 mock-koulutusalat-taso1]
        (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu" :koulutustyyppi "amm-osaamisala" :koulutusKoodiUri "koulutus_222333#1" :metadata fixture/amk-osaamisala-koulutus-metadata)
        (check-all-nil)
        (i/index-koulutukset [koulutus-oid])
        (let [koulutus (get-doc koulutus/index-name koulutus-oid)
              koulutusalat (get-in koulutus [:metadata :koulutusala])]
          (is (= (count koulutusalat) 1))
          (is (-> koulutusalat first :nimi :fi) "Maa- ja metsätalousalat"))))))

(deftest korkeakoulutus-opintojenlaajuusyksikko
  (fixture/with-mocked-indexing
    (testing "Indexer should set fixed opintojenLaajuusyksikko value for amk"
      (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu" :koulutustyyppi "amk" :metadata fixture/amk-koulutus-metadata)
      (check-all-nil)
      (i/index-koulutukset [koulutus-oid])
      (let [koulutus (get-doc koulutus/index-name koulutus-oid)]
        (is (= (get-in koulutus [:metadata :opintojenLaajuusyksikko :koodiUri]) "opintojenlaajuusyksikko_2#1"))))))
