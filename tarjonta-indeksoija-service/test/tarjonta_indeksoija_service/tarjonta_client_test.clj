(ns tarjonta-indeksoija-service.tarjonta-client-test
  (require [midje.sweet :refer :all]
           [tarjonta-indeksoija-service.tarjonta-client :as tarjonta]))

(def hakukohde-oid "1.2.246.562.20.14326014835")

(fact "Tarjonta client should get Hakukohde"
  (select-keys (tarjonta/get-koulutus hakukohde-oid) [:oid :tarjoajaOids :koulutukset])
    => {:oid hakukohde-oid
      :tarjoajaOids ["1.2.246.562.10.72985435253"]
      :koulutukset [{:oid "1.2.246.562.17.974238694910"}]})