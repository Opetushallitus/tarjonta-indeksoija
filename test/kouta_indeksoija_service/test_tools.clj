(ns kouta-indeksoija-service.test-tools
  (:require [cheshire.core :as cheshire]
            [kouta-indeksoija-service.elastic.tools :as tools]
            [kouta-indeksoija-service.elastic.queue :as queue]
            [kouta-indeksoija-service.indexer.job :as j]))

(defn parse
  [body]
  (try
    (cheshire/parse-string (slurp body) true)
    (catch Exception e nil)))

(defn parse-body
  [body]
  (-> body
    parse
    :result))

(defn block-until-indexed
  [timeout]
  (let [start (System/currentTimeMillis)]
    (queue/refresh-queue)
    (while (and (> timeout (- (System/currentTimeMillis) start))
                (not (empty? (queue/get-queue))))
      (Thread/sleep 1000))))

(defn block-until-latest-in-queue
  [timeout]
  (let [start (System/currentTimeMillis)]
    (queue/refresh-queue)
    (while (and (> timeout (- (System/currentTimeMillis) start))
             (empty? (queue/get-queue)))
      (Thread/sleep 1000))))

(defn refresh-and-wait
  [indexname timeout]
  (tools/refresh-index indexname)
  (Thread/sleep timeout))

(defn reset-test-data
  ([reset-jobs?]
   (when reset-jobs? (j/reset-jobs))
   (tools/delete-index "indexdata")
   (tools/delete-index "koulutusmoduuli")
   (tools/delete-index "eperuste")
   (tools/delete-index "organisaatio")
   (tools/delete-index "lastindex")
   (Thread/sleep 1000))
  ([]
   (reset-test-data true)))

(defn in?
  [e coll]
  (some #(= e %) coll))

(defn contains-same-elements-in-any-order?
  [expected actual]
  (and
   (not-empty actual)
   (= (count actual) (count expected))
   (every? #(in? % expected) actual)))

(defn contains-elements-in-any-order?
  [expected actual]
  (and
   (not-empty actual)
   (every? #(in? % expected) actual)))