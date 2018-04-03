(ns konfo-indeksoija-service.s3-test
  (:require [konfo-indeksoija-service.conf :refer [env]]
            [midje.sweet :refer :all]
            [konfo-indeksoija-service.s3.s3-connect :as s3]
            [konfo-indeksoija-service.s3.s3-client :as client]
            [base64-clj.core :as b64])
  (:import (io.findify.s3mock S3Mock)
           (com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration)
           (com.amazonaws.services.s3 AmazonS3ClientBuilder)
           (com.amazonaws.auth AWSStaticCredentialsProvider AnonymousAWSCredentials)
           (com.amazonaws.services.s3.model CreateBucketRequest)
           (java.io IOException)
           (java.net Socket)))

(defn is-free-local-port [port]
  (try
      (let [socket (new Socket "127.0.0.1" port)]
        (.close socket)
        false)
    (catch IOException ioe true)))

(defn find-free-local-port []
  (let [range (range 1024 60000)
        port (nth range (rand-int (count range)))]
    (if (is-free-local-port port)
      port
      (find-free-local-port))))

(let [port (find-free-local-port)
      mock (-> (S3Mock/create port)
               (.start))
      koulutus-oid "1.2.3.4.567"
      s3-url (str "http://localhost:" port)
      endpoint-config (new AwsClientBuilder$EndpointConfiguration s3-url, (:s3-region env))]

  (let [client (-> (AmazonS3ClientBuilder/standard)
                                 (.withPathStyleAccessEnabled true)
                                 (.withEndpointConfiguration endpoint-config)
                                 (.withCredentials (new AWSStaticCredentialsProvider (new AnonymousAWSCredentials)))
                                 (.build))]
    (reset! s3/s3-client client)
    (.createBucket @s3/s3-client (new CreateBucketRequest (:s3-bucket env) (:s3-region env)))

    (facts "s3 connect should"
      (fact "return empty list"
        (count (s3/list "koulutus" koulutus-oid)) => 0)

      (fact "upload documents"
        (s3/upload (.getBytes "moi") "text/plain" "moi.txt" "koulutus" koulutus-oid "kieli_fi")
        (s3/upload (.getBytes "hej") "text/plain" "moi.txt" "koulutus" koulutus-oid "kieli_sv")
        (s3/upload (.getBytes "terve") "text/plain" "terve.txt" "koulutus" koulutus-oid "kieli_fi")
        (count (s3/list "koulutus" koulutus-oid)) => 3)

      (fact "list documents in specific language"
        (count (s3/list "koulutus" koulutus-oid "kieli_fi")) => 2)

      (fact "delete documents"
        (let [resp (s3/list "koulutus" koulutus-oid)]
          (s3/delete resp))
        (count (s3/list "koulutus" koulutus-oid)) => 0))

    (facts "s3 client should"
      (fact "store pictures"
        (count (s3/list "koulutus" koulutus-oid)) => 0
        (let [obj {:oid koulutus-oid :type "koulutus"}
              pics [{:kieliUri "kieli_fi", :filename "vuh.txt", :mimeType "text/plain", :base64data (b64/encode "vuh")},
                    {:kieliUri "kieli_sv", :filename "vuh.txt", :mimeType "text/plain", :base64data (b64/encode "vuf")},
                    {:kieliUri "kieli_fi", :filename "hau.txt", :mimeType "text/plain", :base64data (b64/encode "hau")}]]
          (client/refresh-s3 obj pics))
        (count (s3/list "koulutus" koulutus-oid)) => 3)
      (fact "refresh all pictures"
        (count (s3/list "koulutus" koulutus-oid)) => 3
        (let [obj {:oid koulutus-oid :type "koulutus"}
              pics [{:kieliUri "kieli_fi", :filename "hau.txt", :mimeType "text/plain", :base64data (b64/encode "hau")}]]
          (client/refresh-s3 obj pics))
        (count (s3/list "koulutus" koulutus-oid)) => 1))))