(ns kouta-indeksoija-service.s3-test
  (:require [kouta-indeksoija-service.util.conf :refer [env]]
            [midje.sweet :refer :all]
            [clj-test-utils.s3-mock-utils :refer :all]
            [clj-test-utils.test-utils :refer [init-test-logging]]
            [clj-s3.s3-connect :as s3]
            [kouta-indeksoija-service.s3.s3-client :as client]
            [base64-clj.core :as b64]))

(init-test-logging)

(defonce koulutus-oid "1.2.3.4.567")

(with-state-changes [(before :facts (init-s3-mock))
                     (after :facts (stop-s3-mock))]
    (facts "s3 client should"
      (fact "store pictures and refresh pictures"
        (count (s3/list-keys "koulutus" koulutus-oid)) => 0
        (let [obj {:oid koulutus-oid :type "koulutus"}
              pics [{:kieliUri "kieli_fi", :filename "hau.txt", :mimeType "text/plain", :base64data ""}]]
          (client/refresh-s3 obj pics))
        (count (s3/list-keys "koulutus" koulutus-oid)) => 0
        (let [obj {:oid koulutus-oid :type "koulutus"}
              pics [{:kieliUri "kieli_fi", :filename "vuh.txt", :mimeType "text/plain", :base64data (b64/encode "vuh")},
                    {:kieliUri "kieli_sv", :filename "vuh.txt", :mimeType "text/plain", :base64data (b64/encode "vuf")},
                    {:kieliUri "kieli_fi", :filename "hau.txt", :mimeType "text/plain", :base64data (b64/encode "hau")}]]
          (client/refresh-s3 obj pics))
        (count (s3/list-keys "koulutus" koulutus-oid)) => 3
        (let [obj {:oid koulutus-oid :type "koulutus"}
              pics [{:kieliUri "kieli_fi", :filename "hau.txt", :mimeType "text/plain", :base64data (b64/encode "hau")}]]
          (client/refresh-s3 obj pics))
        (count (s3/list-keys "koulutus" koulutus-oid)) => 1)))
