(ns kouta-indeksoija-service.indexer.cache.hierarkia
  (:require [kouta-indeksoija-service.rest.organisaatio :refer [get-all-organisaatiot-with-cache clear-get-all-organisaatiot-cache get-hierarkia-v4]]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as o]
            [clojure.core.cache :as cache]))

(defonce hierarkia_cache_time_millis (* 1000 60 45))

(defonce HIERARKIA_CACHE (atom (cache/ttl-cache-factory {} :ttl hierarkia_cache_time_millis)))

(defn- do-cache
  [hierarkia oids]
  (doseq [oid oids]
    (swap! HIERARKIA_CACHE cache/through-cache oid (constantly hierarkia))))

(defn cache-hierarkia
  [oid]
  (when-let [hierarkia (get-hierarkia-v4 oid)]
    (let [this (o/find-from-hierarkia hierarkia oid)]
      (cond
        (o/koulutustoimija? this) (do-cache hierarkia (vector oid))
        (o/oppilaitos? this)      (do-cache hierarkia (filter #(not (o/koulutustoimija? %)) (o/get-all-oids-flat hierarkia)))
        :else                     (when-let [oppilaitos-oid (:oid (o/find-oppilaitos-from-hierarkia hierarkia))]
                                    (cache-hierarkia oppilaitos-oid))))))

(defn get-hierarkia
  [oid]
  (if-let [hierarkia (cache/lookup @HIERARKIA_CACHE oid)]
    hierarkia
    (do (cache-hierarkia oid)
        (cache/lookup @HIERARKIA_CACHE oid))))

(defn- do-evict
  [oids]
  (doseq [oid oids]
    (swap! HIERARKIA_CACHE cache/evict oid)))

(defn clear-hierarkia
  [oid]
  (when-let [hierarkia (cache/lookup @HIERARKIA_CACHE oid)]
    (do-evict (o/get-all-oids-flat hierarkia))))