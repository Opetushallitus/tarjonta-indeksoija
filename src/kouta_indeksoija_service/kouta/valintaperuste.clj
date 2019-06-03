(ns kouta-indeksoija-service.kouta.valintaperuste
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.kouta.common :as common]
            [kouta-indeksoija-service.kouta.indexable :as indexable]))

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

(defn get
  [oid]
  (indexable/get index-name oid))
