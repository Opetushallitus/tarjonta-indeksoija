(ns kouta-indeksoija-service.indexer.kouta.valintaperuste
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.general :refer [not-poistettu?]]))

(def index-name "valintaperuste-kouta")

(defn create-index-entry
  [id execution-id]
  (let [valintaperuste (common/localize-dates (common/complete-entry (kouta-backend/get-valintaperuste-with-cache id execution-id)))]
    (if (not-poistettu? valintaperuste)
      (indexable/->index-entry-with-forwarded-data id valintaperuste valintaperuste)
      (indexable/->delete-entry-with-forwarded-data id valintaperuste))))

(defn do-index
  [oids execution-id]
  (indexable/do-index index-name oids create-index-entry execution-id))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
