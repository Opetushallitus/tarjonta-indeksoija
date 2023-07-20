(ns kouta-indeksoija-service.indexer.search-tests.kouta-oppilaitos-search-integration-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.common-oids :refer :all]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos-search]))

(use-fixtures :each common-indexer-fixture)

(defn- mock-ylakoulutusala
  [koulutus-koodi]
  ["ylakoulutusala2"])

(deftest kouta-oppilaitos-search
  (fixture/with-mocked-indexing
   (testing "lisaa ylakoulutusalan koulutusalaan"
     (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/koulutusalan-ylakoulutusalat mock-ylakoulutusala]
       (oppilaitos-search/do-index [oppilaitos-oid] (. System (currentTimeMillis)))
       (let [oppilaitos (get-doc oppilaitos-search/index-name oppilaitos-oid)
             koulutus-alat (get-in oppilaitos [:search_terms 0 :koulutusalat])]
         (is (= 5 (count koulutus-alat)))
         (is (contains? (set koulutus-alat) "ylakoulutusala2")))))))
