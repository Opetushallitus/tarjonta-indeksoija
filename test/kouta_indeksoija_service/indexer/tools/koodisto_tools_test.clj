(ns kouta-indeksoija-service.indexer.tools.koodisto-tools-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto]
            [kouta-indeksoija-service.rest.koodisto :as rest-koodisto]))

(defn- mock-get-ylakoodisto
  [koodi-uri]
  [{:koodisto {:koodistoUri "kansallinenkoulutusluokitus2016koulutusalataso1"}
    :koodiUri "ylataso1uri"}
   {:koodisto {:koodistoUri "kansallinenkoulutusluokitus2016koulutusalataso2"}
    :koodiUri "ylataso2uri"}
   {:koodisto {:koodistoUri "kansallinenkoulutusluokitus2016koulutusalataso3"}
    :koodiUri "ylataso3uri"}])

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
             {:voimassaLoppuPvm nil}] filtered-koodit))))

  (testing "retrieves ylakoodis for koodisto"
    (with-redefs [rest-koodisto/get-ylakoodit-with-cache mock-get-ylakoodisto]
      (let [ylakoodit (set (koodisto/koulutusalan-ylakoulutusalat "alakoulutusala"))]
        (is (= 2 (count ylakoodit)))
        (is (contains? ylakoodit "ylataso1uri"))
        (is (contains? ylakoodit "ylataso2uri"))))))


