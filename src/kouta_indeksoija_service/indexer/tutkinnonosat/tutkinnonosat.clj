(ns kouta-indeksoija-service.indexer.tutkinnonosat.tutkinnonosat
  (:require [kouta-indeksoija-service.rest.eperuste :as eperuste-service]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "tutkinnonosat")

(defn create-index-entry
  [id]
  (when-let [tutkinnonosat (eperuste-service/get-tutkinnonosat id)]
    (let [id (str (:id tutkinnonosat))]
      (indexable/->index-entry id (assoc tutkinnonosat :oid id :tyyppi "tutkinnonosat")))))

(defn do-index
  [ids]
  (indexable/do-index index-name ids create-index-entry))

(defn get
  [id]
  (indexable/get index-name id))
