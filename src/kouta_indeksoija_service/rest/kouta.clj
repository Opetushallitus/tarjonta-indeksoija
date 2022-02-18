(ns kouta-indeksoija-service.rest.kouta
  (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [kouta-indeksoija-service.util.time :refer [long->rfc1123]]
            [kouta-indeksoija-service.rest.cas.session :refer [init-session cas-authenticated-request-as-json]]
            [clj-log.error-log :refer [with-error-logging]]
            [ring.util.codec :refer [url-encode]]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto]
            [kouta-indeksoija-service.indexer.tools.general :as general]
            [clojure.string]))

(defonce cas-session (init-session (resolve-url :kouta-backend.auth-login) false))

(defonce cas-authenticated-get-as-json (partial cas-authenticated-request-as-json cas-session :get))

(defn get-last-modified
  [since]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.modified-since (url-encode since))
                                 {:query-params {:lastModified since}}))

(defn all-kouta-oids
  []
  (get-last-modified (long->rfc1123 0)))

(defn- get-doc
  [type oid]
  (let [url-keyword (keyword (str "kouta-backend." type (if (or (= "valintaperuste" type) (= "sorakuvaus" type)) ".id" ".oid")))]
    (cas-authenticated-get-as-json (resolve-url url-keyword oid) {:query-params {:myosPoistetut "true"}})))

(defn get-koulutus
  [oid]
  (get-doc "koulutus" oid))

(defn get-toteutus
  [oid]
  (get-doc "toteutus" oid))

(defn get-haku
  [oid]
  (get-doc "haku" oid))

(defn get-hakukohde
  [oid]
  (get-doc "hakukohde" oid))

(defn get-hakukohde-oids-by-jarjestyspaikka [oid]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.jarjestyspaikka.hakukohde-oids oid)
                                              {:query-params {:vainOlemassaolevat "false"}}))

(defn get-valintaperuste
  [id]
  (get-doc "valintaperuste" id))

(defn get-sorakuvaus
  [id]
  (if (some? id) (get-doc "sorakuvaus" id) nil))

(defn get-oppilaitos
  [oid]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.oppilaitos.oid oid) {}))

(defn get-oppilaitos-hierarkia
  [oid]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.oppilaitos.hierarkia oid) {}))

(defn get-oppilaitoksen-osa
  [oid]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.oppilaitoksen-osa.oid oid) {}))

(defn get-toteutus-list-for-koulutus
  ([koulutus-oid vainJulkaistut]
   (cas-authenticated-get-as-json (resolve-url :kouta-backend.koulutus.toteutukset koulutus-oid)
                                  {:query-params {:vainJulkaistut vainJulkaistut}}))
  ([koulutus-oid]
   (get-toteutus-list-for-koulutus koulutus-oid false)))

(defn get-koulutukset-by-tarjoaja
  [oppilaitos-oid]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.tarjoaja.koulutukset oppilaitos-oid)))

(defn get-hakutiedot-for-koulutus
  [koulutus-oid]
  (let [response (cas-authenticated-get-as-json (resolve-url :kouta-backend.koulutus.hakutiedot koulutus-oid))]
    (if response
     (map (fn [hakutieto]
            (assoc hakutieto :haut
                   (map (fn [haku]
                          (assoc haku :hakukohteet
                                 (map (fn [hakukohde]
                                        (-> hakukohde
                                            (koodisto/assoc-hakukohde-nimi-from-koodi)
                                            (general/set-hakukohde-tila-by-related-haku haku)))
                                      (:hakukohteet haku))))
                        (:haut hakutieto))))
            response)
      response)))

(defn list-haut-by-toteutus
  [toteutus-oid]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.toteutus.haut-list toteutus-oid)
                                              {:query-params {:vainOlemassaolevat "false"}}))

(defn list-hakukohteet-by-haku
  [haku-oid]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.haku.hakukohteet-list haku-oid)
                                              {:query-params {:vainOlemassaolevat "false"}}))

(defn list-toteutukset-by-haku
  [haku-oid]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.haku.toteutukset-list haku-oid)))

(defn list-hakukohteet-by-valintaperuste
  [valintaperuste-id]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.valintaperuste.hakukohteet-list valintaperuste-id)
                                              {:query-params {:vainOlemassaolevat "false"}}))

(defn list-koulutus-oids-by-sorakuvaus
  [sorakuvaus-id]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.sorakuvaus.koulutukset-list sorakuvaus-id)
                                              {:query-params {:vainOlemassaolevat "false"}}))

(defn get-oppilaitoksen-osat
  [oppilaitos-oid]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.oppilaitos.osat oppilaitos-oid)))

