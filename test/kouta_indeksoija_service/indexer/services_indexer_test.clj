(ns kouta-indeksoija-service.indexer.services-indexer-test
  (:require [clojure.test :refer :all]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [clj-test-utils.s3-mock-utils :refer :all]
            [kouta-indeksoija-service.indexer.eperuste.eperuste :as eperuste]
            [kouta-indeksoija-service.indexer.organisaatio.organisaatio :as organisaatio]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [mocks.externals-mock :as mock]
            [clj-s3.s3-connect :as s3]
            [kouta-indeksoija-service.test-tools :refer :all]
            [kouta-indeksoija-service.util.conf :refer [env]]))

(defn empty-s3 []
  (let [keys (s3/list-keys)]
    (if (not (empty? keys)) (s3/delete keys))))

(use-fixtures :once (fn [tests] (init-s3-mock) (tests) (stop-s3-mock)))
(use-fixtures :each (fn [test] (test) (reset-test-data false) (empty-s3)))

(deftest eperuste-index-test
  (testing "do index eperuste"
    (with-redefs [kouta-indeksoija-service.rest.eperuste/get-doc mock/get-eperuste-doc
                  kouta-indeksoija-service.rest.eperuste/get-osaamisalakuvaukset (fn [x] [])]
      (i/index-eperusteet ["3397334"])
      (let [indexed-eperuste (eperuste/get "3397334")
            picture-list (s3/list-keys)]
        (is (= 0 (count picture-list)))
        (is (= "koulutustyyppi_12" (get indexed-eperuste :koulutustyyppi)))))))

(deftest organisaatio-index-test
  (testing "index organisaatio"
    (with-redefs [env {:s3-dev-disabled "false"}
                  kouta-indeksoija-service.rest.organisaatio/get-doc mock/get-organisaatio-doc
                  kouta-indeksoija-service.rest.organisaatio/get-tyyppi-hierarkia (fn [x] {:organisaatiot []})]
      (let [oid "1.2.246.562.10.39920288212"]
        (i/index-organisaatiot [oid])
        (let [indexed-org (organisaatio/get oid)
              picture-list (s3/list-keys)]
          (is (= 1 (count picture-list)))
          (is (= "organisaatio/1.2.246.562.10.39920288212/1.2.246.562.10.39920288212.jpg" (first picture-list)))
          (is (= "muu" (get-in indexed-org [:searchData :tyyppi]))))))))