(ns kouta-indeksoija-service.indexer.kouta.oppilaitos
  (:require [clojure.set :refer [rename-keys]]
            [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
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

(defn- oppilaitos-entry
  [organisaatio oppilaitos]
  (cond-> (organisaatio-entry organisaatio)
          (seq oppilaitos) (assoc :oppilaitos (-> oppilaitos
                                                  (common/complete-entry)
                                                  (dissoc :oid)))))

(defn- oppilaitoksen-osa-entry
  [organisaatio oppilaitoksen-osa]
  (cond-> (organisaatio-entry organisaatio)
          (seq oppilaitoksen-osa) (assoc :oppilaitoksenOsa (-> oppilaitoksen-osa
                                                               (common/complete-entry)
                                                               (dissoc :oppilaitosOid :oid)))))

(defn create-index-entry
  [oid]
  (let [hierarkia (cache/get-hierarkia oid)]
    (when-let [organisaatio (organisaatio-tool/find-oppilaitos-from-hierarkia hierarkia)]
      (let [oppilaitos-oid (:oid organisaatio)
            oppilaitos (or (kouta-backend/get-oppilaitos oppilaitos-oid) {})
            oppilaitoksen-osat (kouta-backend/get-oppilaitoksen-osat oppilaitos-oid)]

        (defn- osa
          [child]
          (or (first (filter #(= (:oid %) (:oid child)) oppilaitoksen-osat)) {}))

        (let [oppilaitoksen-osa-entries (vec (map #(oppilaitoksen-osa-entry % (osa %)) (:children organisaatio)))]
          (assoc (oppilaitos-entry organisaatio oppilaitos) :osat oppilaitoksen-osa-entries))))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get
  [oid]
  (indexable/get index-name oid))