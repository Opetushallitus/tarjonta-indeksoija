(ns kouta-indeksoija-service.indexer.kouta.sorakuvaus
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.general :refer [not-poistettu?]]))

(def index-name "sorakuvaus-kouta")

(defn create-index-entry
  [id execution-id]
  (let [sorakuvaus (kouta-backend/get-sorakuvaus id execution-id)]
    (if (not-poistettu? sorakuvaus)
      (indexable/->index-entry-with-forwarded-data id (common/complete-entry sorakuvaus) sorakuvaus)
      (indexable/->delete-entry-with-forwarded-data id sorakuvaus))))

(defn do-index
  [ids execution-id]
  (indexable/do-index index-name ids create-index-entry execution-id))

(defn get-from-index
  [id]
  (indexable/get index-name id))
