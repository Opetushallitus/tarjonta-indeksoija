(ns kouta-indeksoija-service.util.tools
  (:require [clojure.string :refer [blank? split lower-case]]))

(defn uuid
  []
  (.toString (java.util.UUID/randomUUID)))

(defn get-id
  [doc]
  (or (:oid doc) (:id doc)))

(defn comma-separated-string->vec
  [s]
  (vec (remove blank? (some-> s (split #",")))))

(defn ->distinct-vec
  [coll]
  (vec (distinct (remove nil? coll))))

(defn get-esitysnimi
  [entity]
  (get-in entity [:_enrichedData :esitysnimi] (:nimi entity)))

(defn jarjestaa-urheilijan-amm-koulutusta?
  [jarjestyspaikka]
  (boolean (get-in jarjestyspaikka [:metadata :jarjestaaUrheilijanAmmKoulutusta])))

(defn get-oids
  [key coll]
  (set (remove clojure.string/blank? (map key coll))))
