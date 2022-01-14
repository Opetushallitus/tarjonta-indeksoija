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
      (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
      (is (nil? (get-doc koulutus-search/index-name koulutus-oid)))
      (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
      (compare-json (no-timestamp (merge (json "kouta-koulutus-result") {:tila "tallennettu"}))
                    (no-timestamp (get-doc koulutus/index-name koulutus-oid)))
      (fixture/update-koulutus-mock koulutus-oid :tila "julkaistu"))))

(deftest index-julkaistu-koulutus-test
  (fixture/with-mocked-indexing
    (testing "Indexer should index julkaistu koulutus also to search index"
      (check-all-nil)
      (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
      (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
      (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
      (compare-json (no-timestamp (merge (json "kouta-koulutus-result") {:tila "julkaistu"}))
                    (no-timestamp (get-doc koulutus/index-name koulutus-oid))))))

(deftest index-eperuste-with-koulutus-test
  (fixture/with-mocked-indexing
    (testing "Indexer should index eperuste with koulutus"
      (let [eperuste-id 12345]
        (fixture/update-koulutus-mock koulutus-oid :ePerusteId eperuste-id)
        (check-all-nil)
        (is (nil? (eperuste/get-from-index eperuste-id)))
        (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
        (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
        (is (= eperuste-id (:id (eperuste/get-from-index eperuste-id))))
        (fixture/update-koulutus-mock koulutus-oid :ePerusteId nil)))))

(deftest index-arkistoitu-koulutus-test
  (fixture/with-mocked-indexing
    (testing "Indexer should delete koulutus from search indexes when it's arkistoitu"
      (check-all-nil)
      (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
      (is (= "julkaistu" (:tila (get-doc koulutus/index-name koulutus-oid))))
      (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
      (is (< 0 (count-hits-by-key oppilaitos-search/index-name mocks/Oppilaitos1 :koulutusOid koulutus-oid)))
      (fixture/update-koulutus-mock koulutus-oid :tila "arkistoitu")
      (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
      (is (= "arkistoitu" (:tila (get-doc koulutus/index-name koulutus-oid))))
      (is (nil? (get-doc koulutus-search/index-name koulutus-oid)))
      (is (= 0 (count-hits-by-key oppilaitos-search/index-name mocks/Oppilaitos1 :koulutusOid koulutus-oid)))
      (fixture/update-koulutus-mock koulutus-oid :tila "julkaistu"))))

(deftest delete-non-existing-koulutus
  (fixture/with-mocked-indexing
   (testing "Indexer should delete non-existing koulutus from index"
     (check-all-nil)
     (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
     (is (= "julkaistu" (:tila (get-doc koulutus/index-name koulutus-oid))))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (< 0 (count-hits-by-key oppilaitos-search/index-name mocks/Oppilaitos1 :koulutusOid koulutus-oid)))
     (fixture/update-koulutus-mock koulutus-oid :tila "poistettu")
     (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
     (is (nil? (get-doc koulutus/index-name koulutus-oid)))
     (is (nil? (get-doc koulutus-search/index-name koulutus-oid)))
     (is (= 0 (count-hits-by-key oppilaitos-search/index-name mocks/Oppilaitos1 :koulutusOid koulutus-oid))))))

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
        (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu" :koulutustyyppi "amm-tutkinnon-osa" :metadata fixture/amm-tutkinnon-osa-koulutus-metadata)
        (check-all-nil)
        (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
        (let [koulutus (get-doc koulutus/index-name koulutus-oid)
              koulutusalat (get-in koulutus [:metadata :koulutusala])]
          (is (= 2 (count koulutusalat)))
          (is (-> koulutusalat first :nimi :fi) "Tekniikan alat")
          (is (-> koulutusalat last :nimi :fi) "Palvelualat"))))))

(deftest eperuste-data-enrichment
  (fixture/with-mocked-indexing
   (testing "Indexer should enrich eperuste data to metadata"
     (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu" :koulutustyyppi "amk" :metadata fixture/amk-koulutus-metadata)
     (check-all-nil)
     (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
     (let [koulutus (get-doc koulutus/index-name koulutus-oid)]
       (is (= (get-in koulutus [:metadata :eperuste :id]) 1234))
       (is (= (get-in koulutus [:metadata :eperuste :diaarinumero]) "1111-OPH-2021"))
       (is (= (get-in koulutus [:metadata :eperuste :voimassaoloLoppuu]) "2018-01-01T00:00:00"))))))

(deftest eperuste-data-enrichment-with-no-loppuu-date
  (fixture/with-mocked-indexing
   (testing "Indexer should enrich eperuste data to metadata"
     (with-redefs [kouta-indeksoija-service.indexer.cache.eperuste/get-eperuste-by-id (fn [id] {:id id :diaarinumero "1234-OPH" :voimassaoloLoppuu nil})]
       (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu" :koulutustyyppi "amk" :metadata fixture/amk-koulutus-metadata)
       (check-all-nil)
       (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
       (let [koulutus (get-doc koulutus/index-name koulutus-oid)]
         (is (= (get-in koulutus [:metadata :eperuste :id]) 1234))
         (is (= (get-in koulutus [:metadata :eperuste :diaarinumero]) "1234-OPH"))
         (is (= (get-in koulutus [:metadata :eperuste :voimassaoloLoppuu]) nil)))))))

(deftest tutkinnon-osa-enrichment
  (fixture/with-mocked-indexing
    (testing "Indexer should enrich tutkinnon osat from eperusteet"
      (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/koulutusalat-taso1 mock-koulutusalat-taso1]
        (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu" :koulutustyyppi "amm-tutkinnon-osa" :metadata fixture/amm-tutkinnon-osa-koulutus-metadata)
        (check-all-nil)
        (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
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
        (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu" :koulutustyyppi "amm-osaamisala" :koulutuksetKoodiUri "koulutus_222333#1" :metadata fixture/amm-osaamisala-koulutus-metadata)
        (check-all-nil)
        (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
        (let [koulutus (get-doc koulutus/index-name koulutus-oid)
              koulutusalat (get-in koulutus [:metadata :koulutusala])]
          (is (= (count koulutusalat) 1))
          (is (-> koulutusalat first :nimi :fi) "Maa- ja metsätalousalat"))))))

(deftest index-lukio-koulutus
  (fixture/with-mocked-indexing
   (testing "Indexer should index lukio specific metadata"
     (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/koulutusalat-taso1 mock-koulutusalat-taso1]
       (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu" :koulutustyyppi "lk" :metadata fixture/lukio-koulutus-metadata)
       (check-all-nil)
       (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
       (let [koulutus (get-doc koulutus/index-name koulutus-oid)
             tutkintonimike (-> koulutus
                                (get-in [:metadata :tutkintonimike])
                                (first)
                                (get-in [:nimi :fi]))
             opintojen-laajuus (get-in koulutus [:metadata :opintojenLaajuus :nimi :fi])
             opintojen-laajuusyksikko (get-in koulutus [:metadata :opintojenLaajuusyksikko :nimi :fi])
             koulutusala (-> koulutus
                              (get-in [:metadata :koulutusala])
                              (first)
                              (get-in [:nimi :fi]))]
         (is (= tutkintonimike "tutkintonimikkeet_00001#1 nimi fi"))
         (is (= opintojen-laajuus "opintojenlaajuus_40#1 nimi fi"))
         (is (= opintojen-laajuusyksikko "opintojenlaajuusyksikko_2#1 nimi fi"))
         (is (= koulutusala "kansallinenkoulutusluokitus2016koulutusalataso1_001#1 nimi fi")))))))

(deftest index-amk-koulutus
  (fixture/with-mocked-indexing
    (testing "Indexer should index korkeakoulu specific metadata"
      (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu" :koulutustyyppi "amk" :metadata fixture/amk-koulutus-metadata)
      (check-all-nil)
      (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
      (let [koulutus (get-doc koulutus/index-name koulutus-oid)
            tutkintonimikkeet (get-in koulutus [:metadata :tutkintonimike])]
        (is (= "opintojenlaajuusyksikko_2#1" (get-in koulutus [:metadata :opintojenLaajuusyksikko :koodiUri])))
        (is (= 2 (count tutkintonimikkeet)))
        (is (-> tutkintonimikkeet first :nimi :fi) "tutkintonimikekk_033#1 nimi fi")
        (is (-> tutkintonimikkeet last :nimi :fi) "tutkintonimikekk_031#1 nimi fi")))))

(deftest index-tuva-koulutus
  (fixture/with-mocked-indexing
    (testing "Indexer should index tuva specific metadata"
      (fixture/update-koulutus-mock koulutus-oid :tila "julkaistu" :johtaaTutkintoon "false" :koulutustyyppi "tuva" :metadata fixture/tuva-koulutus-metadata)
      (check-all-nil)
      (i/index-koulutukset [koulutus-oid] (. System (currentTimeMillis)))
      (let [koulutus (get-doc koulutus/index-name koulutus-oid)
            opintojen-laajuus (get-in koulutus [:metadata :opintojenLaajuus :nimi :fi])
            linkki-eperusteisiin (get-in koulutus [:metadata :linkkiEPerusteisiin :fi])
            kuvaus (get-in koulutus [:metadata :kuvaus :fi])
            opintojen-laajuusyksikko (get-in koulutus [:metadata :opintojenLaajuusyksikko :koodiUri])]
        (is (= opintojen-laajuus "opintojenlaajuus_38#1 nimi fi"))
        (is (= opintojen-laajuusyksikko "opintojenlaajuusyksikko_8#1"))
        (is (= linkki-eperusteisiin "http://testilinkki.fi"))
        (is (= kuvaus "kuvausteksti"))))

    (testing "Indexer should index 38 for opintojenLaajuusNumero in case of tuva"
      (let [koulutus (get-doc koulutus-search/index-name koulutus-oid)
            opintojenLaajuusNumero (get-in koulutus [:opintojenLaajuusNumero])]
        (is (= opintojenLaajuusNumero 38))))))
