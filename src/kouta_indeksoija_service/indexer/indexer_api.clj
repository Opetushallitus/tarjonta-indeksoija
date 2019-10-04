(ns kouta-indeksoija-service.indexer.indexer-api
  (:require [clojure.tools.logging :as log]
            [kouta-indeksoija-service.indexer.indexer :as indexer]
            [kouta-indeksoija-service.notifier.notifier :as notifier]
            [kouta-indeksoija-service.rest.eperuste :as eperusteet-client]
            [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.util.time :refer [long->rfc1123]]))

(defn index-oids
  [oids notify]
  (let [indexed (indexer/index-oids oids)]
    (if notify (notifier/notify indexed))
    indexed))

(defn index-koulutukset
  [oids notify]
  (index-oids {:koulutukset oids} notify))

(defn index-toteutukset
  [oids notify]
  (index-oids {:toteutukset oids} notify))

(defn index-haut
  [oids notify]
  (index-oids {:haut oids} notify))

(defn index-hakukohteet
  [oids notify]
  (index-oids {:hakukohteet oids} notify))

(defn index-valintaperusteet
  [oids notify]
  (index-oids {:valintaperusteet oids} notify))

(defn index-sorakuvaukset
  [oids notify]
  (index-oids {:sorakuvaukset oids} notify))

(defn index-eperusteet
  [oids]
  (index-oids {:eperusteet oids} false))

(defn index-oppilaitokset
  [oids]
  (index-oids {:oppilaitokset oids} false))

(defn index-koulutus
  [oid notify]
  (index-koulutukset [oid] notify))

(defn index-toteutus
  [oid notify]
  (index-toteutukset [oid] notify))

(defn index-haku
  [oid notify]
  (index-haut [oid] notify))

(defn index-hakukohde
  [oid notify]
  (index-hakukohteet [oid] notify))

(defn index-valintaperuste
  [oid notify]
  (index-valintaperusteet [oid] notify))

(defn index-eperuste
  [oid]
  (index-eperusteet [oid]))

(defn index-oppilaitos
  [oid]
  (index-oppilaitokset [oid]))

(defn index-since-kouta
  [since notify]
  (log/info (str "Indeksoidaan kouta-backendistä " (long->rfc1123 since) " jälkeen muuttuneet"))
  (let [start (. System (currentTimeMillis))
        date (long->rfc1123 since)
        oids (kouta-backend/get-last-modified date)]
    (index-oids oids notify)
    (log/info (str "Indeksointi valmis ja oidien haku valmis. Aikaa kului " (- (. System (currentTimeMillis)) start) " ms"))))

(defn index-all-koulutukset
  [notify]
  (index-koulutukset (:koulutukset (kouta-backend/all-kouta-oids)) notify))

(defn index-all-toteutukset
  [notify]
  (index-toteutukset (:toteutukset (kouta-backend/all-kouta-oids)) notify))

(defn index-all-haut
  [notify]
  (index-haut (:haut (kouta-backend/all-kouta-oids)) notify))

(defn index-all-hakukohteet
  [notify]
  (index-hakukohteet (:hakukohteet (kouta-backend/all-kouta-oids)) notify))

(defn index-all-valintaperusteet
  [notify]
  (index-valintaperusteet (:valintaperusteet (kouta-backend/all-kouta-oids)) notify))

(defn index-all-eperusteet
  []
  (index-eperusteet (:eperusteet (eperusteet-client/find-all))))

(defn index-all-oppilaitokset
  []
  (index-oppilaitokset (:oppilaitokset (organisaatio-client/get-all-oppilaitos-oids))))
