(ns tarjonta-indeksoija-service.elastic-client-test
  (:require [tarjonta-indeksoija-service.conf :as conf :refer [env]]
            [tarjonta-indeksoija-service.elastic-client :as client]
            [tarjonta-indeksoija-service.test-tools :refer [refresh-and-wait reset-test-data]]
            [tarjonta-indeksoija-service.test-tools :as tools]
            [clj-http.client :as http]
            [midje.sweet :refer :all]))

(defn dummy-indexdata
  [& {:keys [amount id-offset] :or {amount 10
                                    id-offset 100}}]
  (map #(hash-map :oid (+ % id-offset) :type "hakukohde") (range amount)))

(against-background [(after :contents (reset-test-data))]
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
      (client/get-last-index-time) => soon)))
