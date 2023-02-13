(ns kouta-indeksoija-service.cache.thread-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.test-tools :as tools]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as o]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]))
(import '(java.util.concurrent Executors))

(defn mock-get-all-organisaatiot []
  (println "!!!!!!!!!!!!!!!!!!!!! möck")
  (tools/parse (str "test/resources/organisaatiot/hierarkia.json")))

(deftest thread-test
  (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-all-organisaatiot mock-get-all-organisaatiot]
    (testing "playing with threads"
      (let [pool  (Executors/newFixedThreadPool 10)
            tasks (map (fn [t]
                         (fn [] (println "!!!!!!!!!!!!!!!!!! " + (cache/get-hierarkia-item "1.2.246.562.10.54453921329")))) (range 10))]
        (.invokeAll pool tasks)
        (.shutdown pool)))))
