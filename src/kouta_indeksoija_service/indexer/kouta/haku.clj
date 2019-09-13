(ns kouta-indeksoija-service.indexer.kouta.haku
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.notifier.notifier :refer [send-haku-notification]]))

(def index-name "haku-kouta")

(defn create-index-entry
  [oid]
  (let [haku (common/complete-entry (kouta-backend/get-haku oid))
        hakukohde-list (common/complete-entries (kouta-backend/list-hakukohteet-by-haku oid))
        toteutus-list (common/complete-entries (kouta-backend/list-toteutukset-by-haku oid))]
    (assoc haku :hakukohteet (vec (map (fn [h] (assoc h :toteutus (common/assoc-organisaatiot (first (filter #(= (:oid %) (:toteutusOid h)) toteutus-list))))) hakukohde-list)))))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (send-haku-notification (indexable/do-index index-name oids create-index-entries)) )

(defn get
  [oid]
  (indexable/get index-name oid))
