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

;;Tässä ja toteutuksen indeksoinnissa pitäisi indeksoida myös hakukohteet sillä
;;hakukohteen indeksoinnissa luetaan koulutuksen ja toteutuksen tietoja ja jos nuo muuttuu
;;kouta-backendissä, ei muutos valu tällä hetkellä indeksoidulle hakukohteelle.
;;Tästä tiketti KTO-1226
(defn index-koulutukset
  [oids & execution-id]
  (let [entries (koulutus/do-index oids execution-id)]
    (koulutus-search/do-index oids execution-id)
    (eperuste/do-index (eperuste-ids-on-koulutukset entries) execution-id)
    (tutkinnonosa/do-index (tutkinnonosa-ids-on-koulutukset entries) execution-id)
    (oppilaitos-search/do-index (get-oids :oid (mapcat :tarjoajat entries)) execution-id)
    entries))

(defn index-koulutus
  [oid]
  (let [start (. System (currentTimeMillis))]
    (index-koulutukset [oid] start)))

(defn index-toteutukset
  [oids & execution-id]
  (let [entries (toteutus/do-index oids execution-id)
        haut    (mapcat kouta-backend/list-haut-by-toteutus oids)]
    (index-koulutukset (get-oids :koulutusOid entries) execution-id)
    (haku/do-index (get-oids :oid haut) execution-id)
    entries))

(defn index-toteutus
  [oid]
  (let [start (. System (currentTimeMillis))]
    (index-toteutukset [oid] start)))

(defn index-haut
  [oids & execution-id]
  (let [entries           (haku/do-index oids execution-id)
        hakukohde-entries (hakukohde/do-index (get-oids :oid (mapcat :hakukohteet entries)) execution-id)
        toteutus-entries  (toteutus/do-index (get-oids :toteutusOid hakukohde-entries) execution-id)]
    (koulutus-search/do-index (get-oids :koulutusOid toteutus-entries) execution-id)
    (oppilaitos-search/do-index (get-oids :oid (map :jarjestyspaikka hakukohde-entries)) execution-id)
    entries))

(defn index-haku
  [oid]
  (let [start (. System (currentTimeMillis))]
    (index-haut [oid] start)))

(defn index-hakukohteet
  [oids & execution-id]
  (let [hakukohde-entries (hakukohde/do-index oids execution-id)
        haku-oids         (get-oids :hakuOid hakukohde-entries)
        toteutus-entries  (toteutus/do-index (get-oids :toteutusOid hakukohde-entries) execution-id)]
    (haku/do-index haku-oids execution-id)
    (koulutus-search/do-index (get-oids :koulutusOid toteutus-entries) execution-id)
    (oppilaitos-search/do-index (get-oids :oid (map :jarjestyspaikka hakukohde-entries)) execution-id)
    hakukohde-entries))

(defn index-hakukohde
  [oid]
  (let [start (. System (currentTimeMillis))]
   (index-hakukohteet [oid] start)))

(defn index-valintaperusteet
  [oids & execution-id]
  (let [entries     (valintaperuste/do-index oids execution-id)
        hakukohteet (mapcat kouta-backend/list-hakukohteet-by-valintaperuste (get-oids :id entries))]
    (hakukohde/do-index (get-oids :oid hakukohteet) execution-id)
    entries))

(defn index-valintaperuste
  [oid]
  (let [start (. System (currentTimeMillis))]
    (index-valintaperusteet [oid] start)))

(defn index-sorakuvaukset
  [ids & execution-id]
  (let [entries       (sorakuvaus/do-index ids execution-id)
        koulutus-oids (mapcat kouta-backend/list-koulutus-oids-by-sorakuvaus (get-oids :id entries))]
    (koulutus/do-index koulutus-oids execution-id)
    entries))

(defn index-sorakuvaus
  [oid]
  (let [start (. System (currentTimeMillis))]
   (index-sorakuvaukset [oid] start)))

(defn index-eperusteet
  [oids & execution-id]
  (osaamisalakuvaus/do-index oids execution-id)
  (eperuste/do-index oids execution-id))

(defn index-eperuste
  [oid]
  (let [start (. System (currentTimeMillis))]
   (index-eperusteet [oid] start)))

(defn index-oppilaitokset
  [oids & execution-id]
  (let [get-organisaation-koulutukset (fn [oid] (let [result (map :oid (some-> oid
                                                                               (hierarkia/get-hierarkia)
                                                                               (organisaatio-tool/find-oppilaitos-from-hierarkia)
                                                                               (:oid)
                                                                               (kouta-backend/get-koulutukset-by-tarjoaja)))] result))
        entries (oppilaitos/do-index oids execution-id)]
    (oppilaitos-search/do-index oids execution-id)
    (koulutus-search/do-index (mapcat get-organisaation-koulutukset oids) execution-id)
    (hakukohde/do-index (mapcat kouta-backend/get-hakukohde-oids-by-jarjestyspaikka oids) execution-id)
    entries))

(defn index-oppilaitos
  [oid]
  (let [start (. System (currentTimeMillis))]
   (index-oppilaitokset [oid] start)))

(defn index-koodistot
  [koodistot]
  (let [start (. System (currentTimeMillis))]
    (koodisto/do-index koodistot start)))

(defn index-lokalisoinnit
  [lngs & id]
  (lokalisointi/do-index lngs id))

(defn index-lokalisointi
  [lng]
  (let [start (. System (currentTimeMillis))]
    (index-lokalisoinnit [lng] start)))

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
              (count (:oppilaitokset oids)) "oppilaitosta. ID:" [start])
    (let [ret (cond-> {}
                (contains? oids :koulutukset) (assoc :koulutukset (index-koulutukset (:koulutukset oids) start))
                (contains? oids :toteutukset) (assoc :toteutukset (index-toteutukset (:toteutukset oids) start))
                (contains? oids :haut) (assoc :haut (index-haut (:haut oids) start))
                (contains? oids :hakukohteet) (assoc :hakukohteet (index-hakukohteet (:hakukohteet oids)))
                (contains? oids :sorakuvaukset) (assoc :sorakuvaukset (index-sorakuvaukset (:sorakuvaukset oids)))
                (contains? oids :valintaperusteet) (assoc :valintaperusteet (index-valintaperusteet (:valintaperusteet oids)))
                (contains? oids :eperusteet) (assoc :eperusteet (index-eperusteet (:eperusteet oids)))
                (contains? oids :oppilaitokset) (assoc :oppilaitokset (index-oppilaitokset (:oppilaitokset oids))))]
      (log/info (str "Indeksointi valmis. Aikaa kului " (- (. System (currentTimeMillis)) start) " ms. ID:" [start]))
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
  (let [start (. System (currentTimeMillis))
        oids (kouta-backend/all-kouta-oids)]
    (log/info (str "Indeksoidaan kouta-backendistä kaikki, ID:" [start]))
    (let [koulutus-entries (koulutus/do-index (:koulutukset oids) start)]
      (eperuste/do-index (eperuste-ids-on-koulutukset koulutus-entries) start)
      (tutkinnonosa/do-index (tutkinnonosa-ids-on-koulutukset koulutus-entries) start))
    (koulutus-search/do-index (:koulutukset oids) start)
    (toteutus/do-index (:toteutukset oids) start)
    (haku/do-index (:haut oids) start)
    (hakukohde/do-index (:hakukohteet oids) start)
    (valintaperuste/do-index (:valintaperusteet oids) start)
    (oppilaitos/do-index (:oppilaitokset oids) start)
    (sorakuvaus/do-index (:sorakuvaukset oids) start)
    (oppilaitos-search/do-index (:oppilaitokset oids) start)
    (log/info (str "Indeksointi valmis ja oidien haku valmis. Aikaa kului " (- (. System (currentTimeMillis)) start) " ms, ID: " [start]))))

(defn index-all-koulutukset
  []
  (let [start (. System (currentTimeMillis))]
        (index-koulutukset (:koulutukset (kouta-backend/all-kouta-oids)) start)))

(defn index-all-toteutukset
  []
  (let [start (. System (currentTimeMillis))]
        (index-toteutukset (:toteutukset (kouta-backend/all-kouta-oids)) start)))

(defn index-all-haut
  []
  (let [start (. System (currentTimeMillis))]
    (index-haut (:haut (kouta-backend/all-kouta-oids)) start)))

(defn index-all-hakukohteet
  []
  (let [start (. System (currentTimeMillis))]
    (index-hakukohteet (:hakukohteet (kouta-backend/all-kouta-oids)) start)))

(defn index-all-valintaperusteet
  []
  (let [start (. System (currentTimeMillis))]
    (index-valintaperusteet (:valintaperusteet (kouta-backend/all-kouta-oids)) start)))

(defn index-all-sorakuvaukset
  []
  (let [start (. System (currentTimeMillis))]
    (index-sorakuvaukset (:sorakuvaukset (kouta-backend/all-kouta-oids)) start)))

(defn index-all-eperusteet
  []
  (let [eperusteet (eperusteet-client/find-all)
        start (. System (currentTimeMillis))]
    (log/info "Indeksoidaan " (count eperusteet) " eperustetta, ID:" (vec start))
    (index-eperusteet eperusteet start)))

(defn index-all-oppilaitokset
  []
  (let [oppilaitokset (organisaatio-client/get-all-oppilaitos-oids)
        start (. System (currentTimeMillis))]
    (log/info "Indeksoidaan " (count oppilaitokset) " oppilaitosta, ID:" (vec start))
    (index-oppilaitokset oppilaitokset start)))

(defn index-all-lokalisoinnit
  []
  (let [start (. System (currentTimeMillis))]
    (log/info "Indeksoidaan lokalisoinnit kaikilla kielillä, ID:" (vec start))
  (index-lokalisoinnit ["fi" "sv" "en"] start)))
