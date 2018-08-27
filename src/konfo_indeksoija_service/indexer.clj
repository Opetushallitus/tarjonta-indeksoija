(ns konfo-indeksoija-service.indexer
  (:require [konfo-indeksoija-service.util.conf :refer [env job-pool]]
            [konfo-indeksoija-service.rest.tarjonta :as tarjonta-client]
            [konfo-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [konfo-indeksoija-service.elastic.elastic-client :as elastic-client]
            [konfo-indeksoija-service.converter.koulutus-converter :as koulutus-converter]
            [konfo-indeksoija-service.converter.koulutus-search-data-appender :as koulutus-search-data-appender]
            [konfo-indeksoija-service.converter.oppilaitos-search-data-appender :as oppilaitos-search-data-appender]
            [konfo-indeksoija-service.converter.koulutusmoduuli-search-data-appender :as koulutusmoduuli-search-data-appender]
            [konfo-indeksoija-service.converter.hakukohde-converter :as hakukohde-converter]
            [konfo-indeksoija-service.converter.koulutusmoduuli-converter :as koulutusmoduuli-converter]
            [clj-log.error-log :refer [with-error-logging]]
            [konfo-indeksoija-service.util.logging :refer [to-date-string]]
            [konfo-indeksoija-service.s3.s3-client :as s3-client]
            [clojure.tools.logging :as log]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.jobs :as j :refer [defjob]]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]])
  (:import (org.quartz ObjectAlreadyExistsException)))

(defmulti get-doc :type)

(defmethod get-doc :default [entry]
  (tarjonta-client/get-doc entry))

(defmethod get-doc "koulutusmoduuli" [entry]
  (tarjonta-client/get-doc (assoc entry :type "komo")))

(defmethod get-doc "organisaatio" [entry]
  (organisaatio-client/get-doc entry))

(defmulti convert-doc :tyyppi)

(defmethod convert-doc :default [doc]
  doc)

(defmethod convert-doc "hakukohde" [doc]
  (hakukohde-converter/convert doc))

(defmethod convert-doc "organisaatio" [doc]
  (oppilaitos-search-data-appender/append-search-data doc))

(defmethod convert-doc "koulutusmoduuli" [doc]
  (->> doc
       koulutusmoduuli-converter/convert
       koulutusmoduuli-search-data-appender/append-search-data))

(defmethod convert-doc "koulutus" [doc]
  (->> doc
       koulutus-converter/convert
       koulutus-search-data-appender/append-search-data))

(defmulti get-pics :type)

(defmethod get-pics :default [entry]
  [])

(defmethod get-pics "koulutus" [entry]
  (flatten (tarjonta-client/get-pic entry)))

(defmethod get-pics "organisaatio" [entry]
  (let [pic (-> entry
                (organisaatio-client/get-doc true) ;TODO lue kuva samasta kyselystä, jonka get-coverted-doc tekee
                (:metadata)
                (:kuvaEncoded))]
    (if (not (nil? pic))
      [{:base64data pic :filename (str (:oid entry) ".jpg") :mimeType "image/jpg"}]
      [])))

(defn get-converted-doc
  [obj]
  (with-error-logging
    (let [doc (get-doc obj)]
      (if (nil? doc)
        (log/error "Couldn't fetch" (:type obj) "oid:" (:oid obj))
        (-> doc
            (assoc :tyyppi (:type obj))
            (convert-doc))))))

(defn end-indexing
  [successful-oids failed-oids last-timestamp start]
  (let [duration (- (System/currentTimeMillis) start)
        msg (str "Successfully indexed " (count successful-oids) " objects in " (int (/ duration 1000)) " seconds. Total failed:" (count failed-oids))]
    (log/info msg)
    (when (seq failed-oids) (log/info "Failed oids:" (seq failed-oids)))
    (elastic-client/insert-indexing-perf (count successful-oids) duration start)
    (elastic-client/bulk-update-failed "indexdata" "indexdata" (map (fn [x] {:oid x}) (seq failed-oids)))
    (elastic-client/delete-handled-queue successful-oids last-timestamp)
    (elastic-client/refresh-index "indexdata")
    msg))

(defn index-objects
  [objects]
  (let [docs-by-type (group-by :tyyppi objects)
        res (doall (map (fn [[type docs]]
                          (elastic-client/bulk-upsert type type docs)) docs-by-type))
        errors (remove false? (map :errors res))]
    (log/info "Index-objects done. Total indexed: " (count objects))
    (when (some true? (map :errors res))
      (log/error "There were errors inserting to elastic. Refer to elastic logs for information."))))

(defn store-picture [entry]
  (let [pics (get-pics entry)]
    (if (not (empty? pics))
      (s3-client/refresh-s3 entry pics)
      (log/debug (str "No pictures for " (:type entry) (:oid entry))))))

(defn store-pictures [queue]
  (log/info "Storing pictures, queue length:" (count queue))
  (doall (map store-picture queue)))

(defn do-index
  []
  (let [queue (elastic-client/get-queue)
        start (System/currentTimeMillis)]
    (if (empty? queue)
      (log/debug "Nothing to index.")
      (do
        (log/info "Indexing" (count queue) "items from queue" (flatten (for [[k v] (group-by :type queue)] [(count v) k]) ))
        (let [converted-docs (remove nil? (doall (pmap get-converted-doc queue)))
              queue-oids (map :oid queue)
              failed-oids (clojure.set/difference (set queue-oids)
                                                  (set (map :oid converted-docs)))
              successful-oids (clojure.set/difference (set queue-oids)
                                                      (set failed-oids))]
          (log/info "Got converted docs! Going to index objects...")
          (index-objects converted-docs)
          (log/info "Objects indexed! Going to store pictures...")
          (if (not= (:s3-dev-disabled env) "true")
            (store-pictures queue)
            (log/info "Skipping store-pictures because of env value"))
          (log/info "Pictures stored! Going to end indexing items.")
          (end-indexing successful-oids
                        failed-oids
                        (apply max (map :timestamp queue))
                        start))))))

(def elastic-lock? (atom false :error-handler #(log/error %)))

(defmacro wait-for-elastic-lock
  [& body]
  `(if-not (compare-and-set! elastic-lock? false true)
     (log/debug "Indexing job already running, skipping job.")
     (try
       (do ~@body)
       (finally (reset! elastic-lock? false)))))

(defjob indexing-job
  [ctx]
  (with-error-logging
    (wait-for-elastic-lock
     (let [now (System/currentTimeMillis)
           last-modified (elastic-client/get-last-index-time)
           changes-since (tarjonta-client/get-last-modified last-modified)]
       (when-not (nil? changes-since)
         (log/info "Fetched last-modified since" (to-date-string last-modified)", containing" (count changes-since) "changes.")
         (let [related-koulutus (flatten (pmap tarjonta-client/get-related-koulutus changes-since))
               last-modified-with-related-koulutus (clojure.set/union changes-since related-koulutus)]
           (if-not (empty? related-koulutus)
             (log/info "Fetched" (count related-koulutus) "related koulutukses for previous changes"))
           (elastic-client/upsert-indexdata last-modified-with-related-koulutus)
           (elastic-client/set-last-index-time now)
           (do-index)))))))

(defn start-indexer-job
  ([] (start-indexer-job (:cron-string env)))
  ([cronstring]
  (log/info "Starting indexer job!")
  (let [job (j/build
             (j/of-type indexing-job)
             (j/with-identity "jobs.index.1"))
        trigger (t/build
                 (t/with-identity (t/key "crontirgger"))
                 (t/start-now)
                 (t/with-schedule
                  (schedule (cron-schedule cronstring))))]
    (log/info (str "Starting indexer with cron schedule " cronstring)
              (qs/schedule job-pool job trigger)))))

(defn reset-jobs
  []
  (reset! elastic-lock? false)
  (qs/clear! job-pool))

(defn start-stop-indexer
  [start?]
  (try
    (if start?
      (do
        (log/info "Starting indexer job")
        (start-indexer-job))
      (do
        (log/info "Stopping all jobs and clearing job pool.")
        (reset-jobs)
        ))
    (catch ObjectAlreadyExistsException e "Indexer already running.")
    (catch Exception e)))
