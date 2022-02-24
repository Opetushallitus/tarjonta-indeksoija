(ns kouta-indeksoija-service.indexer.tools.search-test
  (:require [clojure.test :refer [deftest testing is]]
            [kouta-indeksoija-service.indexer.tools.search :as search]))

(deftest get-haun-julkaistut-hakukohteet-tests
  (testing "filters hakukohteet with tila luonnos from hakutiedot"
    (let [toteutus {:oid "1.2.246.562.13.00000000000000000009"}
          hakutiedot [{
                      :toteutusOid "1.2.246.562.13.00000000000000000009"
                      :haut [
                             {:hakuOid "1.2.246.562.29.00000000000000000009",
                      :tila "julkaistu",
                      :hakukohteet [{
                                     :hakukohdeOid "1.2.246.562.20.00000000000000000009",
                                     :tila "tallennettu"
                                     }]
                      }]}]
          julkaistut-hakutiedot (search/get-toteutuksen-julkaistut-hakutiedot hakutiedot toteutus)]
      (is (empty? (:haut julkaistut-hakutiedot))))))
