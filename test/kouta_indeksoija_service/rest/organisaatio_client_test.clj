(ns kouta-indeksoija-service.rest.organisaatio-client-test
  (:require [midje.sweet :refer :all]
            [kouta-indeksoija-service.test-tools :as tools]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [mocks.externals-mock :refer [with-externals-mock]]
            [kouta-indeksoija-service.rest.util :as client]))

(defn mock-last-modified [url opts]
  {:body (tools/parse (str "test/resources/organisaatiot/last-modified.json"))})

(fact "Getting bad url should return nil"
  (with-externals-mock
    (organisaatio-client/get-doc {:oid 1234 :type "organisaatio"}) => nil))

(fact "Get last modified"
  (with-redefs [client/get mock-last-modified]
    (let [res (organisaatio-client/find-last-changes (System/currentTimeMillis))]
      res => [{:oid "1.2.246.562.10.129178838410" :type "organisaatio"}
              {:oid "1.2.246.562.10.97852531538" :type "organisaatio"}
              {:oid "1.2.246.562.10.336097503610" :type "organisaatio"}
              {:oid "1.2.246.562.28.12771401465" :type "organisaatio"}])))
