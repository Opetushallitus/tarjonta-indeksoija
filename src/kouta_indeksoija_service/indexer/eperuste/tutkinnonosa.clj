(ns kouta-indeksoija-service.indexer.eperuste.tutkinnonosa
  (:require [kouta-indeksoija-service.rest.eperuste :as eperuste-service]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "tutkinnonosa")

(defn create-index-entry
  [id execution-id]
  (when-let [tutkinnonosa (eperuste-service/get-tutkinnonosa id)]
    (let [id (str (:id tutkinnonosa))]
      (indexable/->index-entry id (assoc tutkinnonosa :oid id :tyyppi "tutkinnonosa")))))

(defn do-index
  [ids execution-id]
  (indexable/do-index index-name ids create-index-entry execution-id))

(defn get-from-index
  [id]
  (indexable/get index-name id))
