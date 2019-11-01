(ns kouta-indeksoija-service.util.tools
  (:require [clojure.string :refer [blank? split lower-case]]))

(defn uuid
  []
  (.toString (java.util.UUID/randomUUID)))

(defn get-id
  [doc]
  (or (:oid doc) (:id doc)))

(defn comma-separated-string->vec
  [s]
  (vec (remove blank? (some-> s (split #",")))))
