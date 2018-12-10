(ns konfo-indeksoija-service.search-data.koulutus-kouta
  (:require [konfo-indeksoija-service.converter.tyyppi :refer [koulutustyyppi-uri-to-tyyppi]]
            [konfo-indeksoija-service.rest.kouta :refer [get-toteutus-list-for-koulutus]]
            [konfo-indeksoija-service.rest.organisaatio :refer [find-by-oids]]
            [konfo-indeksoija-service.rest.koodisto :refer [get-koodi-with-cache]]
            [konfo-indeksoija-service.converter.common :refer [extract-paikkakunta]]
            [clojure.tools.logging :as log]))

(defn- get-tyyppi [koulutus]
  (koulutustyyppi-uri-to-tyyppi (:uri (first (:koulutustyyppis koulutus))))
  )

(defn- get-toteutukset [koulutus]
  (let [toteutukset (get-toteutus-list-for-koulutus (:oid koulutus))
        organisaatio-oids (distinct (flatten (map #(:tarjoajat %1) toteutukset)))
        organisaatiot (find-by-oids organisaatio-oids)
        oid-paikkakunta (into {} (map (fn [org]
                                        {(keyword (:oid org))
                                         {:paikkakunta (extract-paikkakunta (:metadata (get-koodi-with-cache "kunta" (:kotipaikkaUri org))))
                                          :nimi (:nimi org)
                                          :oid (:oid org)}})
                                      organisaatiot))]
    (log/info oid-paikkakunta)
    (map (fn [t]
           (assoc t :tarjoajat (map #((keyword %1) oid-paikkakunta) (:tarjoajat t)))) toteutukset)))

(defn append-search-data
  [koulutus]
  (let [tyyppi (get-tyyppi koulutus)
        toteutukset (get-toteutukset koulutus)]
    (let [searchData (-> {}
                         (assoc :tyyppi tyyppi)
                         (assoc :toteutukset toteutukset))]
    (assoc koulutus :searchData searchData))))
