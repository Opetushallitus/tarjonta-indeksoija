(ns kouta-indeksoija-service.rest.kouta
 (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
           [kouta-indeksoija-service.rest.cas.session :refer [init-session cas-authenticated-request]]
           [clj-log.error-log :refer [with-error-logging]]
           [ring.util.codec :refer [url-encode]]
           [clojure.tools.logging :as log]))

(defonce cas-session (init-session (resolve-url :kouta-backend.auth-login) false))

(defonce cas-authenticated-get (partial cas-authenticated-request cas-session :get))

(defn- cas-authenticated-get-as-json
  ([url opts]
   (with-error-logging
     (log/debug (str "GET => " url))
     (:body (cas-authenticated-get url (assoc opts :as :json)))))
  ([url]
   (cas-authenticated-get-as-json url {})))

(defn get-last-modified
  [since]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.modified-since (url-encode since)) {:query-params {:lastModified since}}))

(defn get-doc
  [obj]
  (let [type (:type obj)
        url-keyword (keyword (str "kouta-backend." type (if (= "valintaperuste" type) ".id" ".oid")))]
    (cas-authenticated-get-as-json (resolve-url url-keyword (:oid obj)))))

(defn get-koulutus
  [oid]
  (get-doc {:type "koulutus" :oid oid}))

(defn get-toteutus
  [oid]
  (get-doc {:type "toteutus" :oid oid}))

(defn get-haku
  [oid]
  (get-doc {:type "haku" :oid oid}))

(defn get-hakukohde
  [oid]
  (get-doc {:type "hakukohde" :oid oid}))

(defn get-valintaperuste
  [id]
  (get-doc {:type "valintaperuste" :oid id}))

(defn get-toteutus-list-for-koulutus
  ([koulutus-oid vainJulkaistut]
   (cas-authenticated-get-as-json (resolve-url :kouta-backend.koulutus.toteutukset koulutus-oid)
                                  {:query-params {:vainJulkaistut vainJulkaistut}}))
  ([koulutus-oid]
   (get-toteutus-list-for-koulutus koulutus-oid false)))

(defn get-hakutiedot-for-koulutus
  [koulutus-oid]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.koulutus.hakutiedot koulutus-oid)))

(defn list-hakukohteet-by-toteutus
  [toteutus-oid]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.toteutus.hakukohteet-list toteutus-oid)))

(defn list-haut-by-toteutus
  [toteutus-oid]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.toteutus.haut-list toteutus-oid)))

(defn list-hakukohteet-by-haku
  [haku-oid]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.haku.hakukohteet-list haku-oid)))

(defn list-koulutukset-by-haku
  [haku-oid]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.haku.koulutukset-list haku-oid)))

(defn list-hakukohteet-by-valintaperuste
  [valintaperuste-id]
  (cas-authenticated-get-as-json (resolve-url :kouta-backend.valintaperuste.hakukohteet-list valintaperuste-id)))