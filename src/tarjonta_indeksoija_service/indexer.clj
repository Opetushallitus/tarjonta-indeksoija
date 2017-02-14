(ns tarjonta-indeksoija-service.indexer
  (:require [tarjonta-indeksoija-service.conf :refer [env job-pool]]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta-client]
            [tarjonta-indeksoija-service.organisaatio-client :as organisaatio-client]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [tarjonta-indeksoija-service.converter.koulutus-converter :as koulutus-converter]
            [tarjonta-indeksoija-service.converter.hakukohde-converter :as hakukohde-converter]
            [tarjonta-indeksoija-service.util.tools :refer [with-error-logging]]
            [taoensso.timbre :as log]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.jobs :as j :refer [defjob]]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]])
  (:import (org.quartz ObjectAlreadyExistsException)))

(def running? (atom false :error-handler #(log/error %)))

(defn append-search-data
  [koulutus]
  (let [hakukohteet-raw (tarjonta-client/get-hakukohteet-for-koulutus (:oid koulutus))
        hakukohteet (doall (map #(clojure.set/rename-keys % {:relatedOid :hakuOid}) hakukohteet-raw))
        haut-raw (tarjonta-client/get-haut-by-oids (distinct (map :hakuOid hakukohteet)))
        haut (doall (map #(dissoc % [:hakukohdeOidsYlioppilastutkintoAntaaHakukelpoisuuden
                                     :hakukohdeOids]) haut-raw))]
    (assoc koulutus :searchData {:hakukohteet hakukohteet :haut haut})))

(defn convert-doc
  [doc type]
  (cond
    (.contains type "koulutus") (->> doc
                                     koulutus-converter/convert
                                     append-search-data)
    (.contains type "hakukohde") (hakukohde-converter/convert doc)
    :else doc))

(defn- get-doc [obj]
  (cond
    (= (:type obj) "organisaatio") (organisaatio-client/get-doc obj)
    (= (:type obj) "koulutus") (tarjonta-client/get-doc obj)
    (= (:type obj) "hakukohde") (tarjonta-client/get-doc obj)
    (= (:type obj) "haku") (tarjonta-client/get-doc obj)))

(defn get-coverted-doc
  [obj]
  (with-error-logging
    (let [doc (get-doc obj)]
      (if (nil? doc)
        (log/error "Couldn't fetch " (:type obj) "oid:" (:oid obj))
        (-> doc
            (convert-doc (:type obj))
            (assoc :tyyppi (:type obj)))))))

(defn end-indexing
  [oids last-timestamp start]
  (let [amount-indexed (count oids)
        duration (- (System/currentTimeMillis) start)]
    (log/info "The indexing queue was empty, stopping indexing and deleting indexed items from queue.")
    (log/info "Indexed" amount-indexed "objects in" (int (/ duration 1000)) "seconds.")
    (elastic-client/insert-indexing-perf amount-indexed duration start)
    (elastic-client/delete-handled-queue oids last-timestamp)
    (elastic-client/refresh-index "indexdata")))

(defn index-objects [objects]
  (log/info "Indexing" (count objects) "items")
  (let [docs-by-type (group-by :tyyppi objects)
        res (doall (map (fn [[type docs]]
                          (elastic-client/bulk-upsert type type docs)) docs-by-type))
        errors (remove false? (map :errors res))]
    (when-not (empty? errors)
      (log/error (str "Indexing failed\n" errors)))))

(defn do-index
  []
  (let [queue (elastic-client/get-queue)
        now (System/currentTimeMillis)]
    (if (empty? queue)
      (log/debug "Nothing to index.")
      (do
        (index-objects (remove nil? (doall (pmap get-coverted-doc queue))))
        (end-indexing (map :oid queue)
                      (apply max (map :timestamp queue))
                      now)))))

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

(defn get-related-koulutus [obj]
  (cond
    (= (:type obj) "organisaatio") (tarjonta-client/find-docs "koulutus" {:organisationOid (:oid obj)})
    (= (:type obj) "hakukohde") (tarjonta-client/find-docs "koulutus" {:hakukohdeOid (:oid obj)})
    (= (:type obj) "haku") (tarjonta-client/find-docs "koulutus" {:hakuOid (:oid obj)})
    (= (:type obj) "koulutus") ()))

(defjob indexing-job
  [ctx]
  (with-error-logging
    (let [last-modified (tarjonta-client/get-last-modified (elastic-client/get-last-index-time))
          now (System/currentTimeMillis)]
      (when-not (nil? last-modified)
        (let [related-koulutus (flatten (map get-related-koulutus last-modified))
              last-modified-with-related-koulutus (clojure.set/union last-modified related-koulutus)]
          (elastic-client/upsert-indexdata last-modified-with-related-koulutus)
          (elastic-client/set-last-index-time now)
          (start-indexing))))))

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
    (catch Exception e)))
