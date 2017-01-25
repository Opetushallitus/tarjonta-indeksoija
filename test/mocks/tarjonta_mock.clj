(ns mocks.tarjonta-mock
  (:require [tarjonta-indeksoija-service.test-tools :as tools]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta]))

(defn get-doc
  [obj]
  (cond
    (.contains (:type obj) "hakukohde") (:result (tools/parse-body (str "test/resources/hakukohteet/" (:oid obj) ".json")))
    (.contains (:type obj) "koulutus") (:result (tools/parse-body (str "test/resources/koulutukset/" (:oid obj) ".json")))
    (.contains (:type obj) "haku") (:result (tools/parse-body (str "test/resources/haut/" (:oid obj) ".json")))))

(defmacro with-tarjonta-mock
  [& body]
  `(with-redefs [tarjonta/get-doc mocks.tarjonta-mock/get-doc]
     (do ~@body)))
