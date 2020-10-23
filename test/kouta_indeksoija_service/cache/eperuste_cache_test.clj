(ns kouta-indeksoija-service.cache.eperuste-cache-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.indexer.cache.eperuste :as cache]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer [json]]
            [kouta-indeksoija-service.test-tools :refer [debug-pretty]]))

(def eperuste-id 6942140)

(def expected (json "test/resources/eperusteet/" (str eperuste-id "_cached")))

(defn get-by-id
  [id]
  (json "test/resources/eperusteet/" (str id)))

(deftest eperuste-cache-test
  (with-redefs [kouta-indeksoija-service.rest.eperuste/get-doc get-by-id
                kouta-indeksoija-service.rest.eperuste/get-by-koulutuskoodi #(when (= % "koulutus_361201") (get-by-id eperuste-id))
                kouta-indeksoija-service.rest.koodisto/get-koodi-nimi-with-cache kouta-indeksoija-service.fixture.external-services/mock-koodisto]
    (testing "Eperuste cache should return correct data..."
      (testing "when requested by id"
        (let [d (cache/get-eperuste-by-id eperuste-id)]
          (is (= d expected))))
      (testing "when requested by koulutus_koodi"
        (let [d (cache/get-eperuste-by-koulutuskoodi "koulutus_361201#1")]
          (is (= d expected)))))))