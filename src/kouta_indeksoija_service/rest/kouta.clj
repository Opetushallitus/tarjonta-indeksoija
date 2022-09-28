(ns kouta-indeksoija-service.rest.kouta
  (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [kouta-indeksoija-service.util.time :refer [long->rfc1123]]
            [kouta-indeksoija-service.rest.cas.session :refer [init-session cas-authenticated-request-as-json]]
            [clj-log.error-log :refer [with-error-logging]]
            [ring.util.codec :refer [url-encode]]
            [ivarref.memoize-ttl :as ttl]
            [cheshire.core :as json]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto]
            [kouta-indeksoija-service.indexer.tools.general :as general]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [clojure.string :as str]))

(defonce cas-session (init-session (resolve-url :kouta-backend.auth-login) false))

(defonce cas-authenticated-get-as-json (partial cas-authenticated-request-as-json cas-session :get))
(defonce cas-authenticated-post-as-json (partial cas-authenticated-request-as-json cas-session :post))

(defonce massa-kouta-cache-time-seconds (Integer. (:kouta-indeksoija-massa-kouta-cache-time-seconds env)))
(defonce kouta-cache-time-seconds (Integer. (:kouta-indeksoija-kouta-cache-time-seconds env)))

(defn- get-cache-time [execution-id]
  (if (str/starts-with? (str execution-id) "MASSA-")
    massa-kouta-cache-time-seconds
    kouta-cache-time-seconds))

(defn get-last-modified
  [since]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.modified-since (url-encode since))
                                 {:query-params {:lastModified since}}))

(defn all-kouta-oids
  []
  (get-last-modified (long->rfc1123 0)))

(defn- get-doc
  [type oid execution-id]
  {:val (let [url-keyword (keyword (str "kouta-backend." type (if (or (= "valintaperuste" type) (= "sorakuvaus" type)) ".id" ".oid")))]
          (cas-authenticated-get-as-json (resolve-url url-keyword oid) {:query-params {:myosPoistetut "true"}}))
   :ttl (get-cache-time execution-id)})

(def get-doc-with-cache
  (ttl/memoize-ttl get-doc))

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
  {:val (cas-authenticated-post-as-json (resolve-url :kouta-backend.jarjestyspaikat.hakukohde-oids) {:body (json/generate-string oids) :content-type :json})
   :ttl (get-cache-time execution-id)})

(def get-hakukohde-oids-by-jarjestyspaikat-with-cache
  (ttl/memoize-ttl get-hakukohde-oids-by-jarjestyspaikat))

(defn get-toteutus-oids-by-tarjoajat
  [oids execution-id]
  {:val (cas-authenticated-post-as-json (resolve-url :kouta-backend.tarjoajat.toteutus-oids) {:body (json/generate-string oids) :content-type :json})
   :ttl (get-cache-time execution-id)})

(def get-toteutus-oids-by-tarjoajat-with-cache
  (ttl/memoize-ttl get-toteutus-oids-by-tarjoajat))

(defn get-valintaperuste-with-cache
  [id execution-id]
  (get-doc-with-cache "valintaperuste" id execution-id))

(defn get-sorakuvaus-with-cache
  [id execution-id]
  (if (some? id) (get-doc-with-cache "sorakuvaus" id execution-id) nil))

(defn get-oppilaitos
  [oid execution-id]
  {:val (cas-authenticated-get-as-json (resolve-url :kouta-backend.oppilaitos.oid oid) {})
   :ttl (get-cache-time execution-id)})

(def get-oppilaitos-with-cache
  (ttl/memoize-ttl get-oppilaitos))

(defn get-oppilaitokset
  [oids execution-id]
  {:val (cas-authenticated-post-as-json (resolve-url :kouta-backend.oppilaitos.oppilaitokset) {:body (json/generate-string oids) :content-type :json})
   :ttl (get-cache-time execution-id)})

(def get-oppilaitokset-with-cache
  (ttl/memoize-ttl get-oppilaitokset))

(defn get-toteutus-list-for-koulutus
  ([koulutus-oid vainJulkaistut execution-id]
   {:val (cas-authenticated-get-as-json (resolve-url :kouta-backend.koulutus.toteutukset koulutus-oid)
                                        {:query-params {:vainJulkaistut vainJulkaistut}})
    :ttl (get-cache-time execution-id)})
  ([koulutus-oid execution-id]
   (get-toteutus-list-for-koulutus koulutus-oid false execution-id)))

(def get-toteutus-list-for-koulutus-with-cache
  (ttl/memoize-ttl get-toteutus-list-for-koulutus))

(defn get-koulutukset-by-tarjoaja
  [oppilaitos-oid execution-id]
  {:val (cas-authenticated-get-as-json (resolve-url :kouta-backend.tarjoaja.koulutukset oppilaitos-oid))
   :ttl (get-cache-time execution-id)})

(def get-koulutukset-by-tarjoaja-with-cache
  (ttl/memoize-ttl get-koulutukset-by-tarjoaja))


(defn get-hakutiedot-for-koulutus
  [koulutus-oid execution-id]
  {:val (let [response (cas-authenticated-get-as-json (resolve-url :kouta-backend.koulutus.hakutiedot koulutus-oid))]
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
            response))
   :ttl (get-cache-time execution-id)})

(def get-hakutiedot-for-koulutus-with-cache
  (ttl/memoize-ttl get-hakutiedot-for-koulutus))

(defn list-haut-by-toteutus
  [toteutus-oid execution-id]
  {:val (cas-authenticated-get-as-json (resolve-url :kouta-backend.toteutus.haut-list toteutus-oid)
                                       {:query-params {:vainOlemassaolevat "false"}})
   :ttl (get-cache-time execution-id)})

(def list-haut-by-toteutus-with-cache
  (ttl/memoize-ttl list-haut-by-toteutus))

(defn list-hakukohteet-by-haku
  [haku-oid execution-id]
  {:val (cas-authenticated-get-as-json (resolve-url :kouta-backend.haku.hakukohteet-list haku-oid)
                                       {:query-params {:vainOlemassaolevat "false"}})
   :ttl (get-cache-time execution-id)})

(def list-hakukohteet-by-haku-with-cache
  (ttl/memoize-ttl list-hakukohteet-by-haku))

(defn list-toteutukset-by-haku
  [haku-oid execution-id]
  {:val (cas-authenticated-get-as-json (resolve-url :kouta-backend.haku.toteutukset-list haku-oid))
   :ttl (get-cache-time execution-id)})

(def list-toteutukset-by-haku-with-cache
  (ttl/memoize-ttl list-toteutukset-by-haku))

(defn list-hakukohteet-by-valintaperuste
  [valintaperuste-id execution-id]
  {:val (cas-authenticated-get-as-json (resolve-url :kouta-backend.valintaperuste.hakukohteet-list valintaperuste-id)
                                       {:query-params {:vainOlemassaolevat "false"}})
   :ttl (get-cache-time execution-id)})

(def list-hakukohteet-by-valintaperuste-with-cache
  (ttl/memoize-ttl list-hakukohteet-by-valintaperuste))

(defn list-koulutus-oids-by-sorakuvaus
  [sorakuvaus-id execution-id]
  {:val (cas-authenticated-get-as-json (resolve-url :kouta-backend.sorakuvaus.koulutukset-list sorakuvaus-id)
                                       {:query-params {:vainOlemassaolevat "false"}})
   :ttl (get-cache-time execution-id)})

(def list-koulutus-oids-by-sorakuvaus-with-cache
  (ttl/memoize-ttl list-koulutus-oids-by-sorakuvaus))

(defn get-oppilaitoksen-osat
  [oppilaitos-oid execution-id]
  {:val (cas-authenticated-get-as-json (resolve-url :kouta-backend.oppilaitos.osat oppilaitos-oid))
   :ttl (get-cache-time execution-id)})

(def get-oppilaitoksen-osat-with-cache
  (ttl/memoize-ttl get-oppilaitoksen-osat))

(defn get-toteutukset
  [oids execution-id]
  {:val (cas-authenticated-post-as-json
          (resolve-url :kouta-backend.toteutukset) {:body (json/generate-string oids) :content-type :json})
   :ttl (get-cache-time execution-id)})

(def get-toteutukset-with-cache
  (ttl/memoize-ttl get-toteutukset))

(defn get-opintokokonaisuus-oids-by-toteutus-oids
  [oids execution-id]
  {:val (cas-authenticated-post-as-json
          (resolve-url :kouta-backend.opintokokonaisuus-oids) {:body (json/generate-string oids) :content-type :json})
   :ttl (get-cache-time execution-id)})

(def get-opintokokonaisuus-oids-by-toteutus-oids-cache
  (ttl/memoize-ttl get-opintokokonaisuus-oids-by-toteutus-oids))

