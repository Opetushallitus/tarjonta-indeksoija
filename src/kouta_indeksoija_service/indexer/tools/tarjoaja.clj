(ns kouta-indeksoija-service.indexer.tools.tarjoaja
  (:require [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.util.tools :refer [->distinct-vec]]))

(defn get-tarjoaja-oids-for-organisaatio
  [hierarkia]
  (organisaatio-tool/get-all-oids-flat hierarkia))

(defn tarjoajia?
  [entry]
  (not (empty? (:tarjoajat entry))))

(defn remove-other-tarjoajat-from-entry-by-oids
  [oids entry]
  (assoc entry :tarjoajat (vec (clojure.set/intersection (set (:tarjoajat entry)) (set oids)))))

(defn remove-other-tarjoajat-from-entry
  [hierarkia entry]
  (-> (get-tarjoaja-oids-for-organisaatio hierarkia)
      (remove-other-tarjoajat-from-entry-by-oids entry)))

(defn get-tarjoaja-entries-by-oids
  [oids entries]
  (seq (filter tarjoajia? (map #(remove-other-tarjoajat-from-entry-by-oids oids %) entries))))

(defn get-tarjoaja-entries
  [hierarkia entries]
  (-> (get-tarjoaja-oids-for-organisaatio hierarkia)
      (get-tarjoaja-entries-by-oids entries)))

(defn get-tarjoajien-sijainnit
  [hierarkia entry]
  (->> (:tarjoajat entry)
       (organisaatio-tool/find-from-hierarkia hierarkia)
       (map :kotipaikkaKoodiUri)
       (->distinct-vec)))