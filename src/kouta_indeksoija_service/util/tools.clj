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

(defn- organisaatio-jarjestaa-urheilijan-amm-koulutusta?
  [organisaatio]
  (boolean (get-in organisaatio [:metadata :jarjestaaUrheilijanAmmKoulutusta])))

(defn oppilaitos-jarjestaa-urheilijan-amm-koulutusta?
  [oppilaitos]
  (or (organisaatio-jarjestaa-urheilijan-amm-koulutusta? oppilaitos)
      (some #(organisaatio-jarjestaa-urheilijan-amm-koulutusta? %) (:osat oppilaitos))))

(defn get-oids
  [key coll]
  (set (remove clojure.string/blank? (map key coll))))
