(ns konfo-indeksoija-service.search-data.koulutus-kouta
  (:require [konfo-indeksoija-service.converter.tyyppi :refer [koulutustyyppi-uri-to-tyyppi]]
            [konfo-indeksoija-service.rest.kouta :refer [get-toteutus-list-for-koulutus]]
            [konfo-indeksoija-service.rest.organisaatio :refer [find-by-oids]]
            [konfo-indeksoija-service.rest.koodisto :refer [get-koodi-with-cache]]
            [konfo-indeksoija-service.converter.common :refer [extract-paikkakunta]]
            [clojure.tools.logging :as log]))

;TODO -> Hae muokkaaja nimi oppijanumerorekisteristä (vaatii CASsin)

(defn- get-tyyppi [koulutus]
  (koulutustyyppi-uri-to-tyyppi (:uri (first (:koulutustyyppis koulutus)))))

(defn- get-toteutukset [koulutus]
  (let [toteutukset (get-toteutus-list-for-koulutus (:oid koulutus))
        organisaatio-oids (distinct (flatten (map #(:tarjoajat %1) toteutukset)))
        organisaatiot (find-by-oids organisaatio-oids)
        oid-paikkakunta (into {} (map (fn [org]
                                        {(keyword (:oid org))
                                         {:paikkakunta (clojure.set/rename-keys (extract-paikkakunta (:metadata (get-koodi-with-cache "kunta" (:kotipaikkaUri org)))) {:FI :fi :SV :sv :EN :en})
                                          :nimi (:nimi org)
                                          :oid (:oid org)}})
                                      organisaatiot))]
    (log/info oid-paikkakunta)
    (map (fn [t]
           (assoc t :tarjoajat (map #((keyword %1) oid-paikkakunta) (:tarjoajat t))
                    :muokkaaja { :oid (:muokkaaja t) :nimi (rand-nth ["Aku Ankka" "Minni Hiiri" "Mikki Hiiri"])})) toteutukset)))

(defn append-search-data
  [koulutus]
  (let [tyyppi (get-tyyppi koulutus)
        muokkaaja { :oid ( :muokkaaja koulutus) :nimi (rand-nth ["Aku Ankka" "Minni Hiiri" "Mikki Hiiri"])}
        toteutukset (get-toteutukset koulutus)]
    (let [searchData (-> {}
                         (assoc :tyyppi tyyppi)
                         (assoc :toteutukset toteutukset))]
    (assoc koulutus :searchData searchData :muokkaaja muokkaaja))))
