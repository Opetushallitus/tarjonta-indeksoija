(ns mocks.externals-mock
  (:require [kouta-indeksoija-service.test-tools :as tools]
            [kouta-indeksoija-service.elastic.queue :refer [upsert-to-queue]]
            [kouta-indeksoija-service.api]
            [kouta-indeksoija-service.indexer.index]
            [base64-clj.core :as b64]))

(defn get-doc
  ([obj]
   (cond
     (.contains (:type obj) "eperuste") (tools/parse (str "test/resources/eperusteet/" (:oid obj) ".json"))
     (.contains (:type obj) "organisaatio") (tools/parse (str "test/resources/organisaatiot/" (:oid obj) ".json"))))
  ([obj include-image]
   (get-doc obj)
     (if include-image
        {:metadata {:kuvaEncoded (b64/encode "jee")}}
        (get-doc obj))))

(defn refresh-s3
  [obj pics]
  true)

(defn find-last-changes [last-modified] [])

(defn find-changes [last-modified] [])

(defn queue-mock
  [index oid]
  (upsert-to-queue [{:type index :oid oid}]))

(defmacro with-externals-mock
  [& body]
  `(with-redefs [kouta-indeksoija-service.indexer.queue/queue
                 mocks.externals-mock/queue-mock

                 kouta-indeksoija-service.indexer.docs/get-doc
                 mocks.externals-mock/get-doc

                 kouta-indeksoija-service.rest.organisaatio/get-doc
                 mocks.externals-mock/get-doc

                 kouta-indeksoija-service.rest.organisaatio/find-last-changes
                 mocks.externals-mock/find-last-changes

                 kouta-indeksoija-service.rest.eperuste/get-doc
                 mocks.externals-mock/get-doc

                 kouta-indeksoija-service.rest.eperuste/find-changes
                 mocks.externals-mock/find-changes

                 kouta-indeksoija-service.s3.s3-client/refresh-s3
                 mocks.externals-mock/refresh-s3]
     (do ~@body)))
