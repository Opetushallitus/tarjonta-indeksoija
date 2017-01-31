(ns tarjonta-indeksoija-service.elastic-client-test
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [tarjonta-indeksoija-service.elastic-client :as client]
            [tarjonta-indeksoija-service.test-tools :refer [refresh-and-wait]]
            [midje.sweet :refer :all]))

(defn dummy-indexdata
  [& {:keys [amount id-offset] :or {amount 10
                                    id-offset 100}}]
  (map #(hash-map :oid (+ % id-offset) :type "hakukohde") (range amount)))

(against-background [(after :contents [(client/delete-index "hakukohde")
                                       (client/delete-index "koulutus")
                                       (client/delete-index "indexdata")])]
  (facts "Index queue"
    (fact "Should be empty"
      (client/get-queue) => ())

    (fact "should get queue"
      (:errors (client/bulk-upsert "indexdata" "indexdata" (dummy-indexdata))) => false
      (client/refresh-index "indexdata")
      (count (client/get-queue)) => 10
      (sort-by :oid (map #(select-keys % [:oid :type]) (client/get-queue)))
        => (dummy-indexdata))

    (fact "should delete handled oids"
      (let [last-timestamp (:timestamp (last (client/get-queue)))]
        (:errors (client/bulk-upsert "indexdata" "indexdata" (dummy-indexdata :amount 1 :id-offset 1000))) => false
        (client/delete-handled-queue (range 100 110) last-timestamp)
        (client/refresh-index "indexdata")
        (count (client/get-queue)) => 1
        (select-keys (first (client/get-queue)) [:oid :type]) =>
          {:oid 1000 :type "hakukohde"}))

    (fact "should avoid race condition"
      (client/delete-index "indexdata")
      (client/get-queue) => ()
      (:errors (client/bulk-upsert "indexdata" "indexdata" (dummy-indexdata :amount 1))) => false
      (:errors (client/bulk-upsert "indexdata" "indexdata" (dummy-indexdata :amount 1 :id-offset 1000))) => false
      (client/refresh-index "indexdata")
      (let [res (client/get-queue)]
        (count res) =>  2
        (:timestamp (first res)) =not=> (:timestamp (last res))))

    (fact "should only remove oids from queue that haven't been updated after indexing started"
      (client/delete-index "indexdata")
      (:errors (client/bulk-upsert "indexdata" "indexdata" (dummy-indexdata))) => false
      (client/refresh-index "indexdata")
      (let [queue          (client/get-queue)
            last-timestamp (apply max (map :timestamp queue))]
        (count queue) => 10
        (:errors (client/bulk-upsert "indexdata" "indexdata" [{:oid 100 :type "hakukohde"}
                                                              {:oid 109 :type "hakukohde"}])) => false
        (client/refresh-index "indexdata")
        (:deleted (client/delete-handled-queue (map :oid queue) last-timestamp)) => 8
        (client/refresh-index "indexdata")

        (let [remaining (client/get-queue)]
          (count remaining) => 2
          (map :oid remaining) => [100 109]
          (every? #(< last-timestamp (:timestamp %)) remaining) => true)))))
