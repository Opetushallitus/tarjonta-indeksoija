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

(defn jarjestaa-urheilijan-amm-koulutusta?
  [jarjestyspaikka-oid oppilaitos]
  (if (= jarjestyspaikka-oid (:oid oppilaitos))
    (organisaatio-jarjestaa-urheilijan-amm-koulutusta? oppilaitos)
    (let [oppilaitoksen-osa (first (filter #(= jarjestyspaikka-oid (:oid %)) (:osat oppilaitos)))]
      (organisaatio-jarjestaa-urheilijan-amm-koulutusta? oppilaitoksen-osa))))

(defn get-oids
  [key coll]
  (set (remove clojure.string/blank? (map key coll))))
