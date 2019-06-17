(ns mocks.externals-mock
  (:require [kouta-indeksoija-service.test-tools :as tools]
            [kouta-indeksoija-service.api]
            [kouta-indeksoija-service.indexer.indexer]
            [base64-clj.core :as b64]))

(defn get-organisaatio-doc
  ([obj]
   (tools/parse (str "test/resources/organisaatiot/" (:oid obj) ".json")))
  ([obj include-image]
   (if include-image
      {:metadata {:kuvaEncoded (b64/encode "jee")}}
      (get-organisaatio-doc obj))))

(defn get-eperuste-doc
  [obj]
  (tools/parse (str "test/resources/eperusteet/" (:oid obj) ".json")))
