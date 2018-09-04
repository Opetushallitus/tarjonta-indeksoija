(ns konfo-indeksoija-service.converter.koulutusmoduuli
  (:require [konfo-indeksoija-service.converter.common :refer :all]))

(def map-koulutusmoduuli-fields {:tila value
                      :eqf koodi
                      :lukiolinja value
                      :koulutustyyppis koodi-list ;Ilmeisesti nykytarjonnan datassa koulutusmoduulilla on vain yksi koulutustyyppi,
                                                  ;mutta rajapinnasta palautetaan koulutustyypit kuitenkin listana.
                      :koulutusasteTyyppi value
                      :osaamisala value
                      :oppilaitostyyppis value
                      :ohjelmas value
                      :opintojenLaajuusyksikko koodi
                      :koulutusala koodi
                      :koulutusohjelma koodi
                      :koulutusaste koodi
                      :modified value
                      :koulutuskoodi koodi
                      :kuvausKomo kuvaus
                      :opintoala koodi
                      :nimi value
                      :oid value
                      :tyyppi value
                      :nqf koodi
                      :koulutusmoduuliTyyppi value
                      :tutkintonimikes koodi-list
                      :tutkinto koodi
                      :komoOid value
                      :tunniste value
                      :version value
                      :organisaatio value
                      :opintojenLaajuusarvo koodi})

(defn convert
  [dto]
  (let [raw-res (into {} (for [[k v] dto] [k ((k map-koulutusmoduuli-fields) v)]))]
    raw-res))
