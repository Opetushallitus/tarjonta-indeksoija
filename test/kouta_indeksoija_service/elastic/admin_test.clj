(ns kouta-indeksoija-service.elastic.admin-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.elastic.settings :as settings]
            [kouta-indeksoija-service.elastic.tools :as tools]
            [kouta-indeksoija-service.elastic.admin :as admin]
            [kouta-indeksoija-service.elastic.queue :as queue]
            [clj-elasticsearch.elastic-utils :refer [max-payload-size bulk-partitions elastic-host]]
            [kouta-indeksoija-service.test-tools :refer [refresh-index reset-test-data]]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [clj-http.client :as http]))

(defn dummy-indexdata
  [& {:keys [amount id-offset] :or {amount 10 id-offset 100}}]
  (map #(hash-map :oid (+ % id-offset) :type "hakukohde") (range amount)))

(deftest elastic-admin-test
  (testing "Elastic admin"
    (testing "should check that elastic search is alive"
      (is (admin/check-elastic-status)))

    (testing "should initialize indices"
      (is (admin/initialize-indices)))

    (testing "should have index analyzer settings set"
      (let [res (http/get (str elastic-host "/hakukohde-kouta_test/_settings") {:as :json :content-type :json})]
        (is (= (:analysis settings/index-settings)
               (get-in res [:body :hakukohde-kouta_test :settings :index :analysis])))))

    (testing "should have index stemmer settings set"
      (let [res (http/get (str elastic-host "/hakukohde-kouta_test/_mappings/") {:as :json :content-type :json})]
        (is (= settings/kouta-settings
               (get-in res [:body :hakukohde-kouta_test :mappings :hakukohde-kouta_test])))))

    (testing "should get elastic-status"
      (is (= [:cluster_health :indices-info] (keys (admin/get-elastic-status))))))

    (testing "Index queue"
      (testing "should be empty"
        (is (= () (queue/get-queue))))

      (testing "should get queue"
        (is (= [] (queue/upsert-to-queue (dummy-indexdata))))
        (queue/refresh-queue)
        (is (= 10 (count (queue/get-queue))))
        (is (= (dummy-indexdata) (sort-by :oid (map #(select-keys % [:oid :type]) (queue/get-queue))))))

      (testing "should delete handled oids"
        (let [last-timestamp (:timestamp (last (queue/get-queue)))]
          (is (= [] (queue/upsert-to-queue (dummy-indexdata :amount 1 :id-offset 1000))))
          (queue/delete-handled-queue (range 100 110) last-timestamp)
          (queue/refresh-queue)
          (is (= 1 (count (queue/get-queue))))
          (is (= {:oid 1000 :type "hakukohde"} (select-keys (first (queue/get-queue)) [:oid :type])))))

      (testing "should avoid race condition"
        (tools/delete-index "indexdata")
        (is (= nil (queue/get-queue)))
        (is (= []  (queue/upsert-to-queue (dummy-indexdata :amount 1))))
        (is (= []  (queue/upsert-to-queue (dummy-indexdata :amount 1 :id-offset 1000))))
        (queue/refresh-queue)
        (let [res (queue/get-queue)]
          (is (= 2 (count res)))
          (is (not (= (:timestamp (last res)) (:timestamp (first res)))))))

      (testing "should only remove oids from queue that haven't been updated after indexing started"
        (tools/delete-index "indexdata")
        (is (= [] (queue/upsert-to-queue (dummy-indexdata))))
        (queue/refresh-queue)
        (let [queue (queue/get-queue)
              last-timestamp (apply max (map :timestamp queue))]
          (is (= 10 (count queue)))
          (is (= [] (queue/upsert-to-queue [{:oid 100 :type "hakukohde"} {:oid 109 :type "hakukohde"}])))
          (queue/refresh-queue)
          (is (= 8 (:deleted (queue/delete-handled-queue (map :oid queue) last-timestamp))))
          (queue/refresh-queue)

          (let [remaining (queue/get-queue)]
            (is (= 2 (count remaining)))
            (is (= [100 109] (map :oid remaining)))
            (is (every? #(< last-timestamp (:timestamp %)) remaining))))))

    (testing "should keep last updated up to date"
      (let [now (System/currentTimeMillis)
            soon (+ now (* 1000 3600))]
        (queue/set-last-index-time now)
        (refresh-index "indexdata")
        (is (= now (queue/get-last-index-time)))

        (queue/set-last-index-time soon)
        (refresh-index "indexdata")
        (is (= soon (queue/get-last-index-time)))))

    (testing "should move failed oids to the end of index and try to index failing oids only three times"
      (let [original-queue (queue/get-queue)
            oids (map #(dissoc % :type :timestamp) original-queue)]
        (tools/bulk-update-failed "indexdata" "indexdata" oids)
        (refresh-index "indexdata")
        (let [new-queue (queue/get-queue)]
          (is (= (:oid (first original-queue)) (:oid (first new-queue))))
          (is (< (:timestamp (first original-queue)) (:timestamp (first new-queue))))
          (is (= (:oid (second original-queue)) (:oid (second new-queue))))
          (is (< (:timestamp (second original-queue)) (:timestamp (second new-queue))))
          (tools/bulk-update-failed "indexdata" "indexdata" oids)
          (refresh-index "indexdata")
          (is (= 2 (count (queue/get-queue))))
          (tools/bulk-update-failed "indexdata" "indexdata" oids)
          (refresh-index "indexdata")
          (is (= 0 (count (queue/get-queue))))))))