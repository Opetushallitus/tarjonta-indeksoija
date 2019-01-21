(ns konfo-indeksoija-service.kouta.valintaperuste
  (:require [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [konfo-indeksoija-service.kouta.common :as common]))

(defn create-index-entry
  [id]
  (let [valintaperuste (common/complete-entry (kouta-backend/get-valintaperuste id))]
    valintaperuste))