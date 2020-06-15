(ns kouta-indeksoija-service.indexer.lokalisointi.lokalisointi
  (:require [kouta-indeksoija-service.rest.lokalisointi :as lokalisointi-service]
            [kouta-indeksoija-service.lokalisointi.util :as util]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "lokalisointi")

(defn create-index-entry
  [lng]
  (when-let [lokalisointi (some-> (lokalisointi-service/get lng)
                                  (seq)
                                  (util/localisation->nested-json))]
    (indexable/->index-entry lng {:lng lng :tyyppi "lokalisointi" :translation lokalisointi})))

(defn do-index
  [lngs]
  (indexable/do-index index-name lngs create-index-entry))

(defn get
  [lng]
  (indexable/get index-name lng))
