(ns tarjonta-indeksoija-service.indexer
  (:require [tarjonta-indeksoija-service.conf :refer [env job-pool]]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta-client]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [tarjonta-indeksoija-service.converter.koulutus-converter :as koulutus-converter]
            [tarjonta-indeksoija-service.converter.hakukohde-converter :as hakukohde-converter]
            [taoensso.timbre :as log]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.jobs :as j :refer [defjob]]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]])
  (:import (org.quartz ObjectAlreadyExistsException)))

(def running? (atom false :error-handler #(log/error %)))

(defn convert-doc
  [type doc]
  (cond
    (.contains type "koulutus") (koulutus-converter/convert doc)
    (.contains type "hakukohde") (hakukohde-converter/convert doc)
    :else doc))

(defn index-haku-hakukohteet
  [hakukohde-oids]
  (let [docs (map #(hash-map :oid % :type "hakukohde") (distinct hakukohde-oids))]
    (elastic-client/upsert-indexdata docs)))

(defn index-related-docs
  ;; TODO: Make propagation work for all docs in all 'directions'. This is just a WIP.
  [type doc]
  (log/debug "indexing docs related")
  (when (= "haku" type)
    (index-haku-hakukohteet (:hakukohdeOids doc))))

(defn index-object
  [obj]
  (log/info "Indexing" (:type obj) (:oid obj))
  (try
    (let [doc (tarjonta-client/get-doc obj)
          converted-doc (convert-doc (:type obj) doc)
          res (elastic-client/bulk-upsert (:type obj) (:type obj) [converted-doc])
          errors (:errors res)
          status (:result (:update (first (:items res))))]
      (if errors
        (log/error (str "Indexing failed for "
                        (clojure.string/capitalize (:type obj)) " " (:oid obj)
                        "\n" errors))
        (log/info (str (clojure.string/capitalize (:type obj)) " " (:oid obj) " " status " succesfully."))))
    (catch Exception e (log/error e))))

(defn end-indexing
  [oids last-timestamp]
  (log/info "The indexing queue was empty, stopping indexing and deleting indexed items from queue.")
  (elastic-client/delete-handled-queue oids last-timestamp)
  (elastic-client/refresh-index "indexdata"))

(defn do-index
  []
  (let [queue (elastic-client/get-queue)]
    (if (empty? queue)
      (log/debug "Nothing to index.")
      (do
        (doall (pmap index-object queue))
        (end-indexing (map :oid queue)
                      (apply max (map :timestamp queue)))))))

(defn start-indexing
  []
  (try
    (if @running?
      (log/debug "Indexing already running.")
      (do
        (reset! running? true)
        (do-index)))
    (catch Exception e (log/error e))
    (finally (reset! running? false))))

(defjob indexing-job
  [ctx]
  (try
    (let [last-modified (tarjonta-client/get-last-modified (elastic-client/get-last-index-time))
          now (System/currentTimeMillis)]
      (elastic-client/upsert-indexdata last-modified)
      (elastic-client/set-last-index-time now)
      (start-indexing))
    (catch Exception e (log/error e))))

(defn start-indexer-job
  []
  (let [job (j/build
              (j/of-type indexing-job)
              (j/with-identity "jobs.index.1"))
        trigger (t/build
                  (t/with-identity (t/key "crontirgger"))
                  (t/start-now)
                  (t/with-schedule
                    (schedule (cron-schedule (:cron-string env)))))]
    (log/info (str "Starting indexer with cron schedule " (:cron-string env))
     (qs/schedule job-pool job trigger))))

(defn reset-jobs
  []
  (reset! running? false)
  (qs/clear! job-pool))

(defn start-stop-indexer
  [start?]
  (try
    (if start?
      (do
        (start-indexer-job)
        "Started indexer job")
      (do
        (reset-jobs)
        "Stopped all jobs and reseted pool."))
    (catch ObjectAlreadyExistsException e "Indexer already running.")
    (catch Exception e (str "Caught exception: " (.getMessage e)))))