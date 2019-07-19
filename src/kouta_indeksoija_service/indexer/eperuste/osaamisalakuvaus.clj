(ns kouta-indeksoija-service.indexer.eperuste.osaamisalakuvaus
  (:require [kouta-indeksoija-service.rest.eperuste :as eperuste-service]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "osaamisalakuvaus")

(defn create-index-entry
  [eperuste-id]
  (let [osaamisalakuvaukset (eperuste-service/get-osaamisalakuvaukset eperuste-id)]
    (map #(assoc %1 :oid (str (:id %1)) :tyyppi "osaamisalakuvaus") osaamisalakuvaukset)))

(defn create-index-entries
  [oids]
  (apply concat (doall (pmap create-index-entry oids))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entries))

(defn get
  [oid]
  (indexable/get index-name oid))