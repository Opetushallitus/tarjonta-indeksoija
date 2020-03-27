(ns kouta-indeksoija-service.queuer.last-queued-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.elastic.admin :as admin]
            [clj-elasticsearch.elastic-connect :refer [refresh-index]]
            [kouta-indeksoija-service.queuer.last-queued :refer [index-name set-last-queued-time get-last-queued-time]]))

(deftest last-queued-test

  (admin/initialize-indices)

  (testing "should keep last queued time up to date"
    (let [now (System/currentTimeMillis)
          soon (+ now (* 1000 3600))]
      (is (< now (get-last-queued-time)))
      (set-last-queued-time now)
      (refresh-index index-name)
      (is (= now (get-last-queued-time)))
      (set-last-queued-time soon)
      (refresh-index index-name)
      (is (= soon (get-last-queued-time))))))