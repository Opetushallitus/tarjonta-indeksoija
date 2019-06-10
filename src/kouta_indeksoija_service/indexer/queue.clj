(ns kouta-indeksoija-service.indexer.queue
  (:require [kouta-indeksoija-service.elastic.queue :refer [reset-queue upsert-to-queue]]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.rest.eperuste :as eperusteet-client]
            [kouta-indeksoija-service.elastic.queue :refer [set-last-index-time get-last-index-time upsert-to-queue]]
            [kouta-indeksoija-service.util.time :refer [long->date-time-string]]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.queue.queue :as q]
            [kouta-indeksoija-service.queue.sqs :as sqs]
            [clojure.set :refer [union]]))


(defn- find-docs
  [index oid]
  (cond
    (= "organisaatio" index) (organisaatio-client/find-docs oid)
    (= "eperuste" index) (eperusteet-client/find-all)
    :else nil))

(defn queue-all
  []
  (log/info "Tyhjennetään indeksointijono ja uudelleenindeksoidaan kaikki data eperusteista ja organisaatiopalvelusta.")
  (reset-queue)
  (let [organisaatio-docs (organisaatio-client/find-docs nil)
        eperusteet-docs (eperusteet-client/find-all)
        docs (union (set organisaatio-docs) (set eperusteet-docs))]
    (log/info "Saving" (count docs) "items to index-queue" (flatten (for [[k v] (group-by :type docs)] [(count v) k]) ))
    (upsert-to-queue docs)))

(defn queue-all-eperusteet
  []
  (let [eperusteet-docs (eperusteet-client/find-all)]
    (log/info "Lisätään indeksoijan jonoon " (count eperusteet-docs) " eperustetta")
    (upsert-to-queue eperusteet-docs)))

(defn queue-all-organisaatiot
  []
  (let [organisaatio-docs (organisaatio-client/find-docs nil)]
    (log/info "Lisätään indeksoijan jonoon " (count organisaatio-docs) " organisaatiota")
    (upsert-to-queue organisaatio-docs)))

(defn queue
  [index oid]
  (when-let [docs (not-empty (find-docs index oid))]
    (upsert-to-queue docs)))

(defn empty-queue []
  (reset-queue))

(def elastic-lock? (atom false :error-handler #(log/error %)))

(defmacro wait-for-elastic-lock
  [& body]
  `(if-not (compare-and-set! elastic-lock? false true)
     (log/debug "Already queueing last changes, skipping job.")
     (try
       (do ~@body)
       (finally (reset! elastic-lock? false)))))

(defn queue-changes
  []
  (wait-for-elastic-lock
   (let [now (System/currentTimeMillis)
         last-modified (get-last-index-time)
         organisaatio-changes (set (organisaatio-client/find-last-changes last-modified))
         eperuste-changes (set (eperusteet-client/find-changes last-modified))
         changes-since (remove nil? (clojure.set/union organisaatio-changes eperuste-changes))]
     (when-not (empty? changes-since)
       (log/info "Fetched last-modified since" (long->date-time-string last-modified)", containing" (count changes-since) "changes.")
       (comment sqs/send-message                            ;TODO
          (q/queue :fast)
          (cond-> {}
                  (not (empty? organisaatio-changes)) (assoc :organisaatiot (set (map #(:oid %) organisaatio-changes)))
                  (not (empty? eperuste-changes)) (assoc :eperusteet (set (map #(:oid %) eperuste-changes)))))
       (set-last-index-time now)))))