(ns tarjonta-indeksoija-service.tarjonta-client-test
  (:require [midje.sweet :refer :all]
            [tarjonta-indeksoija-service.test-tools :as tools]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta]
            [mocks.tarjonta-mock :as mock]))

(fact "Tarjonta client should get Hakukohde"
  (let [oid "1.2.246.562.20.99178639649"]
    (mock/with-tarjonta-mock {:oid oid :type "hakukohde"}
      (select-keys (tarjonta/get-doc {:oid oid :type "hakukohde"}) [:oid :tarjoajaOids :koulutukset])
        => {:oid oid
            :tarjoajaOids ["1.2.246.562.10.72985435253"]
            :koulutukset [{:oid "1.2.246.562.17.67630282753"}]})))
