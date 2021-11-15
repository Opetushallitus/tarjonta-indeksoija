(ns kouta-indeksoija-service.indexer.kouta.sorakuvaus
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "sorakuvaus-kouta")

(defn create-index-entry
  [id]
  (->> id
       (kouta-backend/get-sorakuvaus)
       (common/complete-entry)
       (indexable/->index-entry id)))

(defn do-index
  [ids execution-id]
  (indexable/do-index index-name ids create-index-entry execution-id))

(defn get-from-index
  [id]
  (indexable/get index-name id))
