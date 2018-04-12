(ns konfo-indeksoija-service.organisaatio-client-test
  (:require [midje.sweet :refer :all]
            [konfo-indeksoija-service.organisaatio-client :as organisaatio-client]
            [konfo-indeksoija-service.test-tools :refer [init-test-logging]]
            [mocks.externals-mock :refer [with-externals-mock]]))

(init-test-logging)

(fact "Getting bad url should return nil"
  (with-externals-mock
    (organisaatio-client/get-doc {:oid 1234 :type "organisaatio"}) => nil))
