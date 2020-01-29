(ns kouta-indeksoija-service.indexer.tools.organisaatio
  (:require [clojure.set :refer [intersection]]))

(defonce invalid-organisaatiotyypit #{"organisaatiotyyppi_05",
                                      "organisaatiotyyppi_06",
                                      "organisaatiotyyppi_07",
                                      "organisaatiotyyppi_08"})

(defonce organisaatiotyyppi-koulutustoimija "organisaatiotyyppi_01")
(defonce organisaatiotyyppi-oppilaitos      "organisaatiotyyppi_02")

(defn- recursive-hierarkia-v4-search
  [pred level]
  (when (not (empty? level))
    (or (first (filter pred level))
        (recursive-hierarkia-v4-search pred (mapcat :children level)))))

(defn contains-organisaatiotyyppi?
  [organisaatio organisaatiotyyppi]
  (not (empty? (filter #(= organisaatiotyyppi %) (:organisaatiotyypit organisaatio)))))

(defn oppilaitos?
  [organisaatio]
  (contains-organisaatiotyyppi? organisaatio organisaatiotyyppi-oppilaitos))

(defn koulutustoimija?
  [organisaatio]
  (contains-organisaatiotyyppi? organisaatio organisaatiotyyppi-koulutustoimija))

(defn aktiivinen?
  [organisaatio]
  (= "AKTIIVINEN" (:status organisaatio)))

(defn valid-oppilaitostyyppi?
  [organisaatio]
  (let [organisaatiotyypit (set (:organisaatiotyypit organisaatio))]
    (empty? (intersection invalid-organisaatiotyypit organisaatiotyypit))))

(defn indexable?
  [organisaatio]
  (and (aktiivinen? organisaatio) (valid-oppilaitostyyppi? organisaatio)))

(defn get-indexable-children
  [organisaatio]
  (filter indexable? (:children organisaatio)))

(defn find-koulutustoimija-from-hierarkia
  [hierarkia]
  (recursive-hierarkia-v4-search koulutustoimija? (:organisaatiot hierarkia)))

(defn find-oppilaitos-from-hierarkia
  [hierarkia]
  (recursive-hierarkia-v4-search oppilaitos? (:organisaatiot hierarkia)))

(defn- recursive-hierarkia-v4-get
  [keys level]
  (when (not (empty? level))
    (concat (map #(select-keys % keys) level)
            (recursive-hierarkia-v4-get keys (mapcat :children level)))))

(defn get-organisaatio-keys-flat
  [hierarkia keys]
  (recursive-hierarkia-v4-get keys (:organisaatiot hierarkia)))

(defn get-all-oids-flat
  [hierarkia]
  (vec (map :oid (get-organisaatio-keys-flat hierarkia [:oid]))))

(defn find-from-organisaatio-and-children
  [organisaatio oid]
  (recursive-hierarkia-v4-search #(= (:oid %) oid) (vector organisaatio)))

(defn find-from-hierarkia
  [hierarkia oid]
  (recursive-hierarkia-v4-search #(= (:oid %) oid) (:organisaatiot hierarkia)))

(defn find-oids-from-hierarkia
  [hierarkia oids]
  (vec (map #(find-from-hierarkia hierarkia %) oids)))

(defn filter-indexable-for-hierarkia
  [hierarkia oids]
  (->> oids
       (map #(find-from-hierarkia hierarkia %))
       (remove nil?)
       (filter indexable?)
       (vec)))

(defn filter-indexable-oids-for-hierarkia
  [hierarkia oids]
  (vec (map :oid (filter-indexable-for-hierarkia hierarkia oids))))