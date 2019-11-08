(ns kouta-indeksoija-service.queuer.queuer
  (:require [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.rest.eperuste :as eperusteet-client]
            [kouta-indeksoija-service.queuer.last-queued :refer [set-last-queued-time get-last-queued-time]]
            [kouta-indeksoija-service.util.time :refer [long->date-time-string]]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.queue.sqs :as sqs]
            [clojure.set :refer [union]]))

(def elastic-lock? (atom false :error-handler #(log/error %)))

(defn- queue
  [& {:keys [oppilaitokset eperusteet] :or {oppilaitokset [], eperusteet []}}]
  (sqs/send-message
   (sqs/queue :fast)
   (cond-> {}
           (not-empty oppilaitokset) (assoc :oppilaitokset (vec oppilaitokset))
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
  (let [all-eperusteet (eperusteet-client/find-all)]
    (doseq [eperusteet (partition-all 20 all-eperusteet)]
      (queue :eperusteet eperusteet))))

(defn queue-eperuste
  [oid]
  (queue :eperusteet [oid]))

(defn queue-all-oppilaitokset-from-organisaatiopalvelu
  []
  (let [all-organisaatiot (organisaatio-client/get-all-oppilaitos-oids)]
    (doseq [organisaatiot (partition-all 20 all-organisaatiot)]
      (queue :oppilaitokset organisaatiot))))

(defn queue-oppilaitos
  [oid]
  (queue :oppilaitokset [oid]))

(defn queue-changes
  []
  (wait-for-elastic-lock
   (let [now (System/currentTimeMillis)
         last-modified (get-last-queued-time)
         organisaatio-changes (organisaatio-client/find-last-changes last-modified)
         eperuste-changes (eperusteet-client/find-changes last-modified)
         changes-count (+ (count organisaatio-changes) (count eperuste-changes))]
     (when (< 0 changes-count)
       (log/info "Fetched last-modified since" (long->date-time-string last-modified)", containing" changes-count "changes.")
       (queue :oppilaitokset organisaatio-changes :eperusteet eperuste-changes)
       (set-last-queued-time now)))))
