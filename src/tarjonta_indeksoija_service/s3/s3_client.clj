(ns tarjonta-indeksoija-service.s3.s3-client
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [tarjonta-indeksoija-service.s3.s3-connect :as s3]
            [tarjonta-indeksoija-service.util.tools :refer [with-error-logging]]
            [base64-clj.core :as b64]
            [taoensso.timbre :as log]))

  (defn- upload-koulutus-pic [oid pic]
    (let [data (b64/decode-bytes (.getBytes (:base64data pic)))
          filename (:filename pic)
          kieli (:kieliUri pic)
          mimetype (:mimeType pic)]
      (log/info (str "Updating picture " filename " with lang " kieli " for koulutus " oid))
      (with-error-logging
        (s3/upload data mimetype filename oid kieli))))

  (defn- update-koulutus-pics [oid pics]
    (with-error-logging
      (let [old-pics (s3/list oid)]
        (if (not (empty? old-pics))
          (s3/delete old-pics))))
    (log/info (str "Updating " (count pics) " pics for koulutus " oid "..."))
    (doall (map #(upload-koulutus-pic oid %) pics)))

  (defn refresh-s3 [obj pics]
    (cond
      (= (:type obj) "koulutus") (update-koulutus-pics (:oid obj) pics)
      (= (:type obj) "organisaatio") true
      :else true))