(ns kouta-indeksoija-service.s3.s3-test
  (:require [kouta-indeksoija-service.util.conf :refer [env]]
            [clojure.test :refer :all]
            [clj-test-utils.s3-mock-utils :refer :all]
            [clj-s3.s3-connect :as s3]
            [kouta-indeksoija-service.s3.s3-client :as client]
            [base64-clj.core :as b64]))

(use-fixtures :each mock-s3-fixture)

(let [koulutus-oid "1.2.3.4.567"]
 (deftest s3-client-test
   (testing "s3 client should"
     (testing "store pictures and refresh pictures"
       (is (= 0 (count (s3/list-keys "koulutus" koulutus-oid))))
       (let [obj {:oid koulutus-oid :type "koulutus"}
             pics [{:kieliUri "kieli_fi", :filename "hau.txt", :mimeType "text/plain", :base64data ""}]]
         (client/refresh-s3 obj pics))
       (is (= 0 (count (s3/list-keys "koulutus" koulutus-oid))))
       (let [obj {:oid koulutus-oid :type "koulutus"}
             pics [{:kieliUri "kieli_fi", :filename "vuh.txt", :mimeType "text/plain", :base64data (b64/encode "vuh")},
                   {:kieliUri "kieli_sv", :filename "vuh.txt", :mimeType "text/plain", :base64data (b64/encode "vuf")},
                   {:kieliUri "kieli_fi", :filename "hau.txt", :mimeType "text/plain", :base64data (b64/encode "hau")}]]
         (client/refresh-s3 obj pics))
       (is (= 3 (count (s3/list-keys "koulutus" koulutus-oid))))
       (let [obj {:oid koulutus-oid :type "koulutus"}
             pics [{:kieliUri "kieli_fi", :filename "hau.txt", :mimeType "text/plain", :base64data (b64/encode "hau")}]]
         (client/refresh-s3 obj pics))
       (is (= 1 (count (s3/list-keys "koulutus" koulutus-oid))))))))