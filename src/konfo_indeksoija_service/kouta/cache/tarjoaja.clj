(ns konfo-indeksoija-service.kouta.cache.tarjoaja
  (:require [konfo-indeksoija-service.rest.organisaatio :as organisaatio-service]
            [konfo-indeksoija-service.rest.koodisto :refer [get-koodi-with-cache]]
            [clojure.core.cache :as cache]))

(defonce cache_time_millis (* 1000 60 20))

(defonce CACHE (atom (cache/ttl-cache-factory {} :ttl cache_time_millis)))

(defn paikkakunta
  [organisaatio]
  (defn extract
    [value]
    (reduce #(assoc %1 (keyword (clojure.string/lower-case (:kieli %2))) (:nimi %2)) {} value))
  (extract (:metadata (get-koodi-with-cache "kunta" (:kotipaikkaUri organisaatio)))))

(defn tarjoaja
  [organisaatio]
  {:paikkakunta (paikkakunta organisaatio)
   :nimi (:nimi organisaatio)
   :oid (:oid organisaatio)})

(defn get-tarjoaja
  [oid]
  (swap! CACHE cache/through-cache oid #(tarjoaja (organisaatio-service/get-by-oid %1)))
  (cache/lookup @CACHE oid))

(defn cache-tarjoajat
  [oids]
  (let [tarjoajat (map tarjoaja (organisaatio-service/find-by-oids oids))]
    (map (fn [t] (swap! CACHE cache/through-cache (:oid t) (constantly t))) tarjoajat)))