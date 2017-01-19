(ns tarjonta-indeksoija-service.elastic-client-test
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [tarjonta-indeksoija-service.elastic-client :as client]
            [midje.sweet :refer :all]))

(def index-identifier (.getTime (java.util.Date.)))

(defn index-name
  [base-name]
  (str base-name "_" index-identifier))

(against-background [(after :contents (client/delete-index (index-name "hakukohde_test")))]
  (fact "Elastic client should index hakukohde"
    (let [res (client/index (index-name "hakukohde_test") (index-name "hakukohde_test") {:oid "1234"})]
      (:result res) => "created"
      (Thread/sleep 1000) ;; TODO figure out a way to do this smarter
      (count (client/query (index-name "hakukohde_test") (index-name "hakukohde_test") :_id (:_id res))) => 1)))

