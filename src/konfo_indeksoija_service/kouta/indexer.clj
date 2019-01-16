(ns konfo-indeksoija-service.kouta.indexer
  (:require [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [konfo-indeksoija-service.kouta.koulutus :as koulutus]
            [konfo-indeksoija-service.kouta.toteutus :as toteutus]
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

(defn index-all
  [since]
  (log/info (str "Indeksoidaan kouta-backendistä " (format-long-to-rfc1123 since) " jälkeen muuttuneet"))
  (let [date (format-long-to-rfc1123 since)
        oids (kouta-backend/get-last-modified date)]
    (log/info (str "Indeksoidaan " (count (:koulutukset oids)) " koulutusta"))
    (index-koulutukset (:koulutukset oids))
    (log/info (str "Indeksoidaan " (count (:toteutukset oids)) " toteutusta"))
    (index-toteutukset (:toteutukset oids))))



