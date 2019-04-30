(ns kouta-indeksoija-service.eperuste-client-test
  (:require [midje.sweet :refer :all]
            [kouta-indeksoija-service.rest.eperuste :refer [find-all find-changes]]
            [kouta-indeksoija-service.rest.util :as client]))

(defn mock-get-one [url opts]
  {:body {:data [{:id 0 :oid "0"}],
          :sivuja 1,
          :kokonaismäärä 1,
          :sivukoko 100,
          :sivu 0}})

(defn mock-get-none [url opts]
  {:body {:data [],
          :sivuja 0,
          :kokonaismäärä 0,
          :sivukoko 100
          :sivu 0}})

(defn mock-get-many [url opts]
  (let [sivu (:sivu (:query-params opts))
        x (* 2 sivu)]
    {:body {:data (map (fn [i] {:id i}) (range x (+ x 2))),
            :sivuja 5,
            :kokonaismäärä 10,
            :sivukoko 2
            :sivu sivu}}))

(fact "Get one page"
  (with-redefs [client/get mock-get-one]
    (find-all) => [{:oid "0" :type "eperuste"}]))

(fact "Get zero pages"
  (with-redefs [client/get mock-get-none]
    (find-all) => []))

(fact "Get many pages"
  (with-redefs [client/get mock-get-many]
    (find-all) => [{:oid "0" :type "eperuste"}, {:oid "1" :type "eperuste"},
                    {:oid "2" :type "eperuste"}, {:oid "3" :type "eperuste"},
                    {:oid "4" :type "eperuste"}, {:oid "5" :type "eperuste"},
                    {:oid "6" :type "eperuste"}, {:oid "7" :type "eperuste"},
                    {:oid "8" :type "eperuste"}, {:oid "9" :type "eperuste"}]))

(fact "No muokattu param"
  (defn no-muokattu-params [url opts]
    (:muokattu (:query-params opts)) => nil
    (mock-get-none url opts))
  (with-redefs [client/get no-muokattu-params]
    (find-all)))

(fact "Muokattu param present"
  (defn muokattu-param-present [url opts]
    (:muokattu (:query-params opts)) => 1213145
    (mock-get-none url opts))
  (with-redefs [client/get muokattu-param-present]
    (find-changes 1213145)))
