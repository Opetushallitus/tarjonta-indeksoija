(ns kouta-indeksoija-service.kouta.hakukohde
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.kouta.common :as common]
            [kouta-indeksoija-service.kouta.indexable :as indexable]))

(def index-name "hakukohde-kouta")

(defn create-index-entry
  [oid]
  (let [hakukohde (common/complete-entry (kouta-backend/get-hakukohde oid))
        valintaperustekuvaus (common/complete-entry (dissoc (kouta-backend/get-valintaperuste (:valintaperusteId hakukohde)) :metadata))]
    (-> hakukohde
        (assoc :valintaperuste valintaperustekuvaus)
        (dissoc :valintaperusteId))))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entries))
