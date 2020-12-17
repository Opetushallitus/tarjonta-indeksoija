(ns kouta-indeksoija-service.indexer.lokalisointi-indexer-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as cheshire]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [kouta-indeksoija-service.indexer.lokalisointi.lokalisointi :as lokalisointi]
            [kouta-indeksoija-service.lokalisointi.service :as service]
            [kouta-indeksoija-service.test-tools :refer :all]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.elastic.admin :as admin]))

(defonce translation-json (parse (str "test/resources/lokalisointi/translation.json")))

(def lokalisointi-service (atom []))

(defn store-traslation-json-fixture
  []
  (with-redefs [kouta-indeksoija-service.rest.cas.session/cas-authenticated-request-as-json
                (fn [x y z req] (swap! lokalisointi-service conj (cheshire/parse-string (:body req) true)))]
    (service/save-translation-json-to-localisation-service "fi" translation-json)))

(use-fixtures :once (fn [test]
                      (store-traslation-json-fixture)
                      (admin/initialize-lokalisointi-indices-for-reindexing)
                      (test)))

(deftest lokalisointi-indeksointi-test
  (with-redefs [kouta-indeksoija-service.rest.util/get->json-body (fn [url] @lokalisointi-service)]
    (testing "should index finnish lokalisointi"
      (i/index-lokalisointi "fi")
      (let [indeksoitu (lokalisointi/get "fi")]
        (is (= translation-json (:translation indeksoitu)))
        (is (= "fi" (:lng indeksoitu)))))))