(ns konfo-indeksoija-service.kouta.koulutus
  (:require [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [konfo-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [konfo-indeksoija-service.kouta.common :as common]))

(defn- shrink-toteutus
  [toteutus]
  (-> toteutus
      (assoc :metadata (-> toteutus
                           (:metadata)
                           (assoc :opetus (-> toteutus
                                              (:metadata)
                                              (:opetus)
                                              (dissoc :osiot)))
                           (dissoc :kuvaus :yhteystieto)))
      (dissoc :koulutusOid :kielivalinta)))

(defn create-index-entry
  [oid]
  (let [koulutus (common/complete-entry (kouta-backend/get-koulutus oid))
        toteutukset (common/complete-entries (kouta-backend/get-toteutus-list-for-koulutus oid))]
    (-> koulutus
        (assoc :toteutukset (map shrink-toteutus toteutukset)))))



(comment assoc :koulutus (get-koodi-nimi-with-cache "koulutus" (:koulutusKoodiUri koulutus)))
(comment dissoc :koulutusKoodiUri)
