(ns kouta-indeksoija-service.indexer.kouta.koulutus
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.toteutus :refer [to-list-item]]
            [kouta-indeksoija-service.notifier.notifier :refer [send-koulutus-notifications]]))

(def index-name "koulutus-kouta")

(defn create-index-entry
  [oid]
  (let [koulutus (common/complete-entry (kouta-backend/get-koulutus oid))
        toteutukset (common/complete-entries (kouta-backend/get-toteutus-list-for-koulutus oid))]
    (-> koulutus
        (common/assoc-organisaatiot)
        (assoc :toteutukset (map to-list-item toteutukset)))))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (send-koulutus-notifications (indexable/do-index index-name oids create-index-entries)))

(defn get
  [oid]
  (indexable/get index-name oid))
