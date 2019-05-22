(ns kouta-indeksoija-service.rest.kouta
 (:require [kouta-indeksoija-service.util.conf :refer [env]]
           [kouta-indeksoija-service.rest.cas-session :refer [init-session cas-authenticated-request]]
           [clj-log.error-log :refer [with-error-logging]]
           [kouta-indeksoija-service.util.logging :refer [to-date-string]]
           [ring.util.codec :refer [url-encode]]
           [clojure.tools.logging :as log]))

(defonce cas-session (init-session "/kouta-backend/auth/login" false))

(defonce cas-authenticated-get (partial cas-authenticated-request cas-session :get))

(defn- cas-authenticated-get-as-json
  ([url opts]
   (with-error-logging
     (log/debug (str "GET => " url))
     (:body (cas-authenticated-get url (assoc opts :as :json)))))
  ([url]
   (cas-authenticated-get-as-json url {})))

(defn get-last-modified [since]
  (let [url (str (:kouta-backend-url env) "anything/modifiedSince/" (url-encode since))]
    (cas-authenticated-get-as-json url {:query-params {:lastModified since}})))

(defn get-doc
  [obj]
  (let [url (str (:kouta-backend-url env) (:type obj) "/" (:oid obj))]
    (cas-authenticated-get-as-json url)))

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
   (let [url (str (:kouta-backend-url env) "koulutus/" koulutus-oid "/toteutukset")]
     (cas-authenticated-get-as-json url {:query-params {:vainJulkaistut vainJulkaistut}})))
  ([koulutus-oid]
   (get-toteutus-list-for-koulutus koulutus-oid false)))

(defn get-hakutiedot-for-koulutus
  [koulutus-oid]
  (cas-authenticated-get-as-json (str (:kouta-backend-url env) "koulutus/" koulutus-oid "/hakutiedot")))

(defn list-hakukohteet-by-toteutus
  [toteutus-oid]
  (cas-authenticated-get-as-json (str (:kouta-backend-url env) "toteutus/" toteutus-oid "/hakukohteet/list")))

(defn list-haut-by-toteutus
  [toteutus-oid]
  (cas-authenticated-get-as-json (str (:kouta-backend-url env) "toteutus/" toteutus-oid "/haut/list")))

(defn list-hakukohteet-by-haku
  [haku-oid]
  (cas-authenticated-get-as-json (str (:kouta-backend-url env) "haku/" haku-oid "/hakukohteet/list")))

(defn list-koulutukset-by-haku
  [haku-oid]
  (cas-authenticated-get-as-json (str (:kouta-backend-url env) "haku/" haku-oid "/koulutukset/list")))

(defn list-hakukohteet-by-valintaperuste
  [valintaperuste-id]
  (cas-authenticated-get-as-json (str (:kouta-backend-url env) "valintaperuste/" valintaperuste-id "/hakukohteet/list")))