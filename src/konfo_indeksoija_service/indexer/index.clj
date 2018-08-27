(ns konfo-indeksoija-service.indexer.index
  (:require [konfo-indeksoija-service.indexer.docs :refer :all]
            [konfo-indeksoija-service.util.conf :refer [env]]
            [konfo-indeksoija-service.elastic.elastic-client :as elastic-client]
            [clj-log.error-log :refer [with-error-logging]]
            [konfo-indeksoija-service.s3.s3-client :as s3-client]
            [clojure.tools.logging :as log]))

(defn get-index-doc
  [entry]
  (with-error-logging
    (let [doc (get-doc entry)]
      (if (nil? doc)
        (log/error "Couldn't fetch" (:type entry) "oid:" (:oid entry))
        (-> doc
            (assoc :tyyppi (:type entry))
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
        (let [converted-docs (remove nil? (doall (pmap get-index-doc queue)))
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