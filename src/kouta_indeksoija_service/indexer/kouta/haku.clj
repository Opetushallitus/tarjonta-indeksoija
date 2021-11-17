(ns kouta-indeksoija-service.indexer.kouta.haku
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.general :as general]))

(def index-name "haku-kouta")

(defn create-index-entry
  [oid]
  (let [haku           (common/complete-entry (kouta-backend/get-haku oid))
        hakukohde-list (common/complete-entries (map #(-> %
                                                          (general/set-hakukohde-tila-by-related-haku haku)
                                                         (koodisto/assoc-hakukohde-nimi-from-koodi) )
                                                     (kouta-backend/list-hakukohteet-by-haku oid)))
        toteutus-list  (common/complete-entries (kouta-backend/list-toteutukset-by-haku oid))
        assoc-toteutus (fn [h] (assoc h :toteutus (common/assoc-organisaatiot (first (filter #(= (:oid %) (:toteutusOid h)) toteutus-list)))))]
    (indexable/->index-entry oid (-> haku
                                     (assoc :hakukohteet (vec (map assoc-toteutus hakukohde-list)))
                                     (conj (common/create-hakulomake-linkki-for-haku haku (:oid haku)))))))

(defn do-index
  [oids execution-id]
  (indexable/do-index index-name oids create-index-entry execution-id))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
