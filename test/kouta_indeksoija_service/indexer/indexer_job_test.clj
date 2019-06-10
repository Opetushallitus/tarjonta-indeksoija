(ns kouta-indeksoija-service.indexer.indexer-job-test
  (:require [clojure.test :refer :all]
            [mount.core :as mount]
   ;[kouta-indeksoija-service.indexer.job :as job]
            [kouta-indeksoija-service.elastic.docs :as docs]
            [kouta-indeksoija-service.elastic.queue :as queue]
            [kouta-indeksoija-service.elastic.admin :refer [initialize-indices]]
            [kouta-indeksoija-service.test-tools :as tools :refer [reset-test-data]]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [mocks.externals-mock :refer [with-externals-mock]]))

(use-fixtures :once (fn [tests] (mount/start) (reset-test-data true) (tests) (mount/stop)))
(use-fixtures :each (fn [test] (test) (reset-test-data true)))

(comment deftest indexer-job-test
  (testing "Indexer should start scheduled indexing and index objects"
    (let [oid "1.2.246.562.10.39920288212"]
      (with-externals-mock
        (job/start-indexer-job "*/5 * * ? * *")
        (is (= [nil] (map #(docs/get-organisaatio %) [oid])))
        (queue/upsert-to-queue [{:oid oid :type "organisaatio"}])
        (tools/block-until-indexed 15000)
        (tools/refresh-index "organisaatio")
        (let [res (docs/get-organisaatio oid)]
          (is (= oid (:oid res)))
          (queue/upsert-to-queue [{:oid oid :type "organisaatio"}])
          (tools/block-until-indexed 15000)
          (tools/refresh-index "organisaatio")
          (is (< (:timestamp res) (:timestamp (docs/get-organisaatio oid)))))))))