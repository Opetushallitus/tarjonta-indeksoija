(ns kouta-indeksoija-service.indexer.docs
  (:require [kouta-indeksoija-service.rest.organisaatio :as o]
            [kouta-indeksoija-service.rest.eperuste :as e]
            [kouta-indeksoija-service.converter.eperuste :as ec]
            [kouta-indeksoija-service.converter.osaamisalakuvaus :as oc]
            [kouta-indeksoija-service.search-data.oppilaitos :as oa]))

(defmulti get-doc :type)

(defmulti convert-doc :tyyppi)

(defmulti get-pics :type)

(defmethod get-doc :default [entry]
  nil)

(defmethod get-doc "organisaatio" [entry]
  (o/get-doc entry))

(defmethod get-doc "eperuste" [entry]
  (when-some [eperuste (e/get-doc entry)]
    {:eperuste eperuste :osaamisalat (e/get-osaamisalakuvaukset (:oid entry))}))

(defmethod convert-doc :default [doc]
  doc)

(defmethod convert-doc "eperuste" [doc]
  (flatten (conj (oc/convert (:osaamisalat doc)) (ec/convert (:eperuste doc)))))

(defmethod convert-doc "organisaatio" [doc]
  (oa/append-search-data doc))

(defmethod get-pics :default [entry]
  [])

(defmethod get-pics "organisaatio" [entry]
  (let [pic (-> entry
                (o/get-doc true) ;TODO lue kuva samasta kyselystä, jonka get-coverted-doc tekee
                (:metadata)
                (:kuvaEncoded))]
    (if (not (nil? pic))
      [{:base64data pic :filename (str (:oid entry) ".jpg") :mimeType "image/jpg"}]
      [])))