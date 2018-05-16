(ns konfo-indeksoija-service.indexer
  (:require [konfo-indeksoija-service.conf :refer [env job-pool]]
            [konfo-indeksoija-service.tarjonta-client :as tarjonta-client]
            [konfo-indeksoija-service.organisaatio-client :as organisaatio-client]
            [konfo-indeksoija-service.elastic-client :as elastic-client]
            [konfo-indeksoija-service.converter.koulutus-converter :as koulutus-converter]
            [konfo-indeksoija-service.converter.koulutus-search-data-appender :as koulutus-search-data-appender]
            [konfo-indeksoija-service.converter.hakukohde-converter :as hakukohde-converter]
            [clj-log.error-log :refer [with-error-logging]]
            [konfo-indeksoija-service.util.logging :refer [to-date-string]]
            [konfo-indeksoija-service.s3-client :as s3-client]
            [clojure.tools.logging :as log]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.jobs :as j :refer [defjob]]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]])
  (:import (org.quartz ObjectAlreadyExistsException)))

(defn convert-doc
  [doc type]
  (cond
    (.contains type "koulutus") (->> doc
                                     koulutus-converter/convert
                                     koulutus-search-data-appender/append-search-data)
    (.contains type "hakukohde") (hakukohde-converter/convert doc)
    :else doc))

(defn- get-doc [obj]
  (cond
    (= (:type obj) "organisaatio") (organisaatio-client/get-doc obj)
    :else (tarjonta-client/get-doc obj)))

(defn get-converted-doc
  [obj]
  (with-error-logging
    (let [doc (get-doc obj)]
      (if (nil? doc)
        (log/error "Couldn't fetch" (:type obj) "oid:" (:oid obj))
        (-> doc
            (convert-doc (:type obj))
            (assoc :tyyppi (:type obj)))))))

(defn end-indexing
  [oids successful-oids failed-oids last-timestamp start]
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

(defn store-koulutus-pics [obj]
  (let [pics (flatten (tarjonta-client/get-pic obj))]
    (if (not (empty? pics))
      (s3-client/refresh-s3 obj pics)
      (log/debug (str "No pictures for koulutus " (:oid obj))))))

(defn store-organisaatio-pic [obj]
  (let [pic (-> obj
                (organisaatio-client/get-doc true)          ;TODO lue kuva samasta kyselystä, jonka get-coverted-doc tekee
                (:metadata)
                (:kuvaEncoded))]
    (if (not (nil? pic))
      (s3-client/refresh-s3 obj [{:base64data pic :filename (str (:oid obj) ".jpg") :mimeType "image/jpg"}])
      (log/debug (str "No picture for organisaatio " (:oid obj))))))

(defn store-pictures
  [queue]
  (log/info "Storing pictures, queue length:" (count queue))
  (let [store-pic-fn (fn [obj] (cond
                                 (= (:type obj) "koulutus") (store-koulutus-pics obj)
                                 (= (:type obj) "organisaatio") (store-organisaatio-pic obj)
                                 :else true))]
    (doall (map store-pic-fn queue))))

(defn do-index
  []
  (let [queue (elastic-client/get-queue)
        start (System/currentTimeMillis)]
    (if (empty? queue)
      (log/debug "Nothing to index.")
      (do
        (log/info "Indexing" (count queue) "items")
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
          (end-indexing queue-oids
                        successful-oids
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
             (log/info "Fetched" (count related-koulutus) "related koulutukses for previous changes:"))
           (elastic-client/upsert-indexdata last-modified-with-related-koulutus)
           (elastic-client/set-last-index-time now)
           (do-index)))))))

(defn start-indexer-job
  []
  (log/info "Starting indexer job!")
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
