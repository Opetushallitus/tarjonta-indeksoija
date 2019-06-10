(ns kouta-indeksoija-service.test-tools
  (:require [cheshire.core :as cheshire]
            [kouta-indeksoija-service.elastic.tools :as tools]
            [kouta-indeksoija-service.elastic.queue :as queue]))

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

(defn refresh-index
  [indexname]
  (tools/refresh-index indexname))

(defn reset-test-data
  ([reset-jobs?]
   ;(when reset-jobs? (j/reset-jobs))
   (tools/delete-index "indexdata")
   (tools/delete-index "eperuste")
   (tools/delete-index "osaamisalakuvaus")
   (tools/delete-index "organisaatio")
   (tools/delete-index "palaute")
   (tools/delete-index "lastindex"))
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