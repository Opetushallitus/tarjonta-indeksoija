(ns kouta-indeksoija-service.notifier.notifier
  (:require [kouta-indeksoija-service.rest.util :refer [post]]
            [clj-log.error-log :refer [with-error-logging-value]]
            [clojure.tools.logging :as log]))

(def receiver "http://localhost:6006")
(def external-url "http://localhost:8097")

(defn- to-message
  [type id object]
  (-> {}
      (assoc :type type)
      (assoc id (id object))
      (assoc :organisaatioOid (:oid (:organisaatio object)))
      (assoc :modified (:modified object))
      (assoc :url (str external-url "/" type "/" (id object)))))

(defn- to-sendable-koulutus
  [koulutus]
  (assoc (to-message "koulutus" :oid koulutus) :tarjoajat (map :oid (:tarjoajat koulutus))))

(defn- to-sendable-haku
  [haku]
  (to-message "haku" :oid haku))

(defn- to-sendable-hakukohde
  [hakukohde]
  (to-message "hakukohde" :oid hakukohde))

(defn- to-sendable-toteutus
  [toteutus]
  (assoc (to-message "toteutus" :oid toteutus) :tarjoajat (map :oid (:tarjoajat toteutus))))

(defn- to-sendable-valintaperuste
  [valintaperuste]
  (to-message "valintaperuste" :id valintaperuste))

(defn- send-notification
  [body]
  (post receiver {:form-params body
                  :content-type :json}))

(defn send-koulutus-notification
  [objects]
  (with-error-logging-value
   objects
   (let [messages (map to-sendable-koulutus objects)]
     (doall (map send-notification messages))
     objects)))

(defn send-haku-notification
  [objects]
  (with-error-logging-value
   objects
   (let [messages (map to-sendable-haku objects)]
     (doall (map send-notification messages))
     objects)))

(defn send-hakukohde-notification
  [objects]
  (with-error-logging-value
   objects
   (let [messages (map to-sendable-hakukohde objects)]
     (doall (map send-notification messages))
     objects)))

(defn send-toteutus-notification
  [objects]
  (with-error-logging-value
   objects
   (let [messages (map to-sendable-toteutus objects)]
     (doall (map send-notification messages))
     objects)))

(defn send-valintaperuste-notification
  [objects]
  (with-error-logging-value
   objects
   (let [messages (map to-sendable-valintaperuste objects)]
     (doall (map send-notification messages))
     objects)))
