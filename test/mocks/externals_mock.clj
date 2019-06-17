(ns mocks.externals-mock
  (:require [kouta-indeksoija-service.test-tools :as tools]
            [kouta-indeksoija-service.api]
            [kouta-indeksoija-service.indexer.indexer]
            [base64-clj.core :as b64]))

(defn get-organisaatio-doc
  ([oid]
   (tools/parse (str "test/resources/organisaatiot/" oid ".json")))
  ([oid include-image]
   (if include-image
      {:metadata {:kuvaEncoded (b64/encode "jee")}}
      (get-organisaatio-doc oid))))

(defn get-eperuste-doc
  [id]
  (tools/parse (str "test/resources/eperusteet/" id ".json")))
