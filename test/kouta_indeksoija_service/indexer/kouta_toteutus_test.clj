(ns kouta-indeksoija-service.indexer.kouta-toteutus-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer [common-indexer-fixture check-all-nil no-timestamp json koulutus-oid toteutus-oid hakukohde-oid count-hits-by-key]]
            [kouta-indeksoija-service.test-tools :refer [compare-json]]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.indexer.kouta.toteutus :as toteutus]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos-search]
            [kouta-indeksoija-service.indexer.kouta.koulutus :as koulutus]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus-search]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [cheshire.core :refer [generate-string]])
  (:import (fi.oph.kouta.external KoutaFixtureTool$)))

(defonce KoutaFixtureTool KoutaFixtureTool$/MODULE$)

(use-fixtures :each fixture/indices-fixture)
(use-fixtures :each common-indexer-fixture)

(deftest index-toteutus-test
   (fixture/with-mocked-indexing
    (testing "Indexer should index toteutus to toteutus index and update related indexes"
       (check-all-nil)
       (i/index-toteutukset [toteutus-oid])
       (compare-json (no-timestamp (json "kouta-toteutus-result"))
                     (no-timestamp (get-doc toteutus/index-name toteutus-oid)))
       (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
       (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
       (is (= koulutus-oid (:oid (get-doc koulutus/index-name koulutus-oid)))))))

(deftest index-lukio-toteutus-test
    (fixture/with-mocked-indexing
     (testing "Indexer should index lukio toteutus to toteutus index"
         (fixture/update-koulutus-mock koulutus-oid :koulutustyyppi "lk" :metadata fixture/lk-koulutus-metadata)
         (fixture/update-toteutus-mock toteutus-oid :tila "tallennettu" :metadata (.lukioToteutusMedatada KoutaFixtureTool))
         (fixture/update-hakukohde-mock hakukohde-oid :tila "tallennettu" :metadata (generate-string {:hakukohteenLinja {:linja nil :alinHyvaksyttyKeskiarvo 6.5 :lisatietoa {:fi "fi-str", :sv "sv-str"}}}))
         (check-all-nil)
         (i/index-toteutukset [toteutus-oid])
         (compare-json (no-timestamp (json "kouta-toteutus-lukio-result"))
                       (no-timestamp (get-doc toteutus/index-name toteutus-oid))))))

(deftest index-arkistoitu-toteutus-test
   (fixture/with-mocked-indexing
    (testing "Indexer should index delete toteutus from search indexes when it's arkistoitu"
       (check-all-nil)
       (fixture/update-toteutus-mock toteutus-oid :tila "julkaistu")
       (i/index-toteutukset [toteutus-oid])
       (is (= "julkaistu" (:tila (get-doc toteutus/index-name toteutus-oid))))
       (is (< 0 (count-hits-by-key koulutus-search/index-name koulutus-oid :toteutusOid toteutus-oid)))
       (is (< 0 (count-hits-by-key oppilaitos-search/index-name mocks/Oppilaitos1 :toteutusOid toteutus-oid)))
       (fixture/update-toteutus-mock toteutus-oid :tila "arkistoitu")
       (i/index-toteutukset [toteutus-oid])
       (is (= "arkistoitu" (:tila (get-doc toteutus/index-name toteutus-oid))))
       (is (= 0 (count-hits-by-key koulutus-search/index-name koulutus-oid :toteutusOid toteutus-oid)))
       (is (= 0 (count-hits-by-key oppilaitos-search/index-name mocks/Oppilaitos1 :toteutusOid toteutus-oid))))))