(ns konfo-indeksoija-service.kouta.hakukohde
  (:require [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [konfo-indeksoija-service.kouta.common :as common]
            [konfo-indeksoija-service.kouta.indexable :as indexable]))

(def index-name "hakukohde-kouta")

(defn create-index-entry
  [oid]
  (let [hakukohde (common/complete-entry (kouta-backend/get-hakukohde oid))
        valintaperustekuvaus (common/complete-entry (kouta-backend/get-valintaperuste (:valintaperusteId hakukohde)))]
    (-> hakukohde
        (assoc :valintaperuste valintaperustekuvaus)
        (dissoc :valintaperusteId))))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entries))