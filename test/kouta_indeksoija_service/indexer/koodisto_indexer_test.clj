(ns kouta-indeksoija-service.indexer.koodisto-indexer-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.test-tools :refer :all]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer [json]]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [kouta-indeksoija-service.indexer.koodisto.koodisto :as koodisto]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [kouta-indeksoija-service.elastic.admin :as admin]))

(use-fixtures :each (fn [test] (admin/initialize-koodisto-indices-for-reindexing) (test)))

(deftest koodisto-index-test
  (testing "do index koodisto"
    (with-redefs [kouta-indeksoija-service.rest.koodisto/get-koodit #(json "test/resources/koodisto/" %)]
      (i/index-koodistot ["maakunta"])
      (let [result (koodisto/get-from-index "maakunta")]
        (is (= "maakunta" (:koodisto result)))
        (is (= 20 (count (distinct (remove nil? (map :koodiUri (:koodit result)))))))
        (doseq [nimi (map :nimi (:koodit result))]
          (is (contains? nimi :fi))
          (is (contains? nimi :sv)))))))