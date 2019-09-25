(ns kouta-indeksoija-service.indexer.tools.organisaatio)

(defn- recursive-hierarkia-v4-search
  [pred level]
  (when (not (empty? level))
    (or (first (filter pred level))
        (recursive-hierarkia-v4-search pred (mapcat :children level)))))

(defn oppilaitos?
  [organisaatio]
  (not (empty? (filter #(= "organisaatiotyyppi_02" %) (:organisaatiotyypit organisaatio)))))

(defn find-oppilaitos-from-hierarkia
  [hierarkia]
  (recursive-hierarkia-v4-search oppilaitos? (:organisaatiot hierarkia)))