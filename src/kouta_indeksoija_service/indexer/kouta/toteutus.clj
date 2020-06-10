(ns kouta-indeksoija-service.indexer.kouta.toteutus
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [clojure.string :refer [blank?]]))

(def index-name "toteutus-kouta")

(defn- determine-correct-aikataulu-and-hakulomake
  [ht-haku ht-hakukohde]
  (let [hakulomakeKeys  [:hakulomaketyyppi :hakulomakeAtaruId :hakulomakeKuvaus :hakulomakeLinkki]
        alkamisaikaKeys [:alkamiskausiKoodiUri :alkamisvuosi]
        aikatauluKeys   [:hakuajat]]
    (merge {}
           (if (true? (:kaytetaanHaunHakulomaketta ht-hakukohde))
             (select-keys ht-haku hakulomakeKeys)
             (select-keys ht-hakukohde hakulomakeKeys))
           (if (true? (:kaytetaanHaunAlkamiskautta ht-hakukohde))
             (select-keys ht-haku alkamisaikaKeys)
             (select-keys ht-hakukohde alkamisaikaKeys))
           (if (true? (:kaytetaanHaunAikataulua ht-hakukohde))
             (select-keys ht-haku aikatauluKeys)
             (select-keys ht-hakukohde aikatauluKeys)))))

(defn- create-hakukohteiden-hakutiedot
  [ht-haku]
  (for [ht-hakukohde (:hakukohteet ht-haku)]
    (-> (select-keys ht-hakukohde [:hakukohdeOid
                                   :nimi
                                   :valintaperusteId
                                   :pohjakoulutusvaatimusKoodiUrit
                                   :pohjakoulutusvaatimusTarkenne
                                   :aloituspaikat
                                   :ensikertalaisenAloituspaikat])
        (merge (determine-correct-aikataulu-and-hakulomake ht-haku ht-hakukohde)))))

(defn- determine-correct-hakutiedot
  [ht-toteutus]
  (-> (for [ht-haku (:haut ht-toteutus)]
        (-> (select-keys ht-haku [:hakuOid :nimi :hakutapaKoodiUri])
            (assoc :hakukohteet (vec (create-hakukohteiden-hakutiedot ht-haku)))))
      (vec)
      (common/decorate-koodi-uris)))

(defn- assoc-hakutiedot
  [toteutus hakutiedot]
  (if-let [ht-toteutus (first (filter (fn [x] (= (:toteutusOid x) (:oid toteutus))) hakutiedot))]
    (assoc toteutus :hakutiedot (determine-correct-hakutiedot ht-toteutus))
    toteutus))

(defn create-index-entry
  [oid]
  (let [toteutus (common/complete-entry (kouta-backend/get-toteutus oid))
        hakutiedot (kouta-backend/get-hakutiedot-for-koulutus (:koulutusOid toteutus))]
    (indexable/->index-entry oid (-> toteutus
                                     (common/assoc-organisaatiot)
                                     (assoc-hakutiedot hakutiedot)))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get
  [oid]
  (indexable/get index-name oid))