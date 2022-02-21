(ns kouta-indeksoija-service.indexer.cache.hierarkia
  (:require [kouta-indeksoija-service.rest.organisaatio :refer [get-all-organisaatiot-with-cache clear-get-all-organisaatiot-cache get-hierarkia-for-oid-from-cache]]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as o]
            [clojure.core.cache :as cache]))

(defonce hierarkia_cache_time_millis (* 1000 60 45))

(defn- make-cache-factory [] (cache/ttl-cache-factory {} :ttl hierarkia_cache_time_millis))

(defonce HIERARKIA_CACHE (atom (make-cache-factory)))

(defn reset-hierarkia-cache [] (reset! HIERARKIA_CACHE (make-cache-factory)))

(defn- do-cache
  [hierarkia oids]
  (doseq [oid oids]
    (swap! HIERARKIA_CACHE cache/through-cache oid (constantly hierarkia))))

(defn cache-hierarkia
  [oid]
  (when-let [hierarkia (get-hierarkia-for-oid-from-cache oid)]
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