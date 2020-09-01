(ns kouta-indeksoija-service.indexer.eperuste.tutkinnonosa
  (:require [kouta-indeksoija-service.rest.eperuste :as eperuste-service]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "tutkinnonosa")

(defn create-index-entry
  [id]
  (when-let [tutkinnonosa (eperuste-service/get-tutkinnonosa id)]
    (let [id (str (:id tutkinnonosa))]
      (indexable/->index-entry id (assoc tutkinnonosa :oid id :tyyppi "tutkinnonosa")))))

(defn do-index
  [ids]
  (indexable/do-index index-name ids create-index-entry))

(defn get
  [id]
  (indexable/get index-name id))
