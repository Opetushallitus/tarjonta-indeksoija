(ns kouta-indeksoija-service.notifier.notifier
  (:require [kouta-indeksoija-service.rest.util :refer [post]]
            [clj-log.error-log :refer [with-error-logging-value]]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [clojure.string :as str]
            [kouta-indeksoija-service.indexer.indexer :as indexer]))

(def receivers (filter not-empty (str/split (:notifier-targets env) #",")))

(defn- to-message
  [type id url object]
  (-> {}
      (assoc :type type)
      (assoc id (id object))
      (assoc :organisaatioOid (:oid (:organisaatio object)))
      (assoc :modified (:modified object))
      (assoc :url url)))

(defn- to-message-koulutus
  [koulutus]
  (let [url (resolve-url :kouta-external.koulutus.oid (:oid koulutus))
        tarjoajat (map :oid (:tarjoajat koulutus))
        message (to-message "koulutus" :oid url koulutus)]
    (assoc message :tarjoajat tarjoajat)))

(defn- to-message-haku
  [haku]
  (let [url (resolve-url :kouta-external.haku.oid (:oid haku))]
    (to-message "haku" :oid url haku)))

(defn- to-message-hakukohde
  [hakukohde]
  (let [url (resolve-url :kouta-external.hakukohde.oid (:oid hakukohde))]
    (to-message "hakukohde" :oid url hakukohde)))

(defn- to-message-toteutus
  [toteutus]
  (let [url (resolve-url :kouta-external.toteutus.oid (:oid toteutus))
        tarjoajat (map :oid (:tarjoajat toteutus))
        message (to-message "toteutus" :oid url toteutus)]
    (assoc message :tarjoajat tarjoajat)))

(defn- to-message-valintaperuste
  [valintaperuste]
  (let [url (resolve-url :kouta-external.valintaperuste.id (:id valintaperuste))]
    (to-message "valintaperuste" :id url valintaperuste)))

(defn- send-notifications
  [body]
  (let [msg {:form-params body
             :content-type :json}]
    (doall (map (fn [receiver]
                  (log/debug "Sending notification message" body "to" receiver)
                  (post receiver msg)) receivers))))

(defn- send-koulutus-notifications
  [koulutukset]
  (with-error-logging-value
   koulutukset
   (let [messages (map to-message-koulutus koulutukset)]
     (doall (map send-notifications messages))
     koulutukset)))

(defn- send-haku-notifications
  [haut]
  (with-error-logging-value
   haut
   (let [messages (map to-message-haku haut)]
     (doall (map send-notifications messages))
     haut)))

(defn- send-hakukohde-notifications
  [hakukohteet]
  (with-error-logging-value
   hakukohteet
   (let [messages (map to-message-hakukohde hakukohteet)]
     (doall (map send-notifications messages))
     hakukohteet)))

(defn- send-toteutus-notifications
  [toteutukset]
  (with-error-logging-value
   toteutukset
   (let [messages (map to-message-toteutus toteutukset)]
     (doall (map send-notifications messages))
     toteutukset)))

(defn- send-valintaperuste-notifications
  [valintaperusteet]
  (with-error-logging-value
   valintaperusteet
   (let [messages (map to-message-valintaperuste valintaperusteet)]
     (doall (map send-notifications messages))
     valintaperusteet)))

(defn notify
  [things]
  (->
   things
   (indexer/rewrite-non-empty :koulutukset send-koulutus-notifications)
   (indexer/rewrite-non-empty :haut send-haku-notifications)
   (indexer/rewrite-non-empty :hakukohteet send-hakukohde-notifications)
   (indexer/rewrite-non-empty :toteutukset send-toteutus-notifications)
   (indexer/rewrite-non-empty :valintaperusteet send-valintaperuste-notifications)))
