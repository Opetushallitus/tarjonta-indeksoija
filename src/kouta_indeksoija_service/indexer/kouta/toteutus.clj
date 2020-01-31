(ns kouta-indeksoija-service.indexer.kouta.toteutus
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [clojure.string :refer [blank?]]))

(def index-name "toteutus-kouta")

(defn- find-by-oid
  ([coll oid key]
   (first (filter #(= oid (get % key)) coll)))
  ([coll oid]
   (find-by-oid coll oid :oid)))

(defn- merge-hakutiedot-to-hakukohde
  [hakutiedot hakukohde]
  (let [haun-hakutiedot        (first (filter #(= (:hakuOid hakukohde) (:hakuOid %)) hakutiedot))
        hakukohteen-hakutiedot (first (filter #(= (:oid hakukohde) (:hakukohdeOid %)) (:hakukohteet haun-hakutiedot)))]

    (->> (-> (select-keys hakukohteen-hakutiedot [:aloituspaikat
                                                  :minAloituspaikat
                                                  :maxAloituspaikat
                                                  :ensikertalaisenAloituspaikat
                                                  :minEnsikertalaisenAloituspaikat
                                                  :maxEnsikertalaisenAloituspaikat])
             (merge (-> (true? (:kaytetaanHaunHakulomaketta hakukohteen-hakutiedot))
                        (if haun-hakutiedot hakukohteen-hakutiedot)
                        (select-keys [:hakulomaketyyppi
                                      :hakulomakeAtaruId
                                      :hakulomakeKuvaus
                                      :hakulomakeLinkki])))
             (merge (-> (true? (:kaytetaanHaunAlkamiskautta hakukohteen-hakutiedot))
                        (if haun-hakutiedot hakukohteen-hakutiedot)
                        (select-keys [:alkamiskausiKoodiUri
                                      :alkamisvuosi])))
             (merge (-> (true? (:kaytetaanHaunAikataulua hakukohteen-hakutiedot))
                        (if haun-hakutiedot hakukohteen-hakutiedot)
                        (select-keys [:hakuajat])))
             (merge (select-keys haun-hakutiedot [:hakutapaKoodiUri])))
         (remove #(-> % val nil?))
         (common/decorate-koodi-uris)
         (merge hakukohde))))

(defn- assoc-hakutiedot
  [toteutus hakutiedot]
  (if-let [toteutuksen-hakutiedot (first (filter (fn [x] (= (:toteutusOid x) (:oid toteutus))) hakutiedot))]
    (assoc toteutus :hakukohteet (vec (map #(merge-hakutiedot-to-hakukohde (:haut toteutuksen-hakutiedot) %) (:hakukohteet toteutus))))
    toteutus))

(defn create-index-entry
  [oid]
  (let [toteutus (common/complete-entry (kouta-backend/get-toteutus oid))
        hakukohde-list (common/complete-entries (kouta-backend/list-hakukohteet-by-toteutus oid))
        hakutiedot (kouta-backend/get-hakutiedot-for-koulutus (:koulutusOid toteutus))]
    (indexable/->index-entry oid (-> toteutus
                                     (common/assoc-organisaatiot)
                                     (assoc :hakukohteet hakukohde-list)
                                     (assoc-hakutiedot hakutiedot)))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get
  [oid]
  (indexable/get index-name oid))