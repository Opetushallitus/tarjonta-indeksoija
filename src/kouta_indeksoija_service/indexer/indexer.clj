(ns kouta-indeksoija-service.indexer.indexer
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.koulutus :as koulutus]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus-search]
            [kouta-indeksoija-service.indexer.kouta.toteutus :as toteutus]
            [kouta-indeksoija-service.indexer.kouta.haku :as haku]
            [kouta-indeksoija-service.indexer.kouta.hakukohde :as hakukohde]
            [kouta-indeksoija-service.indexer.kouta.valintaperuste :as valintaperuste]
            [kouta-indeksoija-service.indexer.eperuste.eperuste :as eperuste]
            [kouta-indeksoija-service.indexer.eperuste.osaamisalakuvaus :as osaamisalakuvaus]
            [kouta-indeksoija-service.indexer.organisaatio.organisaatio :as organisaatio]
            [kouta-indeksoija-service.util.time :refer [long->rfc1123]]
            [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.eperuste :as eperusteet-client]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
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
  (haku/do-index oids)
  (let [koulutukset (set (apply concat (map kouta-backend/list-koulutukset-by-haku oids)))]
    (koulutus-search/do-index (get-oids :oid koulutukset))))

(defn index-haku
  [oid]
  (index-haut [oid]))

(defn index-hakukohteet
  [oids]
  (let [hakukohde-entries (hakukohde/do-index oids)
        haku-oids (get-oids :hakuOid hakukohde-entries)
        koulutukset (set (apply concat (map kouta-backend/list-koulutukset-by-haku haku-oids)))]
    (haku/do-index haku-oids)
    (toteutus/do-index (get-oids :toteutusOid hakukohde-entries))
    (koulutus-search/do-index (get-oids :oid koulutukset))))

(defn index-hakukohde
  [oid]
  (index-hakukohteet [oid]))

(defn index-valintaperusteet
  [oids]
  (let [entries (valintaperuste/do-index oids)
        hakukohteet (apply concat (map kouta-backend/list-hakukohteet-by-valintaperuste (get-oids :id entries)))]
     (hakukohde/do-index (get-oids :oid hakukohteet))))

(defn index-valintaperuste
  [oid]
  (index-valintaperusteet [oid]))

(defn index-sorakuvaukset
  [oids]
  (let [valintaperusteet (apply concat (map kouta-backend/list-valintaperusteet-by-sorakuvaus oids))]
    (index-valintaperusteet (get-oids :id valintaperusteet))))

(defn index-sorakuvaus
  [oid]
  (index-sorakuvaukset [oid]))

(defn index-eperusteet
  [oids]
  (eperuste/do-index oids)
  (osaamisalakuvaus/do-index oids))

(defn index-eperuste
  [oid]
  (index-eperusteet [oid]))

(defn index-organisaatiot
  [oids]
  (organisaatio/do-index oids))

(defn index-organisaatio
  [oid]
  (index-organisaatiot [oid]))

(defn index-oids
  [oids]
  (let [start (. System (currentTimeMillis))]
    (log/info "Indeksoidaan: "
              (count (:koulutukset oids)) "koulutusta, "
              (count (:toteutukset oids)) "toteutusta, "
              (count (:haut oids)) "hakua, "
              (count (:hakukohteet oids)) "hakukohdetta, "
              (count (:valintaperusteet oids)) "valintaperustetta, "
              (count (:sorakuvaukset oids)) "sora-kuvausta, "
              (count (:eperusteet oids)) "eperustetta osaamisaloineen sekä"
              (count (:organisaatiot oids)) "organisaatiota.")
    (index-koulutukset (:koulutukset oids))
    (index-toteutukset (:toteutukset oids))
    (index-haut (:haut oids))
    (index-hakukohteet (:hakukohteet oids))
    (index-sorakuvaukset (:sorakuvaukset oids))
    (index-valintaperusteet (:valintaperusteet oids))
    (index-eperusteet (:eperusteet oids))
    (index-organisaatiot (:organisaatiot oids))
    (log/info (str "Indeksointi valmis. Aikaa kului " (- (. System (currentTimeMillis)) start) " ms"))))

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
  (log/info (str "Indeksoidaan kouta-backendistä kaikki"))
  (let [start (. System (currentTimeMillis))
        oids (all-kouta-oids)]
    (koulutus/do-index (:koulutukset oids))
    (koulutus-search/do-index (:koulutukset oids))
    (toteutus/do-index (:toteutukset oids))
    (haku/do-index (:haut oids))
    (hakukohde/do-index (:hakukohteet oids))
    (valintaperuste/do-index (:valintaperusteet oids))
    (log/info (str "Indeksointi valmis ja oidien haku valmis. Aikaa kului " (- (. System (currentTimeMillis)) start) " ms"))))

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

(defn index-all-organisaatiot
  []
  (index-organisaatiot (:organisaatiot (organisaatio-client/find-docs nil))))