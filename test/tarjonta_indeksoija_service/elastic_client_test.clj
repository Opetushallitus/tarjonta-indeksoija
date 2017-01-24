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

(against-background [(after :contents (do (client/delete-index (index-name "hakukohde_test"))
                                          (client/delete-index (index-name "indexdata"))))]
  (facts "Index queue"
    (fact "should get queue"
      (client/get-queue :index indexdata :type indexdata) => ()
      (:result (client/push-to-indexing-queue "123" "hakukohde" :index indexdata :type indexdata)) => "created"
      (:result (client/push-to-indexing-queue "1234" "hakukohde" :index indexdata :type indexdata)) => "created"
      (:result (client/push-to-indexing-queue "12345" "hakukohde" :index indexdata :type indexdata)) => "created"

      (client/refresh-index indexdata)
      (count (client/get-queue :index indexdata :type indexdata)) => 3
      (map #(select-keys % [:oid :type]) (client/get-queue :index indexdata :type indexdata)) => [{:oid "123" :type "hakukohde"}
                                              {:oid "1234" :type "hakukohde"}
                                              {:oid "12345" :type "hakukohde"}])

    (fact "should delete handled oids"
      (let [last-timestamp (:timestamp (last (client/get-queue :index indexdata :type indexdata)))]
        (:result (client/push-to-indexing-queue "123456" "hakukohde" :index indexdata :type indexdata)) => "created"
        (client/refresh-index indexdata)
        (client/delete-handled-queue last-timestamp :index indexdata :type indexdata)
        (client/refresh-index indexdata)
        (count (client/get-queue :index indexdata :type indexdata)) => 1
        (select-keys (first (client/get-queue :index (index-name "indexdata") :type (index-name "indexdata"))) [:oid :type]) =>
          {:oid "123456" :type "hakukohde"})))

  (facts "Elastic client should index hakukohde"
    (let [res (client/upsert hakukohde-index hakukohde-index "1234" {:oid "1234"})]
      (:result res) => "created"
      (client/refresh-index hakukohde-index)
      (:oid (client/get-by-id hakukohde-index hakukohde-index "1234")) => "1234")))

