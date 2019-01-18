(ns konfo-indeksoija-service.kouta.toteutus
  (:require [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [konfo-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [konfo-indeksoija-service.kouta.common :as common]))

(defn create-index-entry
  [oid]
  (let [toteutus (common/complete-entry (kouta-backend/get-toteutus oid))
        hakukohde-list (common/complete-entries (kouta-backend/list-hakukohteet-by-toteutus oid))]
    (assoc toteutus :hakukohteet hakukohde-list)))