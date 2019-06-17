(ns kouta-indeksoija-service.queuer.queuer
  (:require [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.rest.eperuste :as eperusteet-client]
            [kouta-indeksoija-service.queuer.last-queued :refer [set-last-queued-time get-last-queued-time]]
            [kouta-indeksoija-service.util.time :refer [long->date-time-string]]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.queue.queue :as q]
            [kouta-indeksoija-service.queue.sqs :as sqs]
            [clojure.set :refer [union]]))

(def elastic-lock? (atom false :error-handler #(log/error %)))

(defn- queue
  [& {:keys [organisaatiot eperusteet] :or {organisaatiot [], eperusteet []}}]
  (sqs/send-message
   (q/queue :fast)
   (cond-> {}
           (not-empty organisaatiot) (assoc :organisaatiot (vec organisaatiot))
           (not-empty eperusteet) (assoc :eperusteet (vec eperusteet)))))

(defmacro wait-for-elastic-lock
  [& body]
  `(if-not (compare-and-set! elastic-lock? false true)
     (log/debug "Already queueing last changes, skipping job.")
     (try
       (do ~@body)
       (finally (reset! elastic-lock? false)))))

(defn queue-all-eperusteet
  []
  (let [all-eperusteet (map :oid (eperusteet-client/find-all))]
    (doseq [eperusteet (partition-all 20 all-eperusteet)]
      (queue :eperusteet eperusteet))))

(defn queue-eperuste
  [oid]
  (queue :eperusteet [oid]))

(defn queue-all-organisaatiot
  []
  (let [all-organisaatiot (organisaatio-client/get-all-oids)]
    (doseq [organisaatiot (partition-all 20 all-organisaatiot)]
      (queue :organisaatiot organisaatiot))))

(defn queue-organisaatio
  [oid]
  (queue :organisaatiot [oid]))

(defn queue-changes
  []
  (wait-for-elastic-lock
   (let [now (System/currentTimeMillis)
         last-modified (get-last-queued-time)
         organisaatio-changes (set (organisaatio-client/find-last-changes last-modified))
         eperuste-changes (set (eperusteet-client/find-changes last-modified))
         changes-since (remove nil? (clojure.set/union organisaatio-changes eperuste-changes))]
     (when-not (empty? changes-since)
       (log/info "Fetched last-modified since" (long->date-time-string last-modified)", containing" (count changes-since) "changes.")
       (queue :organisaatiot (map :oid organisaatio-changes) :eperusteet (map :oid eperuste-changes))
       (set-last-queued-time now)))))