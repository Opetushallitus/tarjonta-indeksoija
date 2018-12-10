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
     (log/info "Changes found: "
               (count (:koulutukset res)) "koulutusta, "
               (count (:toteutukset res)) "toteutusta, "
               (count (:haut res)) "hakua, "
               (count (:hakukohteet res)) "hakukohdetta ja "
               (count (:valintaperusteet res)) "valintaperustetta")
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

(defn get-toteutus-list-for-koulutus
  [koulutus-oid]
  (with-error-logging
   (let [url (str (:kouta-backend-url env) "koulutus/" koulutus-oid "/toteutukset")]
     (log/debug (str "GET => " url))
     (:body (client/get url {:as :json})))))
