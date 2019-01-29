(ns konfo-indeksoija-service.kouta.indexer
  (:require [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [konfo-indeksoija-service.kouta.koulutus :as koulutus]
            [konfo-indeksoija-service.kouta.koulutus-search :as koulutus-search]
            [konfo-indeksoija-service.kouta.toteutus :as toteutus]
            [konfo-indeksoija-service.kouta.haku :as haku]
            [konfo-indeksoija-service.kouta.hakukohde :as hakukohde]
            [konfo-indeksoija-service.kouta.valintaperuste :as valintaperuste]
            [konfo-indeksoija-service.util.time :refer [format-long-to-rfc1123]]
            [konfo-indeksoija-service.elastic.docs :as docs]
            [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [clojure.tools.logging :as log]))

(defn- get-oids
  [key coll]
  (set (map key coll)))

(defn index-koulutukset
  [oids]
  (koulutus/do-index oids)
  (koulutus-search/do-index oids))

(defn index-koulutus
  [oid]
  (index-koulutukset [oid]))

(defn index-toteutukset
  [oids]
  (let [entries (toteutus/do-index oids)]
    (index-koulutukset (get-oids :koulutusOid entries))))

(defn index-toteutus
  [oid]
  (index-toteutukset [oid]))

(defn index-haut
  [oids]
  (let [entries (haku/do-index oids)]
    (let [toteutus-oids (set (map :toteutusOid (apply clojure.set/union (doall (map :hakukohteet entries)))))
          toteutus-entries (toteutus/do-index toteutus-oids)]
      (koulutus-search/do-index (get-oids :koulutusOid toteutus-entries)))))

(defn index-haku
  [oid]
  (index-haut [oid]))

(defn index-hakukohteet
  [oids]
  (let [hakukohde-entries (hakukohde/do-index oids)
        haku-oids (get-oids :hakuOid hakukohde-entries)
        koulutukset (set (apply clojure.set/union (map kouta-backend/list-koulutukset-by-haku haku-oids)))]
    (haku/do-index haku-oids)
    (koulutus-search/do-index (get-oids :oid koulutukset))))

(defn index-hakukohde
  [oid]
  (index-hakukohteet [oid]))

(defn index-valintaperusteet
  [oids]
   (let [entries (valintaperuste/do-index oids)
         hakukohteet (apply clojure.set/union (map kouta-backend/list-hakukohteet-by-valintaperuste (get-oids :id entries)))]
     (hakukohde/do-index (get-oids :oid hakukohteet))))

(defn index-valintaperuste
  [oid]
  (index-valintaperusteet [oid]))

(defn index-oids
  [oids]
  (let [start (. System (currentTimeMillis))]
    (log/info "Indeksoidaan: "
              (count (:koulutukset oids)) "koulutusta, "
              (count (:toteutukset oids)) "toteutusta, "
              (count (:haut oids)) "hakua, "
              (count (:hakukohteet oids)) "hakukohdetta ja "
              (count (:valintaperusteet oids)) "valintaperustetta")
    (index-koulutukset (:koulutukset oids))
    (index-toteutukset (:toteutukset oids))
    (index-haut (:haut oids))
    (index-hakukohteet (:hakukohteet oids))
    (index-valintaperusteet (:valintaperusteet oids))
    (log/info (str "Indeksointi valmis. Aikaa kului " (- (. System (currentTimeMillis)) start) " ms"))))

(defn index-since
  [since]
  (log/info (str "Indeksoidaan kouta-backendistä " (format-long-to-rfc1123 since) " jälkeen muuttuneet"))
  (let [start (. System (currentTimeMillis))
        date (format-long-to-rfc1123 since)
        oids (kouta-backend/get-last-modified date)]
    (index-oids oids)
    (log/info (str "Indeksointi valmis ja oidien haku valmis. Aikaa kului " (- (. System (currentTimeMillis)) start) " ms"))))

(defn index-all
  []
  (log/info (str "Indeksoidaan kouta-backendistä kaikki"))
  (let [start (. System (currentTimeMillis))
        date (format-long-to-rfc1123 0)
        oids (kouta-backend/get-last-modified date)]
    (koulutus/do-index (:koulutukset oids))
    (koulutus-search/do-index (:koulutukset oids))
    (toteutus/do-index (:toteutukset oids))
    (haku/do-index (:haut oids))
    (hakukohde/do-index (:hakukohteet oids))
    (valintaperuste/do-index (:valintaperusteet oids))
    (log/info (str "Indeksointi valmis ja oidien haku valmis. Aikaa kului " (- (. System (currentTimeMillis)) start) " ms"))))