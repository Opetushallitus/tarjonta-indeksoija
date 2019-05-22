(ns kouta-indeksoija-service.indexer.index
  (:require [kouta-indeksoija-service.indexer.docs :refer :all]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.elastic.queue :as queue]
            [kouta-indeksoija-service.elastic.docs :as docs]
            [clj-log.error-log :refer [with-error-logging]]
            [kouta-indeksoija-service.s3.s3-client :as s3-client]
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
    (queue/update-queue last-timestamp successful-oids failed-oids)
    msg))

(defn index-objects
  [objects]
  (let [docs-by-type (group-by :tyyppi objects)
        failed (apply concat (map (fn [[type docs]]
                                    (docs/upsert-docs type docs)) docs-by-type))
        failed-oids (vec (map :_id failed))]
    (log/info "Index-objects done. Total indexed: " (count objects) ", failed: " (count failed-oids))
    (when (seq failed-oids)
      (log/error (clojure.string/join ", " failed )))
    failed-oids))

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
        (let [converted-docs (remove nil? (flatten (doall (pmap get-index-doc queue))))]
          (log/info "Got converted docs! Going to index objects...")
          (let [osaamisalakuvaukset (filter #(= (:tyyppi %) "osaamisalakuvaus") converted-docs)]
            (log/info (str "Number of osaamisalakuvaukset is " (count osaamisalakuvaukset))))
          (let [failed-to-index (index-objects converted-docs)]
            (log/info "Objects indexed! Going to store pictures...")
            (store-pictures queue)
            (log/info "Pictures stored! Going to end indexing items.")
            (let [queue-oids (map :oid queue)
                  not-converted-oids (clojure.set/difference (set queue-oids)
                                                             (set (map :oid converted-docs)))
                  failed-oids (clojure.set/union not-converted-oids (set failed-to-index))
                  successful-oids (clojure.set/difference (set queue-oids)
                                                          (set failed-oids))]
              (end-indexing successful-oids
                            failed-oids
                            (apply max (map :timestamp queue))
                            start))))))))