(ns mocks.externals-mock
  (:require [tarjonta-indeksoija-service.test-tools :as tools]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta]
            [tarjonta-indeksoija-service.organisaatio-client :as organisaatio]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [tarjonta-indeksoija-service.api]
            [tarjonta-indeksoija-service.indexer]
            [base64-clj.core :as b64]))

(defn get-doc
  [obj]
  (cond
    (.contains (:type obj) "hakukohde") (tools/parse-body (str "test/resources/hakukohteet/" (:oid obj) ".json"))
    (.contains (:type obj) "koulutus") (tools/parse-body (str "test/resources/koulutukset/" (:oid obj) ".json"))
    (.contains (:type obj) "haku") (tools/parse-body (str "test/resources/haut/" (:oid obj) ".json"))
    (.contains (:type obj) "organisaatio") (tools/parse (str "test/resources/organisaatiot/" (:oid obj) ".json"))))

(defn get-pic
  [obj]
  (cond
    (.contains (:type obj) "koulutus") [{:kieliUri "kieli_fi", :filename "vuh.txt", :mimeType "text/plain", :base64data (b64/encode "vuh")},
                                        {:kieliUri "kieli_sv", :filename "vuh.txt", :mimeType "text/plain", :base64data (b64/encode "vuf")},
                                        {:kieliUri "kieli_fi", :filename "hau.txt", :mimeType "text/plain", :base64data (b64/encode "hau")}]))

(defn get-last-modified
  [since]
  (if (= 0 since)
    [{:type "hakukohde" :oid "1.2.246.562.20.99178639649"}
     {:type "koulutus" :oid "1.2.246.562.17.81687174185"}]
    []))

(defn get-related-koulutus
  [obj]
  [])

(defn get-hakukohteet-for-koulutus
  [koulutus-oid]
  [])

(defn get-haut-by-oids
  [oid-list]
  [])

(defn refresh-s3
  [obj pics]
  true)

(defn reindex-mock
  [index oid]
  (elastic-client/upsert-indexdata
   [{:type index :oid oid}]))

(defmacro with-externals-mock
  [& body]
  `(with-redefs [tarjonta-indeksoija-service.api/reindex
                 mocks.externals-mock/reindex-mock

                 tarjonta-indeksoija-service.tarjonta-client/get-last-modified
                 mocks.externals-mock/get-last-modified

                 tarjonta-indeksoija-service.tarjonta-client/get-related-koulutus
                 mocks.externals-mock/get-related-koulutus

                 tarjonta-indeksoija-service.indexer/get-doc
                 mocks.externals-mock/get-doc

                 tarjonta-indeksoija-service.tarjonta-client/get-doc
                 mocks.externals-mock/get-doc

                 tarjonta-indeksoija-service.tarjonta-client/get-pic
                 mocks.externals-mock/get-pic

                 tarjonta-indeksoija-service.organisaatio-client/get-doc
                 mocks.externals-mock/get-doc

                 tarjonta-indeksoija-service.tarjonta-client/get-hakukohteet-for-koulutus
                 mocks.externals-mock/get-hakukohteet-for-koulutus

                 tarjonta-indeksoija-service.tarjonta-client/get-haut-by-oids
                 mocks.externals-mock/get-haut-by-oids

                 tarjonta-indeksoija-service.s3.s3-client/refresh-s3
                 mocks.externals-mock/refresh-s3]
     (do ~@body)))
