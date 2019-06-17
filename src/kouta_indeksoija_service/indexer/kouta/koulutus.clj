(ns kouta-indeksoija-service.indexer.kouta.koulutus
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "koulutus-kouta")

(defn- to-list-item
  [toteutus]
  (-> {}
      (assoc :oid (:oid toteutus))
      (assoc :organisaatio (:organisaatio toteutus))
      (assoc :nimi (:nimi toteutus))
      (assoc :tila (:tila toteutus))
      (assoc :tarjoajat (:tarjoajat toteutus))
      (assoc :muokkaaja (:muokkaaja toteutus))
      (assoc :modified (:modified toteutus))))

(defn create-index-entry
  [oid]
  (let [koulutus (common/complete-entry (kouta-backend/get-koulutus oid))
        toteutukset (common/complete-entries (kouta-backend/get-toteutus-list-for-koulutus oid))]
    (-> koulutus
        (assoc :toteutukset (map to-list-item toteutukset)))))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entries))

(defn get
  [oid]
  (indexable/get index-name oid))