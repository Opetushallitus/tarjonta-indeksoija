(ns konfo-indeksoija-service.kouta.koulutus
  (:require [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [konfo-indeksoija-service.kouta.common :as common]))

(defn- shrink-toteutus
  [toteutus]
  (let [opetus (dissoc (:opetus (:metadata toteutus)) :osiot)
        metadata (dissoc (assoc (:metadata toteutus) :opetus opetus) :kuvaus :yhteystieto)]
    (dissoc (assoc toteutus :metadata metadata) :koulutusOid :kielivalinta)))

(defn create-index-entry
  [oid]
  (let [koulutus (common/complete-entry (kouta-backend/get-koulutus oid))
        toteutukset (common/complete-entries (kouta-backend/get-toteutus-list-for-koulutus oid))]
    (assoc koulutus :toteutukset (map shrink-toteutus toteutukset))))