(ns mocks.tarjonta-mock
  (:require [tarjonta-indeksoija-service.test-tools :as tools]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta]))

(defn get-hakukohde
  [oid]
  (:result (tools/parse-body (str "test/resources/hakukohteet/" oid ".json"))))

(defmacro with-mocked-hakukohde
  [oid & body]
  `(with-redefs [tarjonta/get-hakukohde (fn [~'oid] (get-hakukohde ~oid))]
     (do ~@body)))