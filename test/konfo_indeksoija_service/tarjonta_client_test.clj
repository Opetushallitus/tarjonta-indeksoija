(ns konfo-indeksoija-service.tarjonta-client-test
  (:require [midje.sweet :refer :all]
            [konfo-indeksoija-service.tarjonta-client :as tarjonta]
            [clj-test-utils.test-utils :refer [init-test-logging]]
            [mocks.externals-mock :as mock]))

(init-test-logging)

(fact "Tarjonta client should get Hakukohde"
  (let [oid "1.2.246.562.20.99178639649"]
    (mock/with-externals-mock
      (select-keys (tarjonta/get-doc {:oid oid :type "hakukohde"}) [:oid :tarjoajaOids :koulutukset])
        => {:oid oid
            :tarjoajaOids ["1.2.246.562.10.72985435253"]
            :koulutukset [{:oid "1.2.246.562.17.67630282753"}]})))
