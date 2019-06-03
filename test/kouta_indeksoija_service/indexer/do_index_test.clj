(ns kouta-indeksoija-service.indexer.do-index-test
  (:require [clojure.test :refer :all]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [clj-test-utils.s3-mock-utils :refer :all]
            [kouta-indeksoija-service.elastic.queue :as q]
            [kouta-indeksoija-service.elastic.docs :as d]
            [kouta-indeksoija-service.indexer.index :as i]
            [mocks.externals-mock :as mock]
            [clj-s3.s3-connect :as s3]
            [kouta-indeksoija-service.test-tools :refer :all]
            [kouta-indeksoija-service.rest.organisaatio :as o]
            [kouta-indeksoija-service.rest.eperuste :as e]
            [kouta-indeksoija-service.util.conf :refer [env]]))

(defn setup-queue [type oid]
             (q/upsert-to-queue [{:type type :oid oid}])
             (block-until-indexed 5000)
             (q/get-queue))

(defn empty-s3 []
  (let [keys (s3/list-keys)]
    (if (not (empty? keys)) (s3/delete keys))))

(use-fixtures :once (fn [tests] (init-s3-mock) (tests) (stop-s3-mock)))
(use-fixtures :each (fn [test] (test) (reset-test-data false) (empty-s3)))

(deftest organisaatio-index-test
  (testing "index organisaatio"
    (with-redefs [env {:s3-dev-disabled "false"}
                  o/get-doc mock/get-doc
                  o/get-tyyppi-hierarkia (fn [x] {:organisaatiot []})]
      (let [oid "1.2.246.562.10.39920288212"]
        (setup-queue "organisaatio" oid)
        (i/do-index)
        (let [indexed-org (d/get-organisaatio oid)
              picture-list (s3/list-keys)]
          (is (= 1 (count picture-list)))
          (is (= "organisaatio/1.2.246.562.10.39920288212/1.2.246.562.10.39920288212.jpg" (first picture-list)))
          (is (= "muu" (get-in indexed-org [:searchData :tyyppi]))))))))

(deftest eperuste-index-test
  (testing "index eperuste"
    (with-redefs [env {:s3-dev-disabled "false"}
                  e/get-osaamisalakuvaukset (fn [x] nil)
                  e/get-doc mock/get-doc]
      (let [oid "3397334"]
        (setup-queue "eperuste" oid)
        (i/do-index)
        (let [indexed-eperuste (d/get-eperuste oid)
              picture-list (s3/list-keys)]
          (is (= 0 (count picture-list)))
          (is (= "koulutustyyppi_12" (get indexed-eperuste :koulutustyyppi))))))))