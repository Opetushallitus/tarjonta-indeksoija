(ns kouta-indeksoija-service.indexer.kouta.hakukohde
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.toteutus :refer [to-list-item]]
            [kouta-indeksoija-service.notifier.notifier :refer [send-hakukohde-notification]]))

(def index-name "hakukohde-kouta")

(defn- assoc-valintaperuste
  [hakukohde]
  (cond-> (dissoc hakukohde :valintaperusteId)
          (some? (:valintaperusteId hakukohde)) (assoc :valintaperuste (-> hakukohde
                                                                           :valintaperusteId
                                                                           (kouta-backend/get-valintaperuste)
                                                                           (dissoc :metadata)
                                                                           (common/complete-entry)))))

(defn- assoc-toteutus
  [hakukohde]
  (assoc hakukohde :toteutus (-> hakukohde
                                 :toteutusOid
                                 (kouta-backend/get-toteutus)
                                 (common/complete-entry)
                                 (to-list-item))))

(defn create-index-entry
  [oid]
  (-> oid
      (kouta-backend/get-hakukohde)
      (common/complete-entry)
      (assoc-toteutus)
      (assoc-valintaperuste)))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (send-hakukohde-notification (indexable/do-index index-name oids create-index-entries)) )

(defn get
  [oid]
  (indexable/get index-name oid))
