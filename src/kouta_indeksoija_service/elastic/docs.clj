(ns kouta-indeksoija-service.elastic.docs
  (:require [kouta-indeksoija-service.elastic.tools :refer [bulk-upsert get-by-id]]))

(defn upsert-docs
  [type docs]
  (bulk-upsert type type docs))

(defn get-doc
  [type id]
  (get-by-id type type id))

(defmacro get-organisaatio [oid]
  `(get-by-id "organisaatio" "organisaatio" ~oid))

(defmacro get-eperuste [oid]
  `(get-by-id "eperuste" "eperuste" ~oid))
