(ns kouta-indeksoija-service.rest.eperuste-client-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.rest.eperuste :refer [find-all find-changes]]
            [kouta-indeksoija-service.rest.util :as client]))

(defn mock-get-one [url opts]
  {:status 200
   :body {:data [{:id 0 :oid "0"}],
          :sivuja 1,
          :kokonaismäärä 1,
          :sivukoko 100,
          :sivu 0}})

(defn mock-get-none [url opts]
  {:status 200
   :body {:data [],
          :sivuja 0,
          :kokonaismäärä 0,
          :sivukoko 100
          :sivu 0}})

(defn mock-get-many [url opts]
  (let [sivu (:sivu (:query-params opts))
        x (* 2 sivu)]
    {:status 200
     :body {:data (map (fn [i] {:id i}) (range x (+ x 2))),
            :sivuja 5,
            :kokonaismäärä 10,
            :sivukoko 2
            :sivu sivu}}))

(deftest eperuste-client-test
  (testing "eperuste client should"
    (testing "get one page"
      (with-redefs [client/get mock-get-one]
        (is (= ["0"] (find-all)))))

    (testing "get zero pages"
      (with-redefs [client/get mock-get-none]
        (is (= [] (find-all)))))

    (testing "get many pages"
      (with-redefs [client/get mock-get-many]
        (let [expected ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"]]
          (is (= expected (find-all))))))

    (testing "set no muokattu param when fetching all"
      (defn no-muokattu-params [url opts]
        (is (= nil (:muokattu (:query-params opts))))
        (mock-get-none url opts))
      (with-redefs [client/get no-muokattu-params]
        (find-all)))

    (testing "set muokattu param when fetching changes"
      (defn muokattu-param-present [url opts]
        (is (= 1213145 (:muokattu (:query-params opts))))
        (mock-get-none url opts))
      (with-redefs [client/get muokattu-param-present]
        (find-changes 1213145)))))