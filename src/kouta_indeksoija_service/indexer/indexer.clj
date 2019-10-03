(ns kouta-indeksoija-service.indexer.indexer
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.koulutus :as koulutus]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus-search]
            [kouta-indeksoija-service.indexer.kouta.toteutus :as toteutus]
            [kouta-indeksoija-service.indexer.kouta.haku :as haku]
            [kouta-indeksoija-service.indexer.kouta.hakukohde :as hakukohde]
            [kouta-indeksoija-service.indexer.kouta.valintaperuste :as valintaperuste]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos :as oppilaitos]
            [kouta-indeksoija-service.indexer.eperuste.eperuste :as eperuste]
            [kouta-indeksoija-service.indexer.eperuste.osaamisalakuvaus :as osaamisalakuvaus]
            [kouta-indeksoija-service.util.time :refer [long->rfc1123]]
            [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.eperuste :as eperusteet-client]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [clojure.tools.logging :as log]))

(defn- get-oids
  [key coll]
  (set (map key coll)))

(defn- index-koulutukset
  [oids]
  (koulutus-search/do-index oids)
  (koulutus/do-index oids))

(defn- index-toteutukset
  [oids]
  (let [entries (toteutus/do-index oids)
        haut (set (apply concat (map kouta-backend/list-haut-by-toteutus oids)))]
    (index-koulutukset (get-oids :koulutusOid entries))
    (haku/do-index (get-oids :oid haut))
    entries))

(defn- index-haut
  [oids]
  (let [koulutukset (set (apply concat (map kouta-backend/list-koulutukset-by-haku oids)))]
    (koulutus-search/do-index (get-oids :oid koulutukset)))
  (haku/do-index oids))

(defn- index-hakukohteet
  [oids]
  (let [hakukohde-entries (hakukohde/do-index oids)
        haku-oids (get-oids :hakuOid hakukohde-entries)
        koulutukset (set (apply concat (map kouta-backend/list-koulutukset-by-haku haku-oids)))]
    (haku/do-index haku-oids)
    (toteutus/do-index (get-oids :toteutusOid hakukohde-entries))
    (koulutus-search/do-index (get-oids :oid koulutukset))
    hakukohde-entries))

(defn- index-valintaperusteet
  [oids]
  (let [entries (valintaperuste/do-index oids)
        hakukohteet (apply concat (map kouta-backend/list-hakukohteet-by-valintaperuste (get-oids :id entries)))]
    (hakukohde/do-index (get-oids :oid hakukohteet))
    entries))

(defn- index-sorakuvaukset
  [oids]
  (let [valintaperusteet (apply concat (map kouta-backend/list-valintaperusteet-by-sorakuvaus oids))]
    (index-valintaperusteet (get-oids :id valintaperusteet)))
  oids)

(defn- index-eperusteet
  [oids]
  (osaamisalakuvaus/do-index oids)
  (eperuste/do-index oids))

(defn- index-oppilaitokset
  [oids]
  (oppilaitos/do-index oids))

(defn rewrite-non-empty
  [map keyword f]
  (if (contains? map keyword)
    (assoc map keyword (f (keyword map)))
    map))

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
              (count (:oppilaitokset oids)) "oppilaitosta.")
    (let [ret (-> oids
                  (rewrite-non-empty :koulutukset index-koulutukset)
                  (rewrite-non-empty :toteutukset index-toteutukset)
                  (rewrite-non-empty :haut index-haut)
                  (rewrite-non-empty :hakukohteet index-hakukohteet)
                  (rewrite-non-empty :valintaperusteet index-valintaperusteet)
                  (rewrite-non-empty :sorakuvaukset index-sorakuvaukset)
                  (rewrite-non-empty :eperusteet index-eperusteet)
                  (rewrite-non-empty :oppilaitokset index-oppilaitokset))]
      (log/info (str "Indeksointi valmis. Aikaa kului " (- (. System (currentTimeMillis)) start) " ms"))
      ret)))

(defn index-all-kouta
  [oids]
  (log/info (str "Indeksoidaan kouta-backendistä kaikki"))
  (let [start (. System (currentTimeMillis))]
    (koulutus/do-index (:koulutukset oids))
    (koulutus-search/do-index (:koulutukset oids))
    (toteutus/do-index (:toteutukset oids))
    (haku/do-index (:haut oids))
    (hakukohde/do-index (:hakukohteet oids))
    (valintaperuste/do-index (:valintaperusteet oids))
    (log/info (str "Indeksointi valmis ja oidien haku valmis. Aikaa kului " (- (. System (currentTimeMillis)) start) " ms"))))
