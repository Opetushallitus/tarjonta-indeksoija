(ns kouta-indeksoija-service.indexer.kouta.haku
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "haku-kouta")

(defn create-index-entry
  [oid]
  (let [haku           (common/complete-entry (kouta-backend/get-haku oid))
        hakukohde-list (common/complete-entries (kouta-backend/list-hakukohteet-by-haku oid))
        toteutus-list  (common/complete-entries (kouta-backend/list-toteutukset-by-haku oid))
        assoc-toteutus (fn [h] (assoc h :toteutus (common/assoc-organisaatiot (first (filter #(= (:oid %) (:toteutusOid h)) toteutus-list)))))]
    (indexable/->index-entry oid (-> haku
                                     (assoc :hakukohteet (vec (map assoc-toteutus hakukohde-list)))
                                     (conj (common/create-hakulomake-linkki haku (:oid haku)))))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get
  [oid]
  (indexable/get index-name oid))
