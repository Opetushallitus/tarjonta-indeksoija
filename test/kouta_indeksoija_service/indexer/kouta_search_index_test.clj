(ns kouta-indeksoija-service.indexer.kouta-search-index-test
  (:require [clojure.test :refer :all]
            [clojure.data :refer [diff]]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos]
            [kouta-indeksoija-service.elastic.tools :refer [get-by-id]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [cheshire.core :as cheshire]
            [kouta-indeksoija-service.test-tools :refer [parse]]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]))

(defn no-timestamp
  [json]
  (dissoc json :timestamp))

(defn json
  [name]
  (cheshire/parse-string (slurp (str "test/resources/search/" name ".json")) true))

(defn read
  [index id]
  (get-by-id index index id))

(defn debug-pretty
  [json]
  (println (cheshire/generate-string json {:pretty true})))

(defn compare-json
  [expected actual]
  (let [difference (diff expected actual)]
    (is (= nil (first difference)))
    (is (= nil (second difference)))
    (is (= expected actual))))

(use-fixtures :each (fn [test] (do (test) (fixture/reset-indices))))

(let [oppilaitos-oid1 "1.2.246.562.10.10101010199"
      oppilaitos-oid2 "1.2.246.562.10.77777777799"
      koulutus-oid1   "1.2.246.562.13.00000000000000000099"
      koulutus-oid2   "1.2.246.562.13.00000000000000000098"
      toteutus-oid1   "1.2.246.562.17.00000000000000000099"
      toteutus-oid2   "1.2.246.562.17.00000000000000000098"]

  (fixture/add-oppilaitos-mock oppilaitos-oid1 :tila "julkaistu")
  (fixture/add-oppilaitos-mock oppilaitos-oid2 :tila "julkaistu")

  (fixture/add-koulutus-mock koulutus-oid1
                             :tila "julkaistu"
                             :nimi "Autoalan perustutkinto 0"
                             :tarjoajat (str oppilaitos-oid2 "2")
                             :metadata (slurp (str "test/resources/search/koulutus-metadata.json")))

  (fixture/add-koulutus-mock koulutus-oid2
                             :tila "julkaistu"
                             :nimi "Hevosalan perustutkinto 0"
                             :tarjoajat oppilaitos-oid2
                             :metadata (slurp (str "test/resources/search/koulutus-metadata.json")))

  (fixture/add-toteutus-mock toteutus-oid1
                             koulutus-oid2
                             :tila "julkaistu"
                             :nimi "Hevostoteutus 1"
                             :tarjoajat (str oppilaitos-oid2 "1" "," oppilaitos-oid2 "3")
                             :metadata (slurp (str "test/resources/search/toteutus-metadata.json")))

  (fixture/add-toteutus-mock toteutus-oid2
                             koulutus-oid2
                             :tila "julkaistu"
                             :nimi "Hevostoteutus 2"
                             :tarjoajat (str oppilaitos-oid2 "1"))


  (deftest index-oppilaitos-search-items-test-1

    (fixture/with-mocked-indexing
      (testing "Create correct search item when oppilaitos has no koulutukset"
        (is (= nil (read oppilaitos/index-name oppilaitos-oid1)))
        (i/index-oppilaitos oppilaitos-oid1)
        (compare-json (no-timestamp (json "oppilaitos-search-item-no-koulutukset"))
                      (no-timestamp (read oppilaitos/index-name oppilaitos-oid1))))))

  (deftest index-oppilaitos-search-items-test-2

    (fixture/with-mocked-indexing
     (testing "Create correct search item when oppilaitos has koulutukset and toteutukset"
       (is (= nil (read oppilaitos/index-name oppilaitos-oid2)))
       (i/index-oppilaitos oppilaitos-oid2)
       (compare-json (no-timestamp (json "oppilaitos-search-item-koulutus-and-toteutukset"))
                     (no-timestamp (read oppilaitos/index-name oppilaitos-oid2)))))))