(ns konfo-indeksoija-service.s3-client
  (:require [konfo-indeksoija-service.conf :refer [env]]
            [clj-s3.s3-connect :as s3]
            [clj-log.error-log :refer [with-error-logging]]
            [base64-clj.core :as b64]
            [clojure.tools.logging :as log]))

  (defn init-s3-connection []
    (if-let [s3-region (:s3-region env)]
      (intern 'clj-s3.s3-connect 's3-region s3-region)
      (throw (IllegalStateException. "Could not read s3-region from configuration!")))
    (if-let [s3-bucket (:s3-bucket env)]
      (intern 'clj-s3.s3-connect 's3-bucket s3-bucket)
      (throw (IllegalStateException. "Could not read s3-bucket from configuration!")))
    (log/info "Initializing s3 client with s3-region:" (:s3-region env) ", s3-bucket:" (:s3-bucket env))
    (s3/init-s3-client))

  (defn- upload-pic [oid type pic]
    (log/info (str "PICTURE SIZE " (count (.getBytes (:base64data pic)))))
    (log/info (:base64data pic))
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
      (let [old-pics (s3/list-keys type oid)]
        (if (not (empty? old-pics))
          (s3/delete old-pics))))
    (log/info (str "Updating " (count pics) " pics for " type " " oid "..."))
    (doall (map #(upload-pic oid type %) pics)))

  (defn refresh-s3 [obj pics]
    (cond
      (= (:type obj) "koulutus") (update-pics (:oid obj) (:type obj) pics)
      (= (:type obj) "organisaatio") (update-pics (:oid obj) (:type obj) pics)
      :else true))