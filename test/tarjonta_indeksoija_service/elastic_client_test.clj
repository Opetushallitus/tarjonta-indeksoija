(ns tarjonta-indeksoija-service.elastic-client-test
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [tarjonta-indeksoija-service.elastic-client :as client]
            [midje.sweet :refer :all]))

(def index-identifier (.getTime (java.util.Date.)))

(defn index-name
  [base-name]
  (str base-name "_" index-identifier))

(against-background [(after :contents (do (client/delete-index (index-name "hakukohde_test"))
                                          (client/delete-index (index-name "indexdata"))))]
  (facts "Index queue"
    (fact "should return nil when non-existing index"
      (client/get-first-from-queue :index (index-name "indexdata") :type (index-name "indexdata")) => nil)

    (fact "should insert"
      (:result (client/push-to-indexing-queue "123" "hakukohde" :index (index-name "indexdata") :type (index-name "indexdata"))) => "created"
      (:result (client/push-to-indexing-queue "1234" "hakukohde" :index (index-name "indexdata") :type (index-name "indexdata"))) => "created"
      (:result (client/push-to-indexing-queue "123" "hakukohde" :index (index-name "indexdata") :type (index-name "indexdata"))) => "updated")

    (fact "should get oldest"
      (Thread/sleep 1000) ;; TODO figure out a way to do this smarter
      (:oid (client/get-first-from-queue :index (index-name "indexdata") :type (index-name "indexdata"))) => "1234")

    (fact "should delete oldest"
      (client/delete-by-id (index-name "indexdata") (index-name "indexdata")
                           (:oid (client/get-first-from-queue :index (index-name "indexdata") :type (index-name "indexdata"))))
      (Thread/sleep 1000) ;; TODO figure out a way to do this smarter
      (:oid (client/get-first-from-queue :index (index-name "indexdata") :type (index-name "indexdata"))) => "123")

    (fact "should return nil when index is empty"
      (client/delete-by-id (index-name "indexdata") (index-name "indexdata")
                           (:oid (client/get-first-from-queue :index (index-name "indexdata") :type (index-name "indexdata"))))
      (Thread/sleep 1000) ;; TODO figure out a way to do this smarter
      (:oid (client/get-first-from-queue :index (index-name "indexdata") :type (index-name "indexdata"))) => nil))

  (fact "Elastic client should index hakukohde"
    (let [res (client/upsert (index-name "hakukohde_test") (index-name "hakukohde_test") "1234" {:oid "1234"})]
      (:result res) => "created"
      (Thread/sleep 1000) ;; TODO figure out a way to do this smarter
      (:oid (client/get-by-id (index-name "hakukohde_test") (index-name "hakukohde_test") "1234")) => "1234")))

