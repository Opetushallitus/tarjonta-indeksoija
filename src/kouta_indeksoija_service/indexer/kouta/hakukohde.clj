(ns kouta-indeksoija-service.indexer.kouta.hakukohde
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "hakukohde-kouta")

(defn create-index-entry
  [oid]
  (let [hakukohde (common/complete-entry (kouta-backend/get-hakukohde oid))]
    (if-let [valintaperusteId (:valintaperusteId hakukohde)]
      (let [valintaperustekuvaus (common/complete-entry (dissoc (kouta-backend/get-valintaperuste valintaperusteId) :metadata))]
        (-> hakukohde
            (assoc :valintaperuste valintaperustekuvaus)
            (dissoc :valintaperusteId)))
        (dissoc hakukohde :valintaperusteId))))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entries))

(defn get
  [oid]
  (indexable/get index-name oid))
