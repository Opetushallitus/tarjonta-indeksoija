(ns tarjonta-indeksoija-service.tarjonta-client-test
  (:require [midje.sweet :refer :all]
            [tarjonta-indeksoija-service.test-tools :as tools]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta]
            [mocks.tarjonta-mock :as mock] ))

(fact "Tarjonta client should get Hakukohde"
  (mock/with-mocked-hakukohde "1.2.246.562.20.99178639649"
    (select-keys (tarjonta/get-hakukohde "1.2.246.562.20.99178639649") [:oid :tarjoajaOids :koulutukset])
    => {:oid "1.2.246.562.20.99178639649"
        :tarjoajaOids ["1.2.246.562.10.72985435253"]
        :koulutukset [{:oid "1.2.246.562.17.67630282753"}]}))

