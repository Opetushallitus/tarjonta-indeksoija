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
  (let [eperuste (eperuste-service/get-doc-with-cache eperuste-id)
        osaamialakuvaukset (eperuste-service/get-osaamisalakuvaukset eperuste-id (:tila eperuste))]
    (map ->to-index-entry osaamialakuvaukset)))

(defn do-index
  [oids & execution-id]
  (indexable/do-index index-name oids create-index-entry execution-id))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))