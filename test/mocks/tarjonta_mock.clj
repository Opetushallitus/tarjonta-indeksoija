(ns mocks.tarjonta-mock
  (:require [tarjonta-indeksoija-service.test-tools :as tools]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta]))

(defn get-doc
  [obj]
  (println obj)
  (cond
    (.contains (:type obj) "hakukohde") (:result (tools/parse-body (str "test/resources/hakukohteet/" (:oid obj) ".json")))
    (.contains (:type obj) "koulutus") (:result (tools/parse-body (str "test/resources/koulutukset/" (:oid obj) ".json")))
    (.contains (:type obj) "haku") (:result (tools/parse-body (str "test/resources/haut/" (:oid obj) ".json")))))

(defmacro with-mocked-hakukohde
  [obj & body]
  `(with-redefs [tarjonta/get-doc (fn [~'obj] (get-doc ~obj))]
     (do ~@body)))
