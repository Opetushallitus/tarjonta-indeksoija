(ns kouta-indeksoija-service.queuer.last-queued-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.queuer.last-queued :refer [index-name set-last-queued-time get-last-queued-time]]
            [kouta-indeksoija-service.test-tools :refer [refresh-index]]))

(deftest last-queued-test

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