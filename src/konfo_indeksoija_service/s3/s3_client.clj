(ns konfo-indeksoija-service.s3.s3-client
  (:require [konfo-indeksoija-service.conf :refer [env]]
            [konfo-indeksoija-service.s3.s3-connect :as s3]
            [konfo-indeksoija-service.util.tools :refer [with-error-logging]]
            [base64-clj.core :as b64]
            [taoensso.timbre :as log]))

  (defn- upload-pic [oid type pic]
    (let [data (b64/decode-bytes (.getBytes (:base64data pic)))
          filename (:filename pic)
          kieli (:kieliUri pic)
          mimetype (:mimeType pic)]
      (log/info (str "Updating picture " filename (if (nil? kieli) "" (str " with lang " kieli)) " for " type " " oid))
      (with-error-logging false
        (if (nil? kieli)
          (s3/upload data mimetype filename type oid)
          (s3/upload data mimetype filename type oid kieli)))))

  (defn- update-pics [oid type pics]
    (with-error-logging
      (let [old-pics (s3/list type oid)]
        (if (not (empty? old-pics))
          (s3/delete old-pics))))
    (log/info (str "Updating " (count pics) " pics for " type " " oid "..."))
    (doall (map #(upload-pic oid type %) pics)))

  (defn refresh-s3 [obj pics]
    (cond
      (= (:type obj) "koulutus") (update-pics (:oid obj) (:type obj) pics)
      (= (:type obj) "organisaatio") (update-pics (:oid obj) (:type obj) pics)
      :else true))