(ns tarjonta-indeksoija-service.indexer-test
  (:require [tarjonta-indeksoija-service.indexer :as indexer]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [mocks.tarjonta-mock :as mock]
            [midje.sweet :refer :all]))

(def index-identifier (.getTime (java.util.Date.)))

(defn index-name
  [base-name]
  (str base-name "_" index-identifier))

(fact "Indexer should save hakukohde"
  (let [oid "1.2.246.562.20.99178639649"]
    (mock/with-mocked-hakukohde oid
      (indexer/index-hakukohde oid :index (index-name "hakukohde") :type (index-name "hakukohde"))
      (elastic-client/get-by-id (index-name "hakukohde") (index-name "hakukohde") oid) => (contains {:oid oid}))))
