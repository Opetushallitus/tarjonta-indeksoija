(ns kouta-indeksoija-service.lokalisointi.lokalisointi-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.lokalisointi.util :as util]
            [kouta-indeksoija-service.lokalisointi.service :as service]
            [kouta-indeksoija-service.test-tools :refer [debug-pretty]]
            [cheshire.core :as cheshire]))

(defonce translation-json {"teksti" "teksti"
                           "kappale" "kappale"
                           "osio" {"osioteksti1" "osio1"
                                   "osioteksti2" "osio2"}
                           "otsikko" {"paaotsikko" "pääotsikko"
                                     "valiotsikko" "väliotsikko"}})

(defonce key-value-pairs {"teksti" "teksti"
                          "kappale" "kappale"
                          "osio.osioteksti1" "osio1"
                          "osio.osioteksti2" "osio2"
                          "otsikko.paaotsikko" "pääotsikko"
                          "otsikko.valiotsikko" "väliotsikko"})

(defonce lokalisation [{:category "konfo" :key "teksti" :value "teksti" :locale "fi"},
                       {:category "konfo" :key "kappale" :value "kappale" :locale "fi"}
                       {:category "konfo" :key "osio.osioteksti1" :value "osio1" :locale "fi"}
                       {:category "konfo" :key "osio.osioteksti2" :value "osio2" :locale "fi"}
                       {:category "konfo" :key "otsikko.paaotsikko" :value "pääotsikko" :locale "fi"}
                       {:category "konfo" :key "otsikko.valiotsikko" :value "väliotsikko" :locale "fi"}])

(deftest lokalisointi-util-test
  (testing "Lokalisointi util should"
    (testing "transform json to key-value-pairs"
      (is (= key-value-pairs (util/nested-json->key-value-pairs translation-json))))
    (testing "transform key-value-pairs to json"
      (is (= translation-json (util/key-value-pairs->nested-json key-value-pairs))))
    (testing "transform lokalisation from service to json"
      (is (= translation-json (util/localisation->nested-json lokalisation))))
    (testing "should fail if key-value-pairs cannot be transformed to valid nested json"
      (is (thrown? Exception (util/key-value-pairs->nested-json (merge key-value-pairs {"osio" "osio"}))) ))))

(deftest lokalistointi-serv

  (defn comp [x y] (compare (:key x) (:key y)))
  (def sent (atom []))

  (with-redefs [kouta-indeksoija-service.rest.cas.session/cas-authenticated-request-as-json
                (fn [x y z req] (swap! sent conj (cheshire/parse-string (:body req) true)))]

    (testing "Lokalisointi-service should post json to lokalisation service"
      (reset! sent [])
      (service/save-translation-json-to-localisation-service "konfo" "fi" translation-json)
      (is (= (sort comp lokalisation) (sort comp @sent))))

    (testing "Lokalisointi-service should post translation keys to lokalisation service"
      (reset! sent [])
      (service/save-translation-keys-to-localisation-service "konfo" "fi" key-value-pairs)
      (is (= (sort comp lokalisation) (sort comp @sent))))))