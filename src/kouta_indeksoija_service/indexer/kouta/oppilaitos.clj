(ns kouta-indeksoija-service.indexer.kouta.oppilaitos
  (:require [clojure.set :refer [rename-keys]]
            [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as search]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "oppilaitos-kouta")

(defn- organisaatio-entry
  [organisaatio]
  (-> organisaatio
      (select-keys [:nimi :kieletUris :kotipaikkaUri :status :organisaatiotyypit :oppilaitostyyppi :oid])
      (rename-keys {:kieletUris :opetuskieliKoodiUrit
                    :oppilaitostyyppi :oppilaitostyyppiKoodiUri
                    :kotipaikkaUri :kotipaikkaKoodiUri
                    :organisaatiotyypit :organisaatiotyyppiKoodiUrit})
      (common/complete-entry)))

(defn- assoc-koulutusohjelmia
  [organisaatio]
  (->> (kouta-backend/get-koulutukset-by-tarjoaja (:oid organisaatio))
       (search/get-tarjoaja-entries (cache/get-hierarkia (:oid organisaatio)))
       (filter :johtaaTutkintoon)
       (count)
       (assoc organisaatio :koulutusohjelmia)))

(defn- oppilaitos-entry
  [organisaatio oppilaitos]
  (cond-> (assoc-koulutusohjelmia (organisaatio-entry organisaatio))
          (seq oppilaitos) (assoc :oppilaitos (-> oppilaitos
                                                  (common/complete-entry)
                                                  (dissoc :oid)))))

(defn- oppilaitoksen-osa-entry
  [organisaatio oppilaitoksen-osa]
  (cond-> (assoc-koulutusohjelmia (organisaatio-entry organisaatio))
          (seq oppilaitoksen-osa) (assoc :oppilaitoksenOsa (-> oppilaitoksen-osa
                                                               (common/complete-entry)
                                                               (dissoc :oppilaitosOid :oid)))))

(defn- oppilaitos-entry-with-osat
  [organisaatio]
  (let [oppilaitos-oid (:oid organisaatio)
        oppilaitos (or (kouta-backend/get-oppilaitos oppilaitos-oid) {})
        oppilaitoksen-osat (kouta-backend/get-oppilaitoksen-osat oppilaitos-oid)
        find-oppilaitoksen-osa (fn [child] (or (first (filter #(= (:oid %) (:oid child)) oppilaitoksen-osat)) {}))]

    (-> (oppilaitos-entry organisaatio oppilaitos)
        (assoc :osat (->> (organisaatio-tool/get-indexable-children organisaatio)
                          (map #(oppilaitoksen-osa-entry % (find-oppilaitoksen-osa %)))
                          (vec))))))

(defn create-index-entry
  [oid]
  (let [hierarkia (cache/get-hierarkia oid)]
    (when-let [oppilaitos (organisaatio-tool/find-oppilaitos-from-hierarkia hierarkia)]
      (if (organisaatio-tool/indexable? oppilaitos)
        (indexable/->index-entry (:oid oppilaitos) (oppilaitos-entry-with-osat oppilaitos))
        (indexable/->delete-entry (:oid oppilaitos))))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get
  [oid & query-params]
  (apply indexable/get index-name oid query-params))
