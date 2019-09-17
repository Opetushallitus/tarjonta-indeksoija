(ns kouta-indeksoija-service.indexer.kouta.toteutus
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.notifier.notifier :refer [send-toteutus-notifications]]))

(def index-name "toteutus-kouta")

(defn create-index-entry
  [oid]
  (let [toteutus (common/complete-entry (kouta-backend/get-toteutus oid))
        hakukohde-list (common/complete-entries (kouta-backend/list-hakukohteet-by-toteutus oid))]
    (-> toteutus
        (common/assoc-organisaatiot)
        (assoc :hakukohteet hakukohde-list))))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (send-toteutus-notifications (indexable/do-index index-name oids create-index-entries)))

(defn get
  [oid]
  (indexable/get index-name oid))
