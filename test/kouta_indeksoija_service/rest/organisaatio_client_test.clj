(ns kouta-indeksoija-service.rest.organisaatio-client-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.test-tools :as tools]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.rest.util :as client]))

(defn mock-last-modified [url opts]
  {:body (tools/parse (str "test/resources/organisaatiot/last-modified.json"))})

(deftest organisaatio-client-test
  (testing "Organisaatio client should"
    (testing "get last modified"
      (with-redefs [client/get mock-last-modified]
        (let [res      (organisaatio-client/find-last-changes (System/currentTimeMillis))
              expected [{:oid "1.2.246.562.10.129178838410" :type "organisaatio"}
                        {:oid "1.2.246.562.10.97852531538" :type "organisaatio"}
                        {:oid "1.2.246.562.10.336097503610" :type "organisaatio"}
                        {:oid "1.2.246.562.28.12771401465" :type "organisaatio"}]]
          (is (= expected res)))))))