(ns tarjonta-indeksoija-service.elastic-client-test
  (require [midje.sweet :refer :all]
           [tarjonta-indeksoija-service.elastic-client :as client]
           [cheshire.core :as json]
           [clj-http.client :as http]
           [clojurewerkz.elastisch.rest :as esr]
           [clojurewerkz.elastisch.rest.index :as esi]
           [clojurewerkz.elastisch.rest.document :as esd]
           [clojurewerkz.elastisch.query :as q]))

(defn delete-index
  []
  (let [conn (esr/connect "http://127.0.0.1:9200")]
    (println (esi/delete conn "hakukohde_test"))))

(against-background [(before :contents (delete-index))]
  (fact "Elastic client should index hakukohde"
    (let [res (client/index "hakukohde_test" "hakukohde_test" {:oid "1234"})]
      (:result res) => "created"
      (Thread/sleep 1000) ;; TODO figure out a way to do this smarter
      (count (client/query "hakukohde_test" "hakukohde_test" :_id (:_id res))) => 1)))
