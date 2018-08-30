(ns konfo-indeksoija-service.eperuste-client-test
  (:require [midje.sweet :refer :all]
            [konfo-indeksoija-service.rest.eperuste :refer [find-docs]]
            [konfo-indeksoija-service.rest.util :as client]))

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
    (find-docs) => [{:oid "0" :type "eperuste"}]))

(fact "Get zero pages"
  (with-redefs [client/get mock-get-none]
    (find-docs) => []))

(fact "Get many pages"
  (with-redefs [client/get mock-get-many]
    (find-docs) => [{:oid "0" :type "eperuste"}, {:oid "1" :type "eperuste"},
                    {:oid "2" :type "eperuste"}, {:oid "3" :type "eperuste"},
                    {:oid "4" :type "eperuste"}, {:oid "5" :type "eperuste"},
                    {:oid "6" :type "eperuste"}, {:oid "7" :type "eperuste"},
                    {:oid "8" :type "eperuste"}, {:oid "9" :type "eperuste"}]))