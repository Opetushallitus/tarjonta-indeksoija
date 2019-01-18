(ns konfo-indeksoija-service.kouta.hakukohde
  (:require [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [konfo-indeksoija-service.kouta.common :as common]))

(defn create-index-entry
  [oid]
  (let [hakukohde (common/complete-entry (kouta-backend/get-hakukohde oid))
        valintaperustekuvaus (common/complete-entry (kouta-backend/get-valintaperuste (:valintaperuste hakukohde)))]
    (assoc hakukohde :valintaperuste valintaperustekuvaus)))