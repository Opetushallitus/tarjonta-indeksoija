(ns tarjonta-indeksoija-service.converter.hakukohde-converter-test
  (:require [midje.sweet :refer :all]
            [tarjonta-indeksoija-service.converter.hakukohde-converter :as converter]))

  (facts "Converter"
    (fact
    "should convert hakukohde"
    (let [haukohde {:koulutukset [{:oid "1.2.246.562.17.67630282753"}]
                    :koulutusmoduuliToteutusTarjoajatiedot {:1.2.246.562.17.53874141319 {:tarjoajaOids ["1.2.246.562.10.39920288212"]}}}
          expected-haukohde {:koulutukset ["1.2.246.562.17.67630282753"]
                             :koulutusmoduuliToteutusTarjoajatiedot[{:koulutus  "1.2.246.562.17.53874141319"
                                                                     :tarjoajaOids ["1.2.246.562.10.39920288212"]}]}]
        (converter/convert haukohde) => expected-haukohde)))