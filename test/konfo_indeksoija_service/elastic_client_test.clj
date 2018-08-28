(ns konfo-indeksoija-service.elastic-client-test
  (:require [konfo-indeksoija-service.util.conf :as conf]
            [konfo-indeksoija-service.elastic.tools :as tools]
            [konfo-indeksoija-service.elastic.admin :as admin]
            [konfo-indeksoija-service.elastic.queue :as queue]
            [clj-elasticsearch.elastic-utils :refer [max-payload-size bulk-partitions elastic-host]]
            [konfo-indeksoija-service.test-tools :refer [refresh-and-wait reset-test-data]]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [clj-http.client :as http]
            [midje.sweet :refer :all]))

(defn dummy-indexdata
  [& {:keys [amount id-offset] :or {amount 10
                                    id-offset 100}}]
  (map #(hash-map :oid (+ % id-offset) :type "hakukohde") (range amount)))

(fact "Payload for bulk operation should be partitioned correctly"
  (with-redefs [max-payload-size 2025]
    (let [docs (dummy-indexdata :amount 50 :id-offset 1000)
          data (tools/bulk-upsert-data "indexdata" "indexdata" docs)
          bulk-data (bulk-partitions data)]
      (count bulk-data) => 5
      (println (nth bulk-data 0))
      (println "=======")
      (println (nth bulk-data 1))
      (println "=======")
      (println (nth bulk-data 2))
      (println "=======")
      (println (nth bulk-data 3))
      (println "=======")
      (println (nth bulk-data 4))

      (< (count (.getBytes (nth bulk-data 0))) 2025) => true
      (< (count (.getBytes (nth bulk-data 1))) 2025) => true
      (< (count (.getBytes (nth bulk-data 2))) 2025) => true
      (< (count (.getBytes (nth bulk-data 3))) 2025) => true
      (< (count (.getBytes (nth bulk-data 4))) 2025) => true

      (.startsWith (nth bulk-data 0) "{\"update")  => true
      (.startsWith (nth bulk-data 1) "{\"update")  => true
      (.startsWith (nth bulk-data 2) "{\"update")  => true
      (.startsWith (nth bulk-data 3) "{\"update")  => true
      (.startsWith (nth bulk-data 4) "{\"update")  => true)))

(against-background [(before :contents (init-elastic-test))
                     (after :contents (stop-elastic-test))]
  (fact "Elastic search should be alive"
    (admin/check-elastic-status) => true)

  (fact "Elastic client should initialize indices"
    (admin/initialize-indices) => true)

  (fact "Should have index analyzer settings set"
    (let [res (http/get (str elastic-host "/hakukohde_test/_settings")
                        {:as :json :content-type :json})]
      (get-in res [:body :hakukohde_test :settings :index :analysis]) => (:analysis conf/index-settings)))

  (fact "Should have index stemmer settings set"
    (let [res (http/get (str elastic-host "/hakukohde_test/_mappings/")
                        {:as :json :content-type :json})]
      (get-in res [:body :hakukohde_test :mappings :hakukohde_test]) => conf/stemmer-settings))

  (fact "Should get elastic-status"
    (keys (admin/get-elastic-status)) => [:cluster_health :indices-info])

  (facts "Index queue"
    (fact "Should be empty"
      (queue/get-queue) => ())

    (fact "should get queue"
      (:errors (queue/upsert-to-queue (dummy-indexdata))) => false
      (queue/refresh-queue)
      (count (queue/get-queue)) => 10
      (sort-by :oid (map #(select-keys % [:oid :type]) (queue/get-queue))) => (dummy-indexdata))

    (fact "should delete handled oids"
      (let [last-timestamp (:timestamp (last (queue/get-queue)))]
        (:errors (queue/upsert-to-queue (dummy-indexdata :amount 1 :id-offset 1000))) => false
        (queue/delete-handled-queue (range 100 110) last-timestamp)
        (queue/refresh-queue)
        (count (queue/get-queue)) => 1
        (select-keys (first (queue/get-queue)) [:oid :type]) => {:oid 1000 :type "hakukohde"}))

    (fact "should avoid race condition"
      (tools/delete-index "indexdata")
      (queue/get-queue) => nil
      (:errors (queue/upsert-to-queue (dummy-indexdata :amount 1))) => false
      (:errors (queue/upsert-to-queue (dummy-indexdata :amount 1 :id-offset 1000))) => false
      (queue/refresh-queue)
      (let [res (queue/get-queue)]
        (count res) => 2
        (:timestamp (first res)) =not=> (:timestamp (last res))))

    (fact "should only remove oids from queue that haven't been updated after indexing started"
      (tools/delete-index "indexdata")
      (:errors (queue/upsert-to-queue (dummy-indexdata))) => false
      (queue/refresh-queue)
      (let [queue (queue/get-queue)
            last-timestamp (apply max (map :timestamp queue))]
        (count queue) => 10
        (:errors (queue/upsert-to-queue [{:oid 100 :type "hakukohde"}
                                           {:oid 109 :type "hakukohde"}])) => false
        (queue/refresh-queue)
        (:deleted (queue/delete-handled-queue (map :oid queue) last-timestamp)) => 8
        (queue/refresh-queue)

        (let [remaining (queue/get-queue)]
          (count remaining) => 2
          (map :oid remaining) => [100 109]
          (every? #(< last-timestamp (:timestamp %)) remaining) => true))))

  (fact "should keep last updated up to date"
    (let [now (System/currentTimeMillis)
          soon (+ now (* 1000 3600))]
      (queue/set-last-index-time now)
      (refresh-and-wait "indexdata" 1000)
      (queue/get-last-index-time) => now

      (queue/set-last-index-time soon)
      (refresh-and-wait "indexdata" 1000)
      (queue/get-last-index-time) => soon))

  (fact "should move failed oids to the end of index and try to index failing oids only three times"
    (let [original-queue (queue/get-queue)
          oids (map #(dissoc % :type :timestamp) original-queue)]
      (tools/bulk-update-failed "indexdata" "indexdata" oids)
      (refresh-and-wait "indexdata" 1000)
      (let [new-queue (queue/get-queue)]
        (:oid (first new-queue)) => (:oid (first original-queue))
        (< (:timestamp (first original-queue)) (:timestamp (first new-queue))) => true
        (:oid (second new-queue)) => (:oid (second original-queue))
        (< (:timestamp (second original-queue)) (:timestamp (second new-queue))) => true
        (tools/bulk-update-failed "indexdata" "indexdata" oids)
        (refresh-and-wait "indexdata" 1000)
        (count (queue/get-queue)) => 2
        (tools/bulk-update-failed "indexdata" "indexdata" oids)
        (refresh-and-wait "indexdata" 1000)
        (count (queue/get-queue)) => 0))))
