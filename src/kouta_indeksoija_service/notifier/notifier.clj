(ns kouta-indeksoija-service.notifier.notifier
  (:require [kouta-indeksoija-service.rest.util :refer [post]]
            [clj-log.error-log :refer [with-error-logging]]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [clojure.string :as str]
            [kouta-indeksoija-service.queue.sqs :as sqs]
            [kouta-indeksoija-service.elastic.tools :refer [get-id]]))

(def receivers (filter not-empty (str/split (:notifier-targets env) #",")))

(defn send-notification
  [receiver body]
  (let [msg {:form-params body
             :content-type :json
             :force-redirects true}]
    (log/info "Sending notification message about" (:type body) (get-id body) "to" receiver)
    (post receiver msg)))

(defn- queue-notifications
  [body]
  (let [message {:message body}]
    (doall
     (map
      #(sqs/send-message
        (sqs/find-queue (:notifications (:queue env)))
        (assoc message :receiver %))
      receivers))))

(defn- to-message
  [type id url object]
  (-> {}
      (assoc :type type)
      (assoc id (id object))
      (assoc :organisaatioOid (:oid (:organisaatio object)))
      (assoc :modified (:modified object))
      (assoc :url url)))

(defn- koulutus->message
  [koulutus]
  (let [url (resolve-url :kouta-external.koulutus.oid (:oid koulutus))
        tarjoajat (map :oid (:tarjoajat koulutus))
        message (to-message "koulutus" :oid url koulutus)]
    (assoc message :tarjoajat tarjoajat)))

(defn- haku->message
  [haku]
  (let [url (resolve-url :kouta-external.haku.oid (:oid haku))]
    (to-message "haku" :oid url haku)))

(defn- hakukohde->message
  [hakukohde]
  (let [url (resolve-url :kouta-external.hakukohde.oid (:oid hakukohde))]
    (to-message "hakukohde" :oid url hakukohde)))

(defn- toteutus->message
  [toteutus]
  (let [url (resolve-url :kouta-external.toteutus.oid (:oid toteutus))
        tarjoajat (map :oid (:tarjoajat toteutus))
        message (to-message "toteutus" :oid url toteutus)]
    (assoc message :tarjoajat tarjoajat)))

(defn- valintaperuste->message
  [valintaperuste]
  (let [url (resolve-url :kouta-external.valintaperuste.id (:id valintaperuste))]
    (to-message "valintaperuste" :id url valintaperuste)))

(defn- send-notification-messages
  [body]
  (let [msg {:form-params body
             :content-type :json}]
    (doseq [receiver receivers]
      (log/debug "Sending notification message" body "to" receiver)
      (post receiver msg))))

(defn- send-notifications
  [->message objects]
  (with-error-logging (let [messages (map ->message objects)]
     (doseq [message messages] (queue-notifications message))
     objects)))

(defn notify
  [objects]
  (send-notifications koulutus->message (:koulutukset objects))
  (send-notifications haku->message (:haut objects))
  (send-notifications hakukohde->message (:hakukohteet objects))
  (send-notifications toteutus->message (:toteutukset objects))
  (send-notifications valintaperuste->message (:valintaperusteet objects))
  objects)
