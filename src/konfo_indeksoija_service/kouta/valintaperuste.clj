(ns konfo-indeksoija-service.kouta.valintaperuste
  (:require [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [konfo-indeksoija-service.kouta.common :as common]
            [konfo-indeksoija-service.kouta.indexable :as indexable]))

(def index-name "valintaperuste-kouta")

(defn create-index-entry
  [id]
  (let [valintaperuste (common/complete-entry (kouta-backend/get-valintaperuste id))]
    valintaperuste))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entries))