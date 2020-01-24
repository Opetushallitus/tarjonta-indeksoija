(ns kouta-indeksoija-service.indexer.cache.hierarkia
  (:require [kouta-indeksoija-service.rest.organisaatio :refer [get-hierarkia-v4]]
            [kouta-indeksoija-service.indexer.tools.organisaatio :refer :all]
            [clojure.core.cache :as cache]))

(defonce hierarkia_cache_time_millis (* 1000 60 20))

(defonce HIERARKIA_CACHE (atom (cache/ttl-cache-factory {} :ttl hierarkia_cache_time_millis)))

(defn- do-cache
  [hierarkia oids]
  (doseq [oid oids]
    (swap! HIERARKIA_CACHE cache/through-cache oid (constantly hierarkia))))

(defn cache-hierarkia
  [oid]
  (when-let [hierarkia (get-hierarkia-v4 oid :aktiiviset true :suunnitellut false :lakkautetut true :skipParents false)]
    (let [this (find-from-hierarkia hierarkia oid)]
      (cond
        (koulutustoimija? this) (do-cache hierarkia (vector oid))
        (oppilaitos? this)      (do-cache hierarkia (filter #(not (koulutustoimija? %)) (get-all-oids-flat hierarkia)))
        :else                   (when-let [oppilaitos-oid (:oid (find-oppilaitos-from-hierarkia hierarkia))]
                                  (cache-hierarkia oppilaitos-oid))))))

(defn get-hierarkia
  [oid]
  (if-let [hierarkia (cache/lookup @HIERARKIA_CACHE oid)]
    hierarkia
    (do (cache-hierarkia oid)
        (cache/lookup @HIERARKIA_CACHE oid))))