(ns kouta-indeksoija-service.indexer.indexer-test
  (:require [kouta-indeksoija-service.indexer.index :as indexer]
            [kouta-indeksoija-service.indexer.job :as job]
            [kouta-indeksoija-service.elastic.docs :as docs]
            [kouta-indeksoija-service.elastic.queue :as queue]
            [kouta-indeksoija-service.elastic.admin :refer [initialize-indices]]
            [kouta-indeksoija-service.test-tools :as tools :refer [reset-test-data]]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [mocks.externals-mock :refer [with-externals-mock]]
            [midje.sweet :refer :all]))

(against-background
  [(before :contents (init-elastic-test))
   (after :facts (reset-test-data))
   (after :contents (stop-elastic-test))]

  (fact "Indexer should save organisaatio"
    (let [oid "1.2.246.562.10.39920288212"]
      (with-externals-mock
       (indexer/index-objects [(indexer/get-index-doc {:oid oid :type "organisaatio"})]))
      (docs/get-organisaatio oid) => (contains {:oid oid})))

  (fact "Indexer should start scheduled indexing and index objects"
    (let [oid "1.2.246.562.10.39920288212"]
      (with-externals-mock
        (job/start-indexer-job "*/5 * * ? * *")
        (map #(docs/get-organisaatio %) [oid]) => [nil]

        (queue/upsert-to-queue [{:oid oid :type "organisaatio"}])
        (tools/block-until-indexed 15000)
        (tools/refresh-and-wait "organisaatio" 4000)
        (let [res (docs/get-organisaatio oid)]
          res => (contains {:oid oid})
          (queue/upsert-to-queue [{:oid oid :type "organisaatio"}])
          (tools/block-until-indexed 15000)
          (tools/refresh-and-wait "organisaatio" 4000)
          (< (:timestamp res) (:timestamp (docs/get-organisaatio oid))) => true)))))