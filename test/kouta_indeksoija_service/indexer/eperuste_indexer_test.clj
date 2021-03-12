(ns kouta-indeksoija-service.indexer.eperuste-indexer-test
  (:require [clojure.test :refer :all]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [kouta-indeksoija-service.indexer.eperuste.eperuste :as eperuste]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [mocks.externals-mock :as mock]
            [kouta-indeksoija-service.test-tools :refer :all]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.elastic.admin :as admin]))

(use-fixtures :once (fn [tests]
                      (admin/initialize-eperuste-indices-for-reindexing)
                      (tests)))

(deftest eperuste-index-test
  (testing "do index eperuste"
    (with-redefs [kouta-indeksoija-service.rest.eperuste/get-doc mock/get-eperuste-doc
                  kouta-indeksoija-service.rest.eperuste/get-osaamisalakuvaukset (fn [x] [])]
      (i/index-eperusteet ["3397334"])
      (let [indexed-eperuste (eperuste/get-from-index "3397334")]
        (is (= "koulutustyyppi_12" (get indexed-eperuste :koulutustyyppi)))))))
