(ns kouta-indeksoija-service.indexer.cache.hierarkia
  (:require [kouta-indeksoija-service.rest.organisaatio :refer [get-hierarkia-v4]]
            [kouta-indeksoija-service.indexer.tools.organisaatio :refer [get-all-oids-flat]]
            [clojure.core.cache :as cache]))

(defonce hierarkia_cache_time_millis (* 1000 60 20))

(defonce HIERARKIA_CACHE (atom (cache/ttl-cache-factory {} :ttl hierarkia_cache_time_millis)))

(defn- cache-hierarkia
  [oid]
  (when-let [hierarkia (get-hierarkia-v4 oid :aktiiviset true :suunnitellut false :lakkautetut false :skipParents false)]
    (doseq [oid (get-all-oids-flat hierarkia)]
      (swap! HIERARKIA_CACHE cache/through-cache oid (constantly hierarkia)))))

(defn get-hierarkia
  [oid]
  (if-let [hierarkia (cache/lookup @HIERARKIA_CACHE oid)]
    hierarkia
    (do (cache-hierarkia oid)
        (cache/lookup @HIERARKIA_CACHE oid))))