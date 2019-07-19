(ns kouta-indeksoija-service.indexer.organisaatio.pictures
  (:require [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.s3.s3-client :as s3]
            [kouta-indeksoija-service.rest.organisaatio :as o]
            [clojure.tools.logging :as log]))

(defn- get-pics
  [oid]
  (if-let [pic (-> oid
                   (o/get-doc true) ;TODO lue kuva samasta kyselystä kuin indeksointi?
                   (:metadata)
                   (:kuvaEncoded))]
    [{:base64data pic :filename (str oid ".jpg") :mimeType "image/jpg"}]
    []))

(defn store-picture
  [oid]
  (if-let [pics (not-empty (get-pics oid))]
    (s3/refresh-s3 oid "organisaatio" pics)
    (log/info "Organisaatiolla " oid " ei ole kuvia")))

(defn store-pictures
  [oids]
  (when (not-empty oids)
    (if (not= (:s3-dev-disabled env) "true")
      (let [start (. System (currentTimeMillis))]
        (log/info "Tallennetaan indeksoitavien organisaatioiden kuvat s3:een")
        (doseq [oid oids] (store-picture oid))
        (log/info (count oids) " organisaation kuvien tallennus kesti " (- (. System (currentTimeMillis)) start) " ms."))
      (log/info "Ei tallenneta organisaatioiden kuvia s3:een, koska s3-dev-disabled=true"))))