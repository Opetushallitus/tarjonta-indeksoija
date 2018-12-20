(ns konfo-indeksoija-service.indexer.index
  (:require [konfo-indeksoija-service.indexer.docs :refer :all]
            [konfo-indeksoija-service.util.conf :refer [env]]
            [konfo-indeksoija-service.elastic.perf :as perf]
            [konfo-indeksoija-service.elastic.queue :as queue]
            [konfo-indeksoija-service.elastic.docs :as docs]
            [konfo-indeksoija-service.elastic.tools :as tools]
            [konfo-indeksoija-service.rest.kouta :as kouta]
            [konfo-indeksoija-service.search-data.koulutus-kouta :as kouta-sd]
            [clj-log.error-log :refer [with-error-logging]]
            [konfo-indeksoija-service.s3.s3-client :as s3-client]
            [clojure.tools.logging :as log]))

(defn get-index-doc
  [entry]
  (with-error-logging
    (let [doc (get-doc entry)]
      (if (nil? doc)
        (log/error "Couldn't fetch" (:type entry) "oid:" (:oid entry))
        (if (= (:type entry) "osaamisalakuvaus")
          (-> doc
              (assoc :eperuste-oid (:oid entry))
              (assoc :tyyppi (:type entry))
              (convert-doc))
          (-> doc
              (assoc :tyyppi (:type entry))
              (convert-doc)))))))

(defn end-indexing
  [successful-oids failed-oids last-timestamp start]
  (let [duration (- (System/currentTimeMillis) start)
        msg (str "Successfully indexed " (count successful-oids) " objects in " (int (/ duration 1000)) " seconds. Total failed:" (count failed-oids))]
    (log/info msg)
    (when (seq failed-oids) (log/info "Failed oids:" (seq failed-oids)))
    (perf/insert-indexing-perf (count successful-oids) duration start)
    (queue/update-queue last-timestamp successful-oids failed-oids)
    msg))

(defn index-objects
  [objects]
  (let [docs-by-type (group-by :tyyppi objects)
        res (doall (map (fn [[type docs]]
                          (docs/upsert-docs type docs)) docs-by-type))]
    (log/info "Index-objects done. Total indexed: " (count objects))
    (when (some true? (map :errors res))
      (log/error "There were errors inserting to elastic. Refer to elastic logs for information."))))

(defn store-picture [entry]
  (let [pics (get-pics entry)]
    (if (not (empty? pics))
      (s3-client/refresh-s3 entry pics)
      (log/debug (str "No pictures for " (:type entry) (:oid entry))))))

(defn store-pictures [queue]
  (if (not= (:s3-dev-disabled env) "true")
    (do (log/info "Storing pictures, queue length:" (count queue))
        (doall (map store-picture queue)))
    (log/info "Skipping store-pictures because of env value")))

(defn do-index
  []
  (let [queue (queue/get-queue)
        start (System/currentTimeMillis)]
    (if (empty? queue)
      (log/debug "Nothing to index.")
      (do
        (log/info "Indexing" (count queue) "items from queue" (flatten (for [[k v] (group-by :type queue)] [(count v) k]) ))
        (let [converted-docs (remove nil? (doall (pmap get-index-doc queue)))
              queue-oids (map :oid queue)
              failed-oids (clojure.set/difference (set queue-oids)
                                                  (set (map #(if (= (:tyyppi %1) "osaamisalakuvaus")
                                                               (:eperuste-oid %1)
                                                               (:oid %1))
                                                            converted-docs)))
              successful-oids (clojure.set/difference (set queue-oids)
                                                      (set failed-oids))]
          (log/info "Got converted docs! Going to index objects...")
          (if (some #(= (:tyyppi %1) "osaamisalakuvaus") converted-docs)
            (index-objects (flatten (map #(if (= (:tyyppi %1) "osaamisalakuvaus") (:docs %1) %1) converted-docs)))
            (index-objects converted-docs))
          (log/info "Objects indexed! Going to store pictures...")
          (store-pictures queue)
          (log/info "Pictures stored! Going to end indexing items.")
          (end-indexing successful-oids
                        failed-oids
                        (apply max (map :timestamp queue))
                        start))))))

(defn index-kouta-koulutus [oid]
  (let [koulutus-doc (kouta/get-koulutus oid)
        koulutus (kouta-sd/append-search-data koulutus-doc)]
    (docs/upsert-docs "koulutus-kouta" [koulutus])))