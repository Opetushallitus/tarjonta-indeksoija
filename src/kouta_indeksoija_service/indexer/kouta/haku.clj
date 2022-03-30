(ns kouta-indeksoija-service.indexer.kouta.haku
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.general :as general]))

(def index-name "haku-kouta")

(defn create-index-entry
  [oid execution-id]
  (let [hakukohde-list-raw (kouta-backend/list-hakukohteet-by-haku-with-cache oid)
        haku (assoc (common/complete-entry (kouta-backend/get-haku oid execution-id)) :hakukohteet hakukohde-list-raw)]
    (if (general/not-poistettu? haku)
      (let [toteutus-list  (common/complete-entries (kouta-backend/list-toteutukset-by-haku-with-cache oid execution-id))
            assoc-toteutus (fn [h] (assoc h :toteutus
                                            (common/assoc-organisaatiot
                                             (first (filter #(= (:oid %) (:toteutusOid h)) toteutus-list)))))
            hakukohde-list (vec (map (fn [hk] (-> hk
                                                  (general/set-hakukohde-tila-by-related-haku haku)
                                                  (koodisto/assoc-hakukohde-nimi-from-koodi)
                                                  (common/complete-entry)
                                                  (assoc-toteutus)))
                                     (filter general/not-poistettu? hakukohde-list-raw)))]
            (indexable/->index-entry-with-forwarded-data oid (-> haku
                                       (assoc :hakukohteet hakukohde-list)
                                        (conj (common/create-hakulomake-linkki-for-haku haku (:oid haku)))
                                          (common/localize-dates)) haku))
      (indexable/->delete-entry-with-forwarded-data oid haku))))

(defn do-index
  [oids execution-id]
  (indexable/do-index index-name oids create-index-entry execution-id))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
