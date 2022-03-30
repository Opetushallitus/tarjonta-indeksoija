(ns kouta-indeksoija-service.rest.kouta
  (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [kouta-indeksoija-service.util.time :refer [long->rfc1123]]
            [kouta-indeksoija-service.rest.cas.session :refer [init-session cas-authenticated-request-as-json]]
            [clj-log.error-log :refer [with-error-logging]]
            [ring.util.codec :refer [url-encode]]
            [clojure.core.memoize :as memo]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto]
            [kouta-indeksoija-service.indexer.tools.general :as general]))

(defonce cas-session (init-session (resolve-url :kouta-backend.auth-login) false))

(defonce cas-authenticated-get-as-json (partial cas-authenticated-request-as-json cas-session :get))
(defonce cas-authenticated-post-as-json (partial cas-authenticated-request-as-json cas-session :post))

(defonce kouta_cache_time_millis (* 1000 60 60)) ;60 mins cache

(defn get-last-modified
  [since]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.modified-since (url-encode since))
                                 {:query-params {:lastModified since}}))

(defn all-kouta-oids
  []
  (get-last-modified (long->rfc1123 0)))

(defn- get-doc
  [type oid execution-id]
  ; execution-id for cache purposes only.
  (let [url-keyword (keyword (str "kouta-backend." type (if (or (= "valintaperuste" type) (= "sorakuvaus" type)) ".id" ".oid")))]
    (cas-authenticated-get-as-json (resolve-url url-keyword oid) {:query-params {:myosPoistetut "true"}})))

(def get-doc-with-cache
  (memo/ttl get-doc {} :ttl/threshold kouta_cache_time_millis))

(defn get-koulutus-with-cache
  [oid execution-id]
  (get-doc-with-cache "koulutus" oid execution-id))

(defn get-toteutus-with-cache
  [oid execution-id]
  (get-doc-with-cache "toteutus" oid execution-id))

(defn get-haku-with-cache
  [oid execution-id]
  (get-doc-with-cache "haku" oid execution-id))

(defn get-hakukohde-with-cache
  [oid execution-id]
  (get-doc-with-cache "hakukohde" oid execution-id))

(defn get-hakukohde-oids-by-jarjestyspaikat
  [oids execution-id]
  ; execution-id for cache purposes only
  (cas-authenticated-post-as-json (resolve-url :kouta-backend.jarjestyspaikat.hakukohde-oids) {:body (json/generate-string oids) :content-type :json}))

(def get-hakukohde-oids-by-jarjestyspaikat-with-cache
  (memo/ttl get-hakukohde-oids-by-jarjestyspaikat {} :ttl/threshold kouta_cache_time_millis))

(defn get-valintaperuste-with-cache
  [id execution-id]
  (get-doc-with-cache "valintaperuste" id execution-id))

(defn get-sorakuvaus-with-cache
  [id execution-id]
  (if (some? id) (get-doc-with-cache "sorakuvaus" id execution-id) nil))

(defn get-oppilaitos
  [oid execution-id]
  ; execution-id for cache purposes only
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.oppilaitos.oid oid) {}))

(def get-oppilaitos-with-cache
  (memo/ttl get-oppilaitos {} :ttl/threshold kouta_cache_time_millis))

(defn get-oppilaitos-hierarkia
  [oid execution-id]
  ; execution-id for cache purposes only
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.oppilaitos.hierarkia oid) {}))

(def get-oppilaitos-hierarkia-with-cache
  (memo/ttl get-oppilaitos-hierarkia {} :ttl/threshold kouta_cache_time_millis))

(defn get-toteutus-list-for-koulutus
  ([koulutus-oid vainJulkaistut execution-id]
   ; execution id for cache purposes only
   (cas-authenticated-get-as-json (resolve-url :kouta-backend.koulutus.toteutukset koulutus-oid)
                                  {:query-params {:vainJulkaistut vainJulkaistut}}))
  ([koulutus-oid execution-id]
   (get-toteutus-list-for-koulutus koulutus-oid false execution-id)))

(def get-toteutus-list-for-koulutus-with-cache
  (memo/ttl get-toteutus-list-for-koulutus {} :ttl/threshold kouta_cache_time_millis))

(defn get-koulutukset-by-tarjoaja
  [oppilaitos-oid execution-id]
  ; execution-id for cache purposes only
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.tarjoaja.koulutukset oppilaitos-oid)))

(def get-koulutukset-by-tarjoaja-with-cache
  (memo/ttl get-koulutukset-by-tarjoaja {} :ttl/threshold kouta_cache_time_millis))


(defn get-hakutiedot-for-koulutus
  [koulutus-oid execution-id]
  ; execution id for cache purposes only
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

(def get-hakutiedot-for-koulutus-with-cache
  (memo/ttl get-hakutiedot-for-koulutus {} :ttl/threshold kouta_cache_time_millis))

(defn list-haut-by-toteutus
  [toteutus-oid execution-id]
  ; execution-id for cache purposes only
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.toteutus.haut-list toteutus-oid)
                                 {:query-params {:vainOlemassaolevat "false"}}))

(def list-haut-by-toteutus-with-cache
  (memo/ttl list-haut-by-toteutus {} :ttl/threshold kouta_cache_time_millis))

(defn list-hakukohteet-by-haku
  [haku-oid execution-id]
  ; execution-id for cache purposes only
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.haku.hakukohteet-list haku-oid)
                                 {:query-params {:vainOlemassaolevat "false"}}))

(def list-hakukohteet-by-haku-with-cache
  (memo/ttl list-hakukohteet-by-haku {} :ttl/threshold kouta_cache_time_millis))

(defn list-toteutukset-by-haku
  [haku-oid execution-id]
  ; execution-id for cache purposes only
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.haku.toteutukset-list haku-oid)))

(def list-toteutukset-by-haku-with-cache
  (memo/ttl list-toteutukset-by-haku {} :ttl/threshold kouta_cache_time_millis))

(defn list-hakukohteet-by-valintaperuste
  [valintaperuste-id execution-id]
  ; execution-id for cache purposes only
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.valintaperuste.hakukohteet-list valintaperuste-id)
                                 {:query-params {:vainOlemassaolevat "false"}}))

(def list-hakukohteet-by-valintaperuste-with-cache
  (memo/ttl list-hakukohteet-by-valintaperuste {} :ttl/threshold kouta_cache_time_millis))

(defn list-koulutus-oids-by-sorakuvaus
  [sorakuvaus-id execution-id]
  ; execution-id for cache purposes only
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.sorakuvaus.koulutukset-list sorakuvaus-id)
                                 {:query-params {:vainOlemassaolevat "false"}}))

(def list-koulutus-oids-by-sorakuvaus-with-cache
  (memo/ttl list-koulutus-oids-by-sorakuvaus {} :ttl/threshold kouta_cache_time_millis))

(defn get-oppilaitoksen-osat
  [oppilaitos-oid execution-id]
  ; execution-id for cache purposes only.
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.oppilaitos.osat oppilaitos-oid)))

(def get-oppilaitoksen-osat-with-cache
  (memo/ttl get-oppilaitoksen-osat {} :ttl/threshold kouta_cache_time_millis))

