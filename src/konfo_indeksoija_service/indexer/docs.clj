(ns konfo-indeksoija-service.indexer.docs
  (:require [konfo-indeksoija-service.rest.tarjonta :as t]
            [konfo-indeksoija-service.rest.organisaatio :as o]
            [konfo-indeksoija-service.converter.koulutus-converter :as kc]
            [konfo-indeksoija-service.converter.koulutus-search-data-appender :as ka]
            [konfo-indeksoija-service.converter.oppilaitos-search-data-appender :as oa]
            [konfo-indeksoija-service.converter.koulutusmoduuli-search-data-appender :as kma]
            [konfo-indeksoija-service.converter.hakukohde-converter :as hkc]
            [konfo-indeksoija-service.converter.koulutusmoduuli-converter :as kmc]))

(defmulti get-doc :type)

(defmulti convert-doc :tyyppi)

(defmulti get-pics :type)

(defmethod get-doc :default [entry]
  (t/get-doc entry))

(defmethod get-doc "koulutusmoduuli" [entry]
  (t/get-doc (assoc entry :type "komo")))

(defmethod get-doc "organisaatio" [entry]
  (o/get-doc entry))

(defmethod convert-doc :default [doc]
  doc)

(defmethod convert-doc "hakukohde" [doc]
  (hkc/convert doc))

(defmethod convert-doc "organisaatio" [doc]
  (oa/append-search-data doc))

(defmethod convert-doc "koulutusmoduuli" [doc]
  (->> doc
       kmc/convert
       kma/append-search-data))

(defmethod convert-doc "koulutus" [doc]
  (->> doc
       kc/convert
       ka/append-search-data))

(defmethod get-pics :default [entry]
  [])

(defmethod get-pics "koulutus" [entry]
  (flatten (t/get-pic entry)))

(defmethod get-pics "organisaatio" [entry]
  (let [pic (-> entry
                (o/get-doc true) ;TODO lue kuva samasta kyselystä, jonka get-coverted-doc tekee
                (:metadata)
                (:kuvaEncoded))]
    (if (not (nil? pic))
      [{:base64data pic :filename (str (:oid entry) ".jpg") :mimeType "image/jpg"}]
      [])))