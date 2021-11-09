(ns kouta-indeksoija-service.indexer.lokalisointi.lokalisointi
  (:require [kouta-indeksoija-service.rest.lokalisointi :as lokalisointi-service]
            [kouta-indeksoija-service.lokalisointi.util :as util]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "lokalisointi")

(defn create-index-entry
  [lng]
  (when-let [lokalisointi (some-> (lokalisointi-service/do-get lng)
                                  (seq)
                                  (util/localisation->nested-json))]
    (indexable/->index-entry lng {:lng lng :tyyppi "lokalisointi" :translation lokalisointi})))

(defn do-index
  [lngs & execution-id]
  (indexable/do-index index-name lngs create-index-entry execution-id))

(defn get-from-index
  [lng]
  (indexable/get index-name lng))
