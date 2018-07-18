(ns konfo-indeksoija-service.elastic-client-test
  (:require [konfo-indeksoija-service.conf :as conf :refer [env]]
            [konfo-indeksoija-service.elastic-client :as client]
            [clj-elasticsearch.elastic-connect :as e]
            [clj-elasticsearch.elastic-utils :refer [max-payload-size bulk-partitions]]
            [konfo-indeksoija-service.test-tools :refer [refresh-and-wait reset-test-data init-elastic-test stop-elastic-test]]
            [clj-http.client :as http]
            [midje.sweet :refer :all]))

(defn dummy-indexdata
  [& {:keys [amount id-offset] :or {amount 10
                                    id-offset 100}}]
  (map #(hash-map :oid (+ % id-offset) :type "hakukohde") (range amount)))

(fact "Payload for bulk operation should be partitioned correctly"
  (with-redefs [max-payload-size 2025]
    (let [docs (dummy-indexdata :amount 50 :id-offset 1000)
          data (client/bulk-upsert-data "indexdata" "indexdata" docs)
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
    (client/check-elastic-status) => true)

  (fact "Elastic client should initialize indices"
    (client/initialize-indices) => true)

  (fact "Should have index analyzer settings set"
    (let [res (http/get (str (:elastic-url env) "/hakukohde_test/_settings")
                        {:as :json :content-type :json})]
      (get-in res [:body :hakukohde_test :settings :index :analysis]) => (:analysis conf/index-settings)))

  (fact "Should have index stemmer settings set"
    (let [res (http/get (str (:elastic-url env) "/hakukohde_test/_mappings/")
                        {:as :json :content-type :json})]
      (get-in res [:body :hakukohde_test :mappings :hakukohde_test]) => conf/stemmer-settings))

  (fact "Should get elastic-status"
    (keys (client/get-elastic-status)) => [:cluster_health :indices-info])

  (facts "Index queue"
    (fact "Should be empty"
      (client/get-queue) => ())

    (fact "should get queue"
      (:errors (client/upsert-indexdata (dummy-indexdata))) => false
      (client/refresh-index "indexdata")
      (count (client/get-queue)) => 10
      (sort-by :oid (map #(select-keys % [:oid :type]) (client/get-queue))) => (dummy-indexdata))

    (fact "should delete handled oids"
      (let [last-timestamp (:timestamp (last (client/get-queue)))]
        (:errors (client/upsert-indexdata (dummy-indexdata :amount 1 :id-offset 1000))) => false
        (client/delete-handled-queue (range 100 110) last-timestamp)
        (client/refresh-index "indexdata")
        (count (client/get-queue)) => 1
        (select-keys (first (client/get-queue)) [:oid :type]) => {:oid 1000 :type "hakukohde"}))

    (fact "should avoid race condition"
      (client/delete-index "indexdata")
      (client/get-queue) => nil
      (:errors (client/upsert-indexdata (dummy-indexdata :amount 1))) => false
      (:errors (client/upsert-indexdata (dummy-indexdata :amount 1 :id-offset 1000))) => false
      (client/refresh-index "indexdata")
      (let [res (client/get-queue)]
        (count res) => 2
        (:timestamp (first res)) =not=> (:timestamp (last res))))

    (fact "should only remove oids from queue that haven't been updated after indexing started"
      (client/delete-index "indexdata")
      (:errors (client/upsert-indexdata (dummy-indexdata))) => false
      (client/refresh-index "indexdata")
      (let [queue (client/get-queue)
            last-timestamp (apply max (map :timestamp queue))]
        (count queue) => 10
        (:errors (client/upsert-indexdata [{:oid 100 :type "hakukohde"}
                                           {:oid 109 :type "hakukohde"}])) => false
        (client/refresh-index "indexdata")
        (:deleted (client/delete-handled-queue (map :oid queue) last-timestamp)) => 8
        (client/refresh-index "indexdata")

        (let [remaining (client/get-queue)]
          (count remaining) => 2
          (map :oid remaining) => [100 109]
          (every? #(< last-timestamp (:timestamp %)) remaining) => true))))

  (fact "should keep last updated up to date"
    (let [now (System/currentTimeMillis)
          soon (+ now (* 1000 3600))]
      (client/set-last-index-time now)
      (refresh-and-wait "indexdata" 1000)
      (client/get-last-index-time) => now

      (client/set-last-index-time soon)
      (refresh-and-wait "indexdata" 1000)
      (client/get-last-index-time) => soon))

  (fact "should move failed oids to the end of index and try to index failing oids only three times"
    (let [original-queue (client/get-queue)
          oids (map #(dissoc % :type :timestamp) original-queue)]
      (client/bulk-update-failed "indexdata" "indexdata" oids)
      (refresh-and-wait "indexdata" 1000)
      (let [new-queue (client/get-queue)]
        (:oid (first new-queue)) => (:oid (first original-queue))
        (< (:timestamp (first original-queue)) (:timestamp (first new-queue))) => true
        (:oid (second new-queue)) => (:oid (second original-queue))
        (< (:timestamp (second original-queue)) (:timestamp (second new-queue))) => true
        (client/bulk-update-failed "indexdata" "indexdata" oids)
        (refresh-and-wait "indexdata" 1000)
        (count (client/get-queue)) => 2
        (client/bulk-update-failed "indexdata" "indexdata" oids)
        (refresh-and-wait "indexdata" 1000)
        (count (client/get-queue)) => 0))))
