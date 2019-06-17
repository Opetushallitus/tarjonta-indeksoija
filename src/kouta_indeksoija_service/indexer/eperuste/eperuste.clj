(ns kouta-indeksoija-service.indexer.eperuste.eperuste
  (:require [kouta-indeksoija-service.rest.eperuste :as eperuste-service]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "eperuste")

(defn create-index-entry
  [oid]
  (when-let [eperuste (eperuste-service/get-doc {:oid oid})]
    (assoc eperuste :oid (str (:id eperuste)) :tyyppi "eperuste")))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entries))

(defn get
  [oid]
  (indexable/get index-name oid))