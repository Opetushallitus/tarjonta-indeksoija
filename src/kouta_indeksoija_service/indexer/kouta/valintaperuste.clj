(ns kouta-indeksoija-service.indexer.kouta.valintaperuste
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.general :refer [not-poistettu?]]))

(def index-name "valintaperuste-kouta")

(defn create-index-entry
  [id]
  (let [valintaperuste (kouta-backend/get-valintaperuste id)]
    (if (not-poistettu? valintaperuste)
      (indexable/->index-entry id (common/complete-entry valintaperuste) valintaperuste)
      (indexable/->delete-entry id valintaperuste))))

(defn do-index
  [oids execution-id]
  (indexable/do-index index-name oids create-index-entry execution-id))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
