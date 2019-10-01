(ns kouta-indeksoija-service.indexer.indexer-api
  (:require [clojure.tools.logging :as log]
            [kouta-indeksoija-service.indexer.indexer :as indexer]
            [kouta-indeksoija-service.notifier.notifier :as notifier]
            [kouta-indeksoija-service.rest.eperuste :as eperusteet-client]
            [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.util.time :refer [long->rfc1123]]))

(defn index-oids
  [oids]
  (notifier/notify (indexer/index-oids oids)))

(defn index-koulutukset
  [oids]
  (index-oids {:koulutukset oids}))

(defn index-toteutukset
  [oids]
  (index-oids {:toteutukset oids}))

(defn index-haut
  [oids]
  (index-oids {:haut oids}))

(defn index-hakukohteet
  [oids]
  (index-oids {:hakukohteet oids}))

(defn index-valintaperusteet
  [oids]
  (index-oids {:valintaperusteet oids}))

(defn index-sorakuvaukset
  [oids]
  (index-oids {:sorakuvaukset oids}))

(defn index-eperusteet
  [oids]
  (index-oids {:eperusteet oids}))

(defn index-oppilaitokset
  [oids]
  (index-oids {:oppilaitokset oids}))

(defn index-koulutus
  [oid]
  (index-koulutukset (list oid)))

(defn index-toteutus
  [oid]
  (index-toteutukset (list oid)))

(defn index-haku
  [oid]
  (index-haut (list oid)))

(defn index-hakukohde
  [oid]
  (index-hakukohteet (list oid)))

(defn index-valintaperuste
  [oid]
  (index-valintaperusteet (list oid)))

(defn index-sorakuvaus
  [oid]
  (index-sorakuvaukset (list oid)))

(defn index-eperuste
  [oid]
  (index-eperusteet (list oid)))

(defn index-oppilaitos
  [oid]
  (index-oppilaitokset (list oid)))

(defn index-since-kouta
  [since]
  (log/info (str "Indeksoidaan kouta-backendistä " (long->rfc1123 since) " jälkeen muuttuneet"))
  (let [start (. System (currentTimeMillis))
        date (long->rfc1123 since)
        oids (kouta-backend/get-last-modified date)]
    (index-oids oids)
    (log/info (str "Indeksointi valmis ja oidien haku valmis. Aikaa kului " (- (. System (currentTimeMillis)) start) " ms"))))

(defn- all-kouta-oids
  []
  (kouta-backend/get-last-modified (long->rfc1123 0)))

(defn index-all-kouta
  []
  (indexer/index-all-kouta (all-kouta-oids)))

(defn index-all-koulutukset
  []
  (index-koulutukset (:koulutukset (all-kouta-oids))))

(defn index-all-toteutukset
  []
  (index-toteutukset (:toteutukset (all-kouta-oids))))

(defn index-all-haut
  []
  (index-haut (:haut (all-kouta-oids))))

(defn index-all-hakukohteet
  []
  (index-hakukohteet (:hakukohteet (all-kouta-oids))))

(defn index-all-valintaperusteet
  []
  (index-valintaperusteet (:valintaperusteet (all-kouta-oids))))

(defn index-all-eperusteet
  []
  (index-eperusteet (:eperusteet (eperusteet-client/find-all))))

(defn index-all-oppilaitokset
  []
  (index-oppilaitokset (:oppilaitokset (organisaatio-client/get-all-oppilaitos-oids))))
