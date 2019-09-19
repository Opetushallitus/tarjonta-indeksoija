(ns kouta-indeksoija-service.indexer.kouta.oppilaitos
  (:require [clojure.set :refer [rename-keys]]
            [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "oppilaitos-kouta")

(defn- merge-organisaatio-and-kouta
  [organisaatio oppilaitos-tai-osa]
  (-> organisaatio
      (select-keys [:nimi :kieletUris :kotipaikkaUri :status :organisaatiotyypit :oppilaitostyyppi :oid])
      (rename-keys {:kieletUris :opetuskieliKoodiUrit
                    :oppilaitostyyppi :oppilaitostyyppiKoodiUri
                    :kotipaikkaUri :kotipaikkaKoodiUri
                    :organisaatiotyypit :organisaatiotyyppiKoodiUrit})
      (merge oppilaitos-tai-osa)
      (dissoc :oppilaitosOid)))

(defn create-index-entry
  [oid]
  (let [hierarkia (organisaatio-client/get-hierarkia-v4 oid :aktiiviset true :suunnitellut false :lakkautetut false :skipParents false)]
    (when-let [organisaatio (organisaatio-tool/find-oppilaitos-from-hierarkia hierarkia)]
      (let [oppilaitos-oid (:oid organisaatio)
            oppilaitos (kouta-backend/get-oppilaitos oppilaitos-oid)
            oppilaitoksen-osat (kouta-backend/get-oppilaitoksen-osat oppilaitos-oid)]

        (defn- osa
          [child]
          (or (first (filter #(= (:oid %) (:oid child)) oppilaitoksen-osat)) {}))

        (-> organisaatio
            (merge-organisaatio-and-kouta oppilaitos)
            (assoc :osat (vec (map #(merge-organisaatio-and-kouta % (osa %)) (:children organisaatio))))
            (common/complete-entry))))))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entries))

(defn get
  [oid]
  (indexable/get index-name oid))