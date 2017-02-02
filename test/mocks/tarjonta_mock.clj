(ns mocks.tarjonta-mock
  (:require [tarjonta-indeksoija-service.test-tools :as tools]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]))

(defn get-doc
  [obj]
  (cond
    (.contains (:type obj) "hakukohde") (tools/parse-body (str "test/resources/hakukohteet/" (:oid obj) ".json"))
    (.contains (:type obj) "koulutus") (tools/parse-body (str "test/resources/koulutukset/" (:oid obj) ".json"))
    (.contains (:type obj) "haku") (tools/parse-body (str "test/resources/haut/" (:oid obj) ".json"))))

(defmacro with-tarjonta-mock
  [& body]
  `(with-redefs [tarjonta/get-doc mocks.tarjonta-mock/get-doc
                 tarjonta/get-last-modified mocks.tarjonta-mock/get-last-modified]
     (do ~@body)))

(defn get-last-modified
  [since]
  [])