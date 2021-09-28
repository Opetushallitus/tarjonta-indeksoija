(ns kouta-indeksoija-service.indexer.tools.koodisto-tools-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto]))

(deftest filter-expired-koodi
  (testing "filters koodi with nil voimassaLoppuPvm"
    (let [koodit [{:voimassaLoppuPvm nil}]
          filtered-koodit (koodisto/filter-expired koodit)]
      (is (= 1 (count filtered-koodit)))))

  (testing "filters koodi with voimassaLoppuPvm in future"
    (let [koodit [{:voimassaLoppuPvm "2099-01-01"}]
          filtered-koodit (koodisto/filter-expired koodit)]
      (is (= 1 (count filtered-koodit)))))

  (testing "does not filter koodi with voimassaLoppuPvm in past"
    (let [koodit [{:voimassaLoppuPvm "2019-01-01"}]
          filtered-koodit (koodisto/filter-expired koodit)]
      (is (empty? filtered-koodit))))

  (testing "filters koodit with valid voimassaLoppuPvm"
    (let [koodit [{:voimassaLoppuPvm "2019-01-01"}
                  {:voimassaLoppuPvm "2099-01-01"}
                  {:voimassaLoppuPvm nil}]
          filtered-koodit (koodisto/filter-expired koodit)]
      (is (= 2 (count filtered-koodit)))
      (is (= [{:voimassaLoppuPvm "2099-01-01"}
             {:voimassaLoppuPvm nil}] filtered-koodit)))))


