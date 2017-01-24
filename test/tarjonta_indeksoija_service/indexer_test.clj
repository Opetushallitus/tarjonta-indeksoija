(ns tarjonta-indeksoija-service.indexer-test
  (:require [tarjonta-indeksoija-service.indexer :as indexer]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [mocks.tarjonta-mock :as mock]
            [midje.sweet :refer :all]))

(def index-identifier (.getTime (java.util.Date.)))

(defn index-name
  [base-name]
  (str base-name "_" index-identifier))

(def hakukohde-index (index-name "hakukohde_test"))

(against-background [(after :contents (elastic-client/delete-index hakukohde-index))]
  (fact "Indexer should save hakukohde"
    (let [oid "1.2.246.562.20.99178639649"]
      (mock/with-mock {:oid oid :type hakukohde-index}
                      (indexer/index-object {:oid oid :type hakukohde-index}))
        (elastic-client/get-by-id hakukohde-index hakukohde-index oid) => (contains {:oid oid}))))
