(ns tarjonta-indeksoija-service.elastic-client-test
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [tarjonta-indeksoija-service.elastic-client :as client]
            [midje.sweet :refer :all]))

(def index-identifier (.getTime (java.util.Date.)))

(defn index-name
  [base-name]
  (str base-name "_" index-identifier))

(def indexdata (index-name "indexdata"))
(def hakukohde-index (index-name "hakukohde_test"))

(defn dummy-indexdata
  [& {:keys [amount id-offset] :or {amount 10
                                    id-offset 100}}]
  (map #(hash-map :oid (+ % id-offset) :type "hakukohde") (range amount)))

(against-background [(after :contents (client/delete-index indexdata))]
  (facts "Index queue"
    (fact "Should be empty"
      (client/get-queue :index indexdata :type indexdata) => ())

    (fact "should get queue"
      (:errors (client/bulk-upsert indexdata indexdata (dummy-indexdata))) => false
      (client/refresh-index indexdata)
      (count (client/get-queue :index indexdata :type indexdata)) => 10
      (sort-by :oid (map #(select-keys % [:oid :type]) (client/get-queue :index indexdata :type indexdata)))
        => (dummy-indexdata))

    (fact "should delete handled oids"
      (let [last-timestamp (:timestamp (last (client/get-queue :index indexdata :type indexdata)))]
        (:errors (client/bulk-upsert indexdata indexdata (dummy-indexdata :amount 1 :id-offset 1000))) => false
        (client/delete-handled-queue last-timestamp :index indexdata :type indexdata)
        (client/refresh-index indexdata)
        (count (client/get-queue :index indexdata :type indexdata)) => 1
        (select-keys (first (client/get-queue :index indexdata :type indexdata)) [:oid :type]) =>
          {:oid 1000 :type "hakukohde"}))

    (fact "should avoid race condition"
      (client/delete-index indexdata)
      (client/get-queue :index indexdata :type indexdata) => ()
      (:errors (client/bulk-upsert indexdata indexdata (dummy-indexdata :amount 1))) => false
      (:errors (client/bulk-upsert indexdata indexdata (dummy-indexdata :amount 1 :id-offset 1000))) => false
      (client/refresh-index indexdata)
      (let [res (client/get-queue :index indexdata :type indexdata)]
        (count res) =>  2
        (:timestamp (first res)) =not=> (:timestamp (last res))))))

(against-background [(after :contents (client/delete-index hakukohde-index))]
  (facts "Elastic client should index hakukohde"
    (let [res (client/upsert hakukohde-index hakukohde-index "1234" {:oid "1234"})]
      (:result res) => "created"
      (client/refresh-index hakukohde-index)
      (:oid (client/get-by-id hakukohde-index hakukohde-index "1234")) => "1234")))

