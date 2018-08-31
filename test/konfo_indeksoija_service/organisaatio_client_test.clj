(ns konfo-indeksoija-service.organisaatio-client-test
  (:require [midje.sweet :refer :all]
            [konfo-indeksoija-service.test-tools :as tools]
            [konfo-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [clj-test-utils.test-utils :refer [init-test-logging]]
            [mocks.externals-mock :refer [with-externals-mock]]
            [konfo-indeksoija-service.rest.util :as client]))

(init-test-logging)

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