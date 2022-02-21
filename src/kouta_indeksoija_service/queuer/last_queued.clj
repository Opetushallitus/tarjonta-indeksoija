(ns kouta-indeksoija-service.queuer.last-queued
  (:require [clj-log.error-log :refer [with-error-logging with-error-logging-value]]
            [clj-elasticsearch.elastic-connect :as e]
            [clj-elasticsearch.elastic-utils :as u]))

(defonce index-name "lastqueued")

(defn set-last-queued-time
  [timestamp]
  (let [url (u/elastic-url index-name "_doc" "1" "_update")
        query {:doc {:timestamp timestamp} :doc_as_upsert true}]
    (u/elastic-post url query)))

(defn get-last-queued-time
  []
  (with-error-logging-value
    (System/currentTimeMillis)
    (let [res (e/get-document index-name "1")]
      (if (:found res)
        (get-in res [:_source :timestamp])
        (System/currentTimeMillis)))))