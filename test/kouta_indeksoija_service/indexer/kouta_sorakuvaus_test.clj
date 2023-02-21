(ns kouta-indeksoija-service.indexer.kouta-sorakuvaus-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.common-oids :refer :all]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.indexer.kouta.sorakuvaus :as sorakuvaus]))

(use-fixtures :each common-indexer-fixture)

(deftest delete-nil-sorakuvaus
  (fixture/with-mocked-indexing
    (testing "Indexer should delete sorakuvaus that does not exist in kouta"
      (check-all-nil)
      (i/index-sorakuvaukset [sorakuvaus-id] (. System (currentTimeMillis)))
      (fixture/update-sorakuvaus-mock sorakuvaus-id) ;;Päivitetään sorakuvauksen arvoksi nil
      (i/index-sorakuvaukset [sorakuvaus-id] (. System (currentTimeMillis)))
      (is (nil? (get-doc sorakuvaus/index-name sorakuvaus-id)))
      )))
