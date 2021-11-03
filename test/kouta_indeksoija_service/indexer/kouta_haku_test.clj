(ns kouta-indeksoija-service.indexer.kouta-haku-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer :all]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.test-tools :refer [parse compare-json debug-pretty]]
            [kouta-indeksoija-service.indexer.kouta.haku :as haku]
            [cheshire.core :refer [generate-string]])
  (:import (fi.oph.kouta.external KoutaFixtureTool$)))

(defonce KoutaFixtureTool KoutaFixtureTool$/MODULE$)

(use-fixtures :each fixture/indices-fixture)
(use-fixtures :each common-indexer-fixture)

(deftest index-hakukohteet-without-hakukohdekoodiuri-to-haku-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index hakukohteet with original name to haku-index, if hakukohdeKoodiUri not available"
     (check-all-nil)
     (i/index-haut [haku-oid])
     (let [ hakukohteet (to-array (:hakukohteet (get-doc haku/index-name haku-oid))) ]
       (is (= 2 (alength hakukohteet)))
       (is (= {:fi "Koulutuksen 0 toteutuksen 0 hakukohteen 0 esitysnimi fi",
               :sv "Koulutuksen 0 toteutuksen 0 hakukohteen 0 esitysnimi sv"} (:nimi (aget hakukohteet 0))))
       (is (= {:fi "Koulutuksen 0 toteutuksen 2 hakukohteen 2 esitysnimi fi",
               :sv "Koulutuksen 0 toteutuksen 2 hakukohteen 2 esitysnimi sv"} (:nimi (aget hakukohteet 1))))
       ))))

(deftest index-hakukohteet-with-hakukohdekoodiuri-to-haku-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index hakukohteet with hakukohdeKoodiUri to haku-index, if hakukohdeKoodiUri available"
     (check-all-nil)
     (fixture/update-hakukohde-mock hakukohde-oid :hakukohdeKoodiUri "hakukohteetperusopetuksenjalkeinenyhteishaku_101#1" :nimi "")
     (fixture/update-hakukohde-mock hakukohde-oid2 :hakukohdeKoodiUri "hakukohteetperusopetuksenjalkeinenyhteishaku_101#2" :nimi "")
     (i/index-haut [haku-oid])
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
     (i/index-haut [ei-julkaistu-haku-oid])
     (let [ hakukohteet (to-array (:hakukohteet (get-doc haku/index-name ei-julkaistu-haku-oid))) ]
       (is (= 1 (alength hakukohteet)))
       (is (= "tallennettu" (:tila (aget hakukohteet 0))))))))
