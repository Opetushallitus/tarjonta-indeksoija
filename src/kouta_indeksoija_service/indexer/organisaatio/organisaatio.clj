(ns kouta-indeksoija-service.indexer.organisaatio.organisaatio
  (:require [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.rest.koodisto :as koodisto-client]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [oppilaitostyyppi-uri-to-tyyppi]]
            [kouta-indeksoija-service.indexer.organisaatio.pictures :refer [store-pictures]]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [clojure.string :as str]))

(def index-name "organisaatio")

;TODO -> Tämä on vielä vanhan mallinen organisaation indeksointi ennen koutaa

(defn- recursive-find-oppilaitostyyppi [organisaatio]
  (if (nil? organisaatio)
    nil
    (if-let [children (not-empty (:children organisaatio))]
      (if-let [type-from-child (recursive-find-oppilaitostyyppi (first children))]
        type-from-child
        (:oppilaitostyyppi organisaatio))
      (:oppilaitostyyppi organisaatio))))

(defn- find-parent-oppilaitos-tyyppi-uri [oid]
  (let [hierarkia (organisaatio-client/get-tyyppi-hierarkia oid)]
    (if-let [organisaatiot (not-empty (:organisaatiot hierarkia))]
      (recursive-find-oppilaitostyyppi (first organisaatiot))
      nil)))

(defn- find-oppilaitos-tyyppi-uri [organisaatio]
  (if-let [oppilaitosTyyppiUri (:oppilaitosTyyppiUri organisaatio)]
    oppilaitosTyyppiUri
    (find-parent-oppilaitos-tyyppi-uri (:oid organisaatio))))

(defn- find-oppilaitos-tyyppi-nimi [oppilaitostyyppi-uri]
  (if (not (nil? oppilaitostyyppi-uri))
    (let [koodi-uri (first (str/split oppilaitostyyppi-uri #"#"))
          koodisto (koodisto-client/get-koodi-with-cache "oppilaitostyyppi" koodi-uri)
          metadata (:metadata koodisto)]

      (into {} (map (fn [x] {(keyword (str/lower-case (:kieli x))) (:nimi x)}) metadata)))))

(defn append-search-data
  [organisaatio]
  (let [oppilaitostyyppiUri (find-oppilaitos-tyyppi-uri organisaatio)
        oppilaitostyyppiNimi (find-oppilaitos-tyyppi-nimi oppilaitostyyppiUri)
        tyyppi (oppilaitostyyppi-uri-to-tyyppi oppilaitostyyppiUri)]
    (let [searchData (-> {}
                         (cond-> tyyppi (assoc :tyyppi tyyppi))
                         (cond-> oppilaitostyyppiUri (assoc :oppilaitostyyppi {:koodiUri oppilaitostyyppiUri :nimi oppilaitostyyppiNimi})))]
      (assoc organisaatio :searchData searchData))))

(defn create-index-entry
  [oid]
  (-> (organisaatio-client/get-doc {:oid oid}) append-search-data))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (let [indexed-docs (indexable/do-index index-name oids create-index-entries)]
    (store-pictures oids)
    indexed-docs))

(defn get
  [oid]
  (indexable/get index-name oid))