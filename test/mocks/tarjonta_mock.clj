(ns mocks.tarjonta-mock
  (:require [tarjonta-indeksoija-service.test-tools :as tools]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta]
            [tarjonta-indeksoija-service.organisaatio-client :as organisaatio]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]))

(defn get-doc
  [obj]
  (cond
    (.contains (:type obj) "hakukohde") (tools/parse-body (str "test/resources/hakukohteet/" (:oid obj) ".json"))
    (.contains (:type obj) "koulutus") (tools/parse-body (str "test/resources/koulutukset/" (:oid obj) ".json"))
    (.contains (:type obj) "haku") (tools/parse-body (str "test/resources/haut/" (:oid obj) ".json"))
    (.contains (:type obj) "organisaatio") (tools/parse (str "test/resources/organisaatiot/" (:oid obj) ".json"))))

(defn get-last-modified
  [since]
  (if (= 0 since)
    [{:type "hakukohde" :oid "1.2.246.562.20.99178639649"}
     {:type "koulutus" :oid "1.2.246.562.17.81687174185"}]
    []))

(defn get-related-koulutus
  [obj]
  [])

(defmacro with-tarjonta-mock
  [& body]
  `(with-redefs [tarjonta/get-doc mocks.tarjonta-mock/get-doc
                 tarjonta/get-last-modified mocks.tarjonta-mock/get-last-modified
                 tarjonta/get-related-koulutus mocks.tarjonta-mock/get-related-koulutus]
     (do ~@body)))

(defmacro with-organisaatio-mock
  [& body]
  `(with-redefs [organisaatio/get-doc mocks.tarjonta-mock/get-doc]
     (do ~@body)))

