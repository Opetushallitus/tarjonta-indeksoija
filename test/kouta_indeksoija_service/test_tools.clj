(ns kouta-indeksoija-service.test-tools
  (:require [cheshire.core :as cheshire]
            [clojure.test :refer [is]]
            [clojure.data :refer [diff]]
            [kouta-indeksoija-service.elastic.tools :as tools]))

(defn parse
  [body]
  (try
    (cheshire/parse-string (slurp body) true)
    (catch Exception e nil)))

(defn refresh-index
  [indexname]
  (tools/refresh-index indexname))

(defn reset-test-data
  ([reset-jobs?]
   ;(when reset-jobs? (j/reset-jobs))
   (tools/delete-index "eperuste")
   (tools/delete-index "osaamisalakuvaus")
   (tools/delete-index "organisaatio")
   (tools/delete-index "palaute")
   (tools/delete-index "lastqueued"))
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

(defn debug-pretty
  [json]
  (println (cheshire/generate-string json {:pretty true})))

(defn compare-json
  [expected actual]
  (let [difference (diff expected actual)]
    (is (= nil (first difference)))
    (is (= nil (second difference)))
    (is (= expected actual))))
