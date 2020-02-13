(ns kouta-indeksoija-service.elastic.admin-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.elastic.settings :as settings]
            [kouta-indeksoija-service.elastic.admin :as admin]
            [kouta-indeksoija-service.test-tools :refer [debug-pretty]]
            [clj-elasticsearch.elastic-utils :refer [elastic-host]]
            [clj-http.client :as http]))

(deftest elastic-admin-test
  (testing "Elastic admin"
    (testing "should check that elastic search is alive"
      (is (admin/check-elastic-status)))

    (testing "should initialize indices"
      (is (admin/initialize-indices)))

    (testing "should have index analyzer settings set"
      (let [res (http/get (str elastic-host "/hakukohde-kouta_test/_settings") {:as :json :content-type :json})]
        (is (= (:analysis settings/index-settings)
               (get-in res [:body :hakukohde-kouta_test :settings :index :analysis])))))

    (testing "should have index stemmer settings set"
      (let [res (http/get (str elastic-host "/hakukohde-kouta_test/_mappings/") {:as :json :content-type :json})]
        (is (= settings/kouta-settings
               (get-in res [:body :hakukohde-kouta_test :mappings :hakukohde-kouta_test])))))

    (testing "should get elastic-status"
      (is (= [:cluster_health :indices-info] (keys (admin/get-elastic-status)))))

    (testing "get cluster health"
      (is (= 200 (first (admin/healthcheck)))))))