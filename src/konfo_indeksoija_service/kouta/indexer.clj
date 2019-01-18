(ns konfo-indeksoija-service.kouta.indexer
  (:require [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [konfo-indeksoija-service.kouta.koulutus :as koulutus]
            [konfo-indeksoija-service.kouta.toteutus :as toteutus]
            [konfo-indeksoija-service.kouta.haku :as haku]
            [konfo-indeksoija-service.kouta.hakukohde :as hakukohde]
            [konfo-indeksoija-service.util.time :refer [format-long-to-rfc1123]]
            [konfo-indeksoija-service.elastic.docs :as docs]
            [clojure.tools.logging :as log]))

(defn index-koulutukset
  [oids]
  (if (> (count oids) 0)
    (let [koulutukset (doall (pmap koulutus/create-index-entry oids))]
      (docs/upsert-docs "koulutus-kouta" koulutukset))))

(defn index-koulutus
  [oid]
  (index-koulutukset [oid]))

(defn index-toteutukset
  [oids]
  (if (> (count oids) 0)
    (let [toteutukset (doall (pmap toteutus/create-index-entry oids))]
      (docs/upsert-docs "toteutus-kouta" toteutukset))))

(defn index-toteutus
  [oid]
  (index-toteutukset [oid]))

(defn index-haut
  [oids]
  (if (> (count oids) 0)
    (let [haut (doall (pmap haku/create-index-entry oids))]
      (docs/upsert-docs "haku-kouta" haut))))

(defn index-hakukohteet
  [oids]
  (if (> (count oids) 0)
    (let [hakukohteet (doall (pmap hakukohde/create-index-entry oids))]
      (docs/upsert-docs "hakukohde-kouta" hakukohteet))))



(defn index-all
  [since]
  (log/info (str "Indeksoidaan kouta-backendistä " (format-long-to-rfc1123 since) " jälkeen muuttuneet"))
  (let [start (. System (nanoTime))
        date (format-long-to-rfc1123 since)
        oids (kouta-backend/get-last-modified date)]
    (log/info (str "Indeksoidaan " (count (:koulutukset oids)) " koulutusta"))
    (index-koulutukset (:koulutukset oids))
    (log/info (str "Indeksoidaan " (count (:toteutukset oids)) " toteutusta"))
    (index-toteutukset (:toteutukset oids))
    (log/info (str "Indeksoidaan " (count (:haut oids)) " hakua"))
    (index-haut (:haut oids))
    (log/info (str "Indeksoidaan " (count (:hakukohteet oids)) " hakukohdetta"))
    (index-hakukohteet (:hakukohteet oids))
    (log/info (str "Indeksointi valmis. Aikaa kului " (/ (double (- (. System (nanoTime)) start)) 1000000.0) " ms"))))



