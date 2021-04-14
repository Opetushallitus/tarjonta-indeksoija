(ns kouta-indeksoija-service.indexer.kouta.valintaperuste
  (:require [clojure.string :as s]
            [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "valintaperuste-kouta")

(defn create-index-entry
  [id]
  (let [valintaperuste (common/complete-entry (kouta-backend/get-valintaperuste id))]
    (if-let [sorakuvaus-id (:sorakuvausId valintaperuste)]
      (indexable/->index-entry id (assoc valintaperuste :sorakuvaus (common/complete-entry (kouta-backend/get-sorakuvaus sorakuvaus-id))))
      (indexable/->index-entry id valintaperuste))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
