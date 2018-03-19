(ns tarjonta-indeksoija-service.s3.s3-connect
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [taoensso.timbre :as log])
  (:import (com.amazonaws.services.s3 AmazonS3ClientBuilder)
           (com.amazonaws.services.s3.model ObjectMetadata PutObjectRequest ListObjectsV2Request DeleteObjectsRequest)
           (com.amazonaws.auth InstanceProfileCredentialsProvider)
           (java.io ByteArrayInputStream)))

  (def s3-client (atom nil))

  (defn init-s3-client []
    (when (not (Boolean/valueOf (:test environ.core/env)))
      (reset! s3-client (-> (AmazonS3ClientBuilder/standard)
                            (.withCredentials (InstanceProfileCredentialsProvider/createAsyncRefreshingProvider true))
                            (.withRegion (:s3-region env))
                            (.build)))))

  (defn- gen-path [& path-parts] (clojure.string/join "/" path-parts))
  (defn- gen-key [filename & path-parts] (str (apply gen-path path-parts) "/" filename))

  (defn upload
     [bytes mimetype filename & path-parts]
     (let [key (apply gen-key filename path-parts)
           metadata (-> (new ObjectMetadata)
                           (.setContentLength (count bytes)))
           request (new PutObjectRequest (:s3-bucket env) key (new ByteArrayInputStream bytes) metadata)]
       (log/info key)
       (some? (.putObject @s3-client request))))

  (defn list [& path-parts]
     (let [request (-> (new ListObjectsV2Request)
                       (.withBucketName (:s3-bucket env))
                       (.withPrefix (apply gen-path path-parts)))]
       (map #(.getKey %) (.getObjectSummaries (.listObjectsV2 @s3-client request)))))

  (defn delete [keys]
     (let [request (-> (new DeleteObjectsRequest (:s3-bucket env))
                       (.withKeys (into-array keys)))]
       (.size (.getDeletedObjects (.deleteObjects @s3-client request)))))