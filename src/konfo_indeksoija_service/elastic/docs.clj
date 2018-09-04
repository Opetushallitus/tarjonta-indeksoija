(ns konfo-indeksoija-service.elastic.docs
  (:require [konfo-indeksoija-service.elastic.tools :refer [bulk-upsert get-by-id]]))

(defn upsert-docs [type docs]
  (bulk-upsert type type docs))

(defmacro get-hakukohde [oid]
  `(get-by-id "hakukohde" "hakukohde" ~oid))

(defmacro get-koulutus [oid]
  `(dissoc (get-by-id "koulutus" "koulutus" ~oid) :searchData))

(defmacro get-koulutus-with-searh-data [oid]
  `(get-by-id "koulutus" "koulutus" ~oid))

(defmacro get-haku [oid]
  `(get-by-id "haku" "haku" ~oid))

(defmacro get-organisaatio [oid]
  `(get-by-id "organisaatio" "organisaatio" ~oid))

(defmacro get-koulutusmoduuli [oid]
  `(get-by-id "koulutusmoduuli" "koulutusmoduuli" ~oid))

(defmacro get-eperuste [oid]
  `(get-by-id "eperuste" "eperuste" ~oid))