(ns kouta-indeksoija-service.indexer.kouta-haku-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer :all]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.test-tools :refer [parse compare-json debug-pretty]]
            [kouta-indeksoija-service.indexer.kouta.haku :as haku]
            [kouta-indeksoija-service.indexer.kouta.hakukohde :as hakukohde]
            [kouta-indeksoija-service.indexer.kouta.toteutus :as toteutus]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus-search]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos-search]
            [cheshire.core :refer [generate-string]]))

(use-fixtures :each common-indexer-fixture)

(deftest index-hakukohteet-without-hakukohdekoodiuri-to-haku-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index hakukohteet with original name to haku-index, if hakukohdeKoodiUri not available"
     (check-all-nil)
     (i/index-haut [haku-oid] (. System (currentTimeMillis)))
     (let [ hakukohteet (to-array (:hakukohteet (get-doc haku/index-name haku-oid))) ]
       (is (= 2 (alength hakukohteet)))
       (is (= {:fi "Koulutuksen 0 toteutuksen 0 hakukohde 0 fi",
               :sv "Koulutuksen 0 toteutuksen 0 hakukohde 0 sv"} (:nimi (aget hakukohteet 0))))
       (is (= {:fi "Koulutuksen 0 toteutuksen 2 hakukohde 0 fi",
               :sv "Koulutuksen 0 toteutuksen 2 hakukohde 0 sv"} (:nimi (aget hakukohteet 1))))
       ))))

(deftest index-hakukohteet-with-hakukohdekoodiuri-to-haku-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index hakukohteet with hakukohdeKoodiUri to haku-index, if hakukohdeKoodiUri available"
     (check-all-nil)
     (fixture/update-hakukohde-mock hakukohde-oid :hakukohdeKoodiUri "hakukohteetperusopetuksenjalkeinenyhteishaku_101#1" :nimi {})
     (fixture/update-hakukohde-mock hakukohde-oid2 :hakukohdeKoodiUri "hakukohteetperusopetuksenjalkeinenyhteishaku_101#2" :nimi {})
     (i/index-haut [haku-oid] (. System (currentTimeMillis)))
     (let [ hakukohteet (to-array (:hakukohteet (get-doc haku/index-name haku-oid))) ]
       (is (= 2 (alength hakukohteet)))
       (is (= {:fi "hakukohteetperusopetuksenjalkeinenyhteishaku_101#1 nimi fi",
               :sv "hakukohteetperusopetuksenjalkeinenyhteishaku_101#1 nimi sv"} (:nimi (aget hakukohteet 0))))
       (is (= {:fi "hakukohteetperusopetuksenjalkeinenyhteishaku_101#2 nimi fi",
               :sv "hakukohteetperusopetuksenjalkeinenyhteishaku_101#2 nimi sv"} (:nimi (aget hakukohteet 1))))
       ))))

(deftest index-julkaistut-hakukohteet-of-ei-julkaistu-haku-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index hakukohde in same state as related haku, in case hakukohde was published and haku not"
     (check-all-nil)
     (i/index-haut [ei-julkaistu-haku-oid] (. System (currentTimeMillis)))
     (let [ hakukohteet (to-array (:hakukohteet (get-doc haku/index-name ei-julkaistu-haku-oid))) ]
       (is (= 1 (alength hakukohteet)))
       (is (= "tallennettu" (:tila (aget hakukohteet 0))))))))

(deftest delete-non-existing-haku
  (fixture/with-mocked-indexing
   (testing "Indexer should delete non-existing haku from haku-index"
     (check-all-nil)
     (fixture/update-hakukohde-mock ei-julkaistun-haun-julkaistu-hakukohde-oid :tila "tallennettu")
     (i/index-haut [haku-oid ei-julkaistu-haku-oid] (. System (currentTimeMillis)))
     (is (= "julkaistu" (:tila (get-doc haku/index-name haku-oid))))
     (is (= "tallennettu" (:tila (get-doc haku/index-name ei-julkaistu-haku-oid))))
     (fixture/update-hakukohde-mock ei-julkaistun-haun-julkaistu-hakukohde-oid :tila "poistettu")
     (fixture/update-haku-mock ei-julkaistu-haku-oid :tila "poistettu")
     (i/index-haut [haku-oid ei-julkaistu-haku-oid] (. System (currentTimeMillis)))
     (is (= "julkaistu" (:tila (get-doc haku/index-name haku-oid))))
     (is (nil? (get-doc haku/index-name ei-julkaistu-haku-oid))))))

(deftest delete-non-existing-haku-from-search-index
  (fixture/with-mocked-indexing
   (testing "Indexer should delete non-existing haku from search index"
     (check-all-nil)
     (fixture/update-hakukohde-mock ei-julkaistun-haun-julkaistu-hakukohde-oid :tila "tallennettu")
     (fixture/update-hakukohde-mock hakukohde-oid2 :tila "arkistoitu")
     (fixture/update-toteutus-mock toteutus-oid2 :tila "arkistoitu")
     (i/index-haut [ei-julkaistu-haku-oid] (. System (currentTimeMillis)))
     (is (= "tallennettu" (:tila (get-doc haku/index-name ei-julkaistu-haku-oid))))
     (is (= "tallennettu" (:tila (get-doc hakukohde/index-name ei-julkaistun-haun-julkaistu-hakukohde-oid))))
     (is (= toteutus-oid3 (:oid (get-doc toteutus/index-name toteutus-oid3))))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (= false (hit-key-not-empty oppilaitos-search/index-name mocks/Oppilaitos1 :hakutiedot)))
     (fixture/update-hakukohde-mock ei-julkaistun-haun-julkaistu-hakukohde-oid :tila "poistettu")
     (fixture/update-haku-mock ei-julkaistu-haku-oid :tila "poistettu")
     (fixture/update-toteutus-mock toteutus-oid3 :tila "poistettu")
     (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu")
     (i/index-haut [ei-julkaistu-haku-oid] (. System (currentTimeMillis)))
     (is (nil? (get-doc haku/index-name ei-julkaistu-haku-oid)))
     (is (nil? (get-doc hakukohde/index-name ei-julkaistun-haun-julkaistu-hakukohde-oid)))
     (is (nil? (get-doc toteutus/index-name toteutus-oid3)))
     (is (nil? (get-doc koulutus-search/index-name koulutus-oid)))
     (is (= false (hit-key-not-empty oppilaitos-search/index-name mocks/Oppilaitos1 :hakutiedot))))))
