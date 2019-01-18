(ns konfo-indeksoija-service.kouta.haku
  (:require [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [konfo-indeksoija-service.kouta.common :as common]))

(defn create-index-entry
  [oid]
  (let [haku (common/complete-entry (kouta-backend/get-haku oid))
        hakukohde-list (common/complete-entries (kouta-backend/list-hakukohteet-by-haku oid))]
    (assoc haku :hakukohteet hakukohde-list)))