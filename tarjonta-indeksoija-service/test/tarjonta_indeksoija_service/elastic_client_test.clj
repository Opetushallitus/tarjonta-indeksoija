(ns tarjonta-indeksoija-service.elastic-client-test
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [tarjonta-indeksoija-service.elastic-client :as client]
            [midje.sweet :refer :all]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.query :as q]))

(defn delete-index
  []
  (let [conn (esr/connect (:elastic-url env))]
    (esi/delete conn "hakukohde_test")))

(against-background [(before :contents (delete-index))]
  (fact "Elastic client should index hakukohde"
    (let [res (client/index "hakukohde_test" "hakukohde_test" {:oid "1234"})]
      (:result res) => "created"
      (Thread/sleep 1000) ;; TODO figure out a way to do this smarter
      (count (client/query "hakukohde_test" "hakukohde_test" :_id (:_id res))) => 1)))
