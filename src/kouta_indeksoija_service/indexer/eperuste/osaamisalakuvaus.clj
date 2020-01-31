(ns kouta-indeksoija-service.indexer.eperuste.osaamisalakuvaus
  (:require [kouta-indeksoija-service.rest.eperuste :as eperuste-service]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "osaamisalakuvaus")

(defn- ->to-index-entry
  [osaamisalakuvaus]
  (let [id (str (:id osaamisalakuvaus))]
    (indexable/->index-entry id (assoc osaamisalakuvaus :oid id :tyyppi "osaamisalakuvaus"))))

(defn create-index-entry
  [eperuste-id]
  (let [osaamisalakuvaukset (eperuste-service/get-osaamisalakuvaukset eperuste-id)]
    (map ->to-index-entry osaamisalakuvaukset)))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get
  [oid]
  (indexable/get index-name oid))