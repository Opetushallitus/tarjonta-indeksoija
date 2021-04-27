(ns kouta-indeksoija-service.indexer.indexer
  (:require [kouta-indeksoija-service.indexer.kouta.koulutus :as koulutus]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus-search]
            [kouta-indeksoija-service.indexer.kouta.toteutus :as toteutus]
            [kouta-indeksoija-service.indexer.kouta.haku :as haku]
            [kouta-indeksoija-service.indexer.kouta.hakukohde :as hakukohde]
            [kouta-indeksoija-service.indexer.kouta.valintaperuste :as valintaperuste]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos :as oppilaitos]
            [kouta-indeksoija-service.indexer.kouta.sorakuvaus :as sorakuvaus]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos-search]
            [kouta-indeksoija-service.indexer.eperuste.eperuste :as eperuste]
            [kouta-indeksoija-service.indexer.eperuste.tutkinnonosa :as tutkinnonosa]
            [kouta-indeksoija-service.indexer.eperuste.osaamisalakuvaus :as osaamisalakuvaus]
            [kouta-indeksoija-service.indexer.koodisto.koodisto :as koodisto]
            [kouta-indeksoija-service.util.time :refer [long->rfc1123]]
            [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.eperuste :as eperusteet-client]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as hierarkia]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.indexer.lokalisointi.lokalisointi :as lokalisointi]))

(defn- get-oids
  [key coll]
  (set (remove clojure.string/blank? (map key coll))))

(defn- get-ids
  [key coll]
  (set (remove nil? (map key coll))))

(defn eperuste-ids-on-koulutus
  [koulutus]
  (let [osat (get-in koulutus [:metadata :tutkinnonOsat])]
    (concat [(:ePerusteId koulutus)] (map :eperusteId osat))))

(defn eperuste-ids-on-koulutukset
  [entries]
  (set (remove nil? (mapcat eperuste-ids-on-koulutus entries))))

(defn tutkinnonosa-ids-on-koulutus
  [koulutus]
  (let [osat (get-in koulutus [:metadata :tutkinnonOsat])]
    (map :tutkinnonosatId osat)))

(defn tutkinnonosa-ids-on-koulutukset
  [entries]
  (set (remove nil? (mapcat tutkinnonosa-ids-on-koulutus entries))))

(defn index-koulutukset
  [oids]
  (let [entries (koulutus/do-index oids)]
    (koulutus-search/do-index oids)
    (eperuste/do-index (eperuste-ids-on-koulutukset entries))
    (tutkinnonosa/do-index (tutkinnonosa-ids-on-koulutukset entries))
    (oppilaitos-search/do-index (get-oids :oid (mapcat :tarjoajat entries)))
    entries))

(defn index-koulutus
  [oid]
  (index-koulutukset [oid]))

(defn index-toteutukset
  [oids]
  (let [entries (toteutus/do-index oids)
        haut    (mapcat kouta-backend/list-haut-by-toteutus oids)]
    (index-koulutukset (get-oids :koulutusOid entries))
    (haku/do-index (get-oids :oid haut))
    entries))

(defn index-toteutus
  [oid]
  (index-toteutukset [oid]))

(defn index-haut
  [oids]
  (let [entries           (haku/do-index oids)
        hakukohde-entries (hakukohde/do-index (get-oids :oid (mapcat :hakukohteet entries)))
        toteutus-entries  (toteutus/do-index (get-oids :toteutusOid hakukohde-entries))
        _                 (koulutus-search/do-index (get-oids :koulutusOid toteutus-entries))]
    entries))

(defn index-haku
  [oid]
  (index-haut [oid]))

(defn index-hakukohteet
  [oids]
  (let [hakukohde-entries (hakukohde/do-index oids)
        haku-oids         (get-oids :hakuOid hakukohde-entries)
        koulutukset       (mapcat kouta-backend/list-koulutukset-by-haku haku-oids)]
    (haku/do-index haku-oids)
    (toteutus/do-index (get-oids :toteutusOid hakukohde-entries))
    (koulutus-search/do-index (get-oids :oid koulutukset))
    hakukohde-entries))

(defn index-hakukohde
  [oid]
  (index-hakukohteet [oid]))

(defn index-valintaperusteet
  [oids]
  (let [entries     (valintaperuste/do-index oids)
        hakukohteet (mapcat kouta-backend/list-hakukohteet-by-valintaperuste (get-oids :id entries))]
    (hakukohde/do-index (get-oids :oid hakukohteet))
    entries))

(defn index-valintaperuste
  [oid]
  (index-valintaperusteet [oid]))

(defn index-sorakuvaukset
  [ids]
  (let [entries          (sorakuvaus/do-index ids)
        koulutus-oids (mapcat kouta-backend/list-koulutus-oids-by-sorakuvaus (get-oids :id entries))]
    (index-koulutukset koulutus-oids)
    entries))

(defn index-sorakuvaus
  [oid]
  (index-sorakuvaukset [oid]))

(defn index-eperusteet
  [oids]
  (osaamisalakuvaus/do-index oids)
  (eperuste/do-index oids))

(defn index-eperuste
  [oid]
  (index-eperusteet [oid]))

(defn index-oppilaitokset
  [oids]
  (let [get-organisaation-koulutukset (fn [oid] (map :oid (some-> oid
                                                                  (hierarkia/get-hierarkia)
                                                                  (organisaatio-tool/find-oppilaitos-from-hierarkia)
                                                                  (:oid)
                                                                  (kouta-backend/get-koulutukset-by-tarjoaja))))]
    (let [entries (oppilaitos/do-index oids)]
      (oppilaitos-search/do-index oids)
      (koulutus-search/do-index (mapcat get-organisaation-koulutukset oids))
      entries)))

(defn index-oppilaitos
  [oid]
  (index-oppilaitokset [oid]))

(defn index-koodistot
  [koodistot]
  (koodisto/do-index koodistot))

(defn index-lokalisoinnit
  [lngs]
  (lokalisointi/do-index lngs))

(defn index-lokalisointi
  [lng]
  (index-lokalisoinnit [lng]))

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
    (let [ret (cond-> {}
                (contains? oids :koulutukset) (assoc :koulutukset (index-koulutukset (:koulutukset oids)))
                (contains? oids :toteutukset) (assoc :toteutukset (index-toteutukset (:toteutukset oids)))
                (contains? oids :haut) (assoc :haut (index-haut (:haut oids)))
                (contains? oids :hakukohteet) (assoc :hakukohteet (index-hakukohteet (:hakukohteet oids)))
                (contains? oids :sorakuvaukset) (assoc :sorakuvaukset (index-sorakuvaukset (:sorakuvaukset oids)))
                (contains? oids :valintaperusteet) (assoc :valintaperusteet (index-valintaperusteet (:valintaperusteet oids)))
                (contains? oids :eperusteet) (assoc :eperusteet (index-eperusteet (:eperusteet oids)))
                (contains? oids :oppilaitokset) (assoc :oppilaitokset (index-oppilaitokset (:oppilaitokset oids))))]
      (log/info (str "Indeksointi valmis. Aikaa kului " (- (. System (currentTimeMillis)) start) " ms"))
      ret)))

(defn index-since-kouta
  [since]
  (log/info (str "Indeksoidaan kouta-backendistä " (long->rfc1123 since) " jälkeen muuttuneet"))
  (let [start (. System (currentTimeMillis))
        date (long->rfc1123 since)
        oids (kouta-backend/get-last-modified date)]
    (index-oids oids)
    (log/info (str "Indeksointi valmis ja oidien haku valmis. Aikaa kului " (- (. System (currentTimeMillis)) start) " ms"))))

(defn index-all-kouta
  []
  (log/info (str "Indeksoidaan kouta-backendistä kaikki"))
  (let [start (. System (currentTimeMillis))
        oids (kouta-backend/all-kouta-oids)]
    (let [koulutus-entries (koulutus/do-index (:koulutukset oids))]
      (eperuste/do-index (eperuste-ids-on-koulutukset koulutus-entries))
      (tutkinnonosa/do-index (tutkinnonosa-ids-on-koulutukset koulutus-entries)))
    (koulutus-search/do-index (:koulutukset oids))
    (toteutus/do-index (:toteutukset oids))
    (haku/do-index (:haut oids))
    (hakukohde/do-index (:hakukohteet oids))
    (valintaperuste/do-index (:valintaperusteet oids))
    (oppilaitos/do-index (:oppilaitokset oids))
    (sorakuvaus/do-index (:sorakuvaukset oids))
    (oppilaitos-search/do-index (:oppilaitokset oids))
    (log/info (str "Indeksointi valmis ja oidien haku valmis. Aikaa kului " (- (. System (currentTimeMillis)) start) " ms"))))

(defn index-all-koulutukset
  []
  (index-koulutukset (:koulutukset (kouta-backend/all-kouta-oids))))

(defn index-all-toteutukset
  []
  (index-toteutukset (:toteutukset (kouta-backend/all-kouta-oids))))

(defn index-all-haut
  []
  (index-haut (:haut (kouta-backend/all-kouta-oids))))

(defn index-all-hakukohteet
  []
  (index-hakukohteet (:hakukohteet (kouta-backend/all-kouta-oids))))

(defn index-all-valintaperusteet
  []
  (index-valintaperusteet (:valintaperusteet (kouta-backend/all-kouta-oids))))

(defn index-all-sorakuvaukset
  []
  (index-sorakuvaukset (:sorakuvaukset (kouta-backend/all-kouta-oids))))

(defn index-all-eperusteet
  []
  (let [eperusteet (eperusteet-client/find-all)]
    (log/info "Indeksoidaan " (count eperusteet) " eperustetta")
    (index-eperusteet eperusteet)))

(defn index-all-oppilaitokset
  []
  (let [oppilaitokset (organisaatio-client/get-all-oppilaitos-oids)]
    (log/info "Indeksoidaan " (count oppilaitokset) " oppilaitosta.")
    (index-oppilaitokset oppilaitokset)))

(defn index-all-lokalisoinnit
  []
  (log/info "Indeksoidaan lokalisoinnit kaikilla kielillä.")
  (index-lokalisoinnit ["fi" "sv" "en"]))
