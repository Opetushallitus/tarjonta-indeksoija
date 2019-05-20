(ns kouta-indeksoija-service.kouta.haku
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.kouta.common :as common]
            [kouta-indeksoija-service.kouta.indexable :as indexable]))

(def index-name "haku-kouta")

(defn create-index-entry
  [oid]
  (let [haku (common/complete-entry (kouta-backend/get-haku oid))
        hakukohde-list (common/complete-entries (kouta-backend/list-hakukohteet-by-haku oid))]
    (assoc haku :hakukohteet hakukohde-list)))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entries))

(defn get
  [oid]
  (indexable/get index-name oid))