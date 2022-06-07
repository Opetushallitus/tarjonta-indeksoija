(ns kouta-indeksoija-service.indexer.kouta-valintaperuste-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.indexer.kouta.valintaperuste :as valintaperuste]))

(use-fixtures :each common-indexer-fixture)

(deftest delete-nil-valintaperuste
  (fixture/with-mocked-indexing
    (testing "Indexer should delete valintaperuste that does not exist in kouta"
      (check-all-nil)
      (i/index-valintaperusteet [valintaperuste-id] (. System (currentTimeMillis)))
      (fixture/update-valintaperuste-mock valintaperuste-id) ;;Päivitetään valintaperusteen arvoksi nil
      (i/index-valintaperusteet [valintaperuste-id] (. System (currentTimeMillis)))
      (is (nil? (get-doc valintaperuste/index-name valintaperuste-id)))
      )))
