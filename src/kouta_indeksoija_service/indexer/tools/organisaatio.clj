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

(defn- recursive-hierarkia-v4-get
  [keys level]
  (when (not (empty? level))
    (concat (map #(select-keys % keys) level)
            (recursive-hierarkia-v4-get keys (mapcat :children level)))))

(defn get-organisaatio-keys-flat
  [hierarkia keys]
  (recursive-hierarkia-v4-get keys (:organisaatiot hierarkia)))

(defn find-from-organisaatio-and-children
  [organisaatio oid]
  (recursive-hierarkia-v4-search #(= (:oid %) oid) (vector organisaatio)))