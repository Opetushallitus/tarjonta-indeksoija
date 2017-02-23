(ns tarjonta-indeksoija-service.organisaatio-client-test
  (:require [midje.sweet :refer :all]
            [tarjonta-indeksoija-service.organisaatio-client :as organisaatio-client]
            [mocks.externals-mock :refer [with-externals-mock]]))

(fact "Getting bad url should return nil"
  (with-externals-mock
    (organisaatio-client/get-doc {:oid 1234 :type "organisaatio"}) => nil))
