(ns konfo-indeksoija-service.rest.kouta
 (:require [konfo-indeksoija-service.util.conf :refer [env]]
           [clj-log.error-log :refer [with-error-logging]]
           [konfo-indeksoija-service.util.logging :refer [to-date-string]]
           [ring.util.codec :refer [url-encode]]
           [konfo-indeksoija-service.rest.util :as client]
           [clojure.tools.logging :as log]))

(defn get-last-modified [since]
  (log/info "Fetching last-modified since" since)
  (with-error-logging
   (let [url (str (:kouta-backend-url env) "anything/modifiedSince/" (url-encode since))
         res (:body (client/get url {:query-params {:lastModified since} :as :json}))]
     res)))

(defn get-doc
  [obj]
  (with-error-logging
   (let [url (str (:kouta-backend-url env) (:type obj) "/" (:oid obj))]
     (log/debug (str "GET => " url))
     (:body (client/get url {:as :json})))))

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
   (with-error-logging
    (let [url (str (:kouta-backend-url env) "koulutus/" koulutus-oid "/toteutukset")]
      (log/debug (str "GET => " url))
      (:body (client/get url {:as :json :query-params {:vainJulkaistut vainJulkaistut}})))))
  ([koulutus-oid]
   (get-toteutus-list-for-koulutus koulutus-oid false)))

(defn get-as-json
  [url]
  (with-error-logging
    (log/debug (str "GET => " url))
    (:body (client/get url {:as :json}))))

(defn get-hakutiedot-for-koulutus
  [koulutus-oid]
  (get-as-json (str (:kouta-backend-url env) "koulutus/" koulutus-oid "/hakutiedot")))

(defn list-hakukohteet-by-toteutus
  [toteutus-oid]
  (get-as-json (str (:kouta-backend-url env) "toteutus/" toteutus-oid "/hakukohteet/list")))

(defn list-haut-by-toteutus
  [toteutus-oid]
  (get-as-json (str (:kouta-backend-url env) "toteutus/" toteutus-oid "/haut/list")))

(defn list-hakukohteet-by-haku
  [haku-oid]
  (get-as-json (str (:kouta-backend-url env) "haku/" haku-oid "/hakukohteet/list")))

(defn list-koulutukset-by-haku
  [haku-oid]
  (get-as-json (str (:kouta-backend-url env) "haku/" haku-oid "/koulutukset/list")))

(defn list-hakukohteet-by-valintaperuste
  [valintaperuste-id]
  (get-as-json (str (:kouta-backend-url env) "valintaperuste/" valintaperuste-id "/hakukohteet/list")))