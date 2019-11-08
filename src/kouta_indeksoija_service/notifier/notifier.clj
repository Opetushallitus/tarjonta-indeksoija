(ns kouta-indeksoija-service.notifier.notifier
  (:require [kouta-indeksoija-service.rest.util :refer [post]]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [clojure.string :as str]
            [kouta-indeksoija-service.queue.sqs :as sqs]
            [kouta-indeksoija-service.util.tools :refer [get-id]]))

(def receivers (filter not-empty (str/split (:notifier-targets env) #",")))

(defn send-notification
  [message]
  (let [receiver (:receiver message)
        body (:message message)
        msg {:form-params body
             :content-type :json
             :force-redirects true}]
    (log/info "Sending notification message about" (:type body) (get-id body) "to" receiver)
    (post receiver msg)))

(defn- queue-notification-messages
  [body]
  (let [message {:message body}]
    (doseq [receiver receivers]
      (sqs/send-message
       (sqs/queue :notifications)
       (assoc message :receiver receiver)))))

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

(defn- queue-notifications
  [->message objects]
  (let [messages (map ->message objects)]
     (doseq [message messages] (queue-notification-messages message))
     objects))

(defn notify
  [objects]
  (queue-notifications koulutus->message (:koulutukset objects))
  (queue-notifications haku->message (:haut objects))
  (queue-notifications hakukohde->message (:hakukohteet objects))
  (queue-notifications toteutus->message (:toteutukset objects))
  (queue-notifications valintaperuste->message (:valintaperusteet objects))
  objects)
