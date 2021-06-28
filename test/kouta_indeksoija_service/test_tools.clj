(ns kouta-indeksoija-service.test-tools
  (:require [cheshire.core :as cheshire]
            [clojure.test :refer [is]]
            [clojure.data :refer [diff]]
            [kouta-indeksoija-service.elastic.admin :as admin]
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
   (doseq [index (->> (admin/list-indices-and-aliases) (keys) (map name))]
     (tools/delete-index index)))
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

(defn order-primitive-arrays-for-comparison
  [json]

  (defn primitive?
    [x]
    (or (string? x) (number? x) (boolean? x)))

  (defn sort-primitive-array
    [x]
    (if (and (vector? x) (seq x) (primitive? (first x))) (sort x) x))

  (clojure.walk/postwalk sort-primitive-array json))

(defn compare-json
  [expected actual]
  (let [ordered-expected (order-primitive-arrays-for-comparison expected)
        ordered-actual (order-primitive-arrays-for-comparison actual)]
    (is (= ordered-expected ordered-actual))))
