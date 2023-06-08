(ns kouta-indeksoija-service.test-tools
  (:require [cheshire.core :as cheshire]
            [clojure.test :refer [is]]
            [clojure.walk :as walk]
            [kouta-indeksoija-service.elastic.tools :as tools]))

(defn parse
  [body]
  (try
    (cheshire/parse-string (slurp body) true)
    (catch Exception e nil)))

(defn refresh-index
  [indexname]
  (tools/refresh-index indexname))

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

(defn primitive?
  [x]
  (or (string? x) (number? x) (boolean? x)))

(defn sort-primitive-array
  [x]
  (if (and (vector? x) (seq x) (primitive? (first x))) (sort x) x))

(defn order-primitive-arrays-for-comparison
  [json]
  (walk/postwalk sort-primitive-array json))

(defn compare-json
  [expected actual]
  (let [ordered-expected (order-primitive-arrays-for-comparison expected)
        ordered-actual (order-primitive-arrays-for-comparison actual)]
    (is (= ordered-expected ordered-actual))))
