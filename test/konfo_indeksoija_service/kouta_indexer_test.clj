(ns konfo-indeksoija-service.kouta-indexer-test
  (:require [konfo-indeksoija-service.kouta.indexer :as i]
            [konfo-indeksoija-service.kouta.koulutus :as k]
            [konfo-indeksoija-service.kouta.koulutus-search :as s]
            [konfo-indeksoija-service.kouta.toteutus :as toteutus]
            [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [konfo-indeksoija-service.kouta.common :as common]
            [konfo-indeksoija-service.rest.koodisto :as koodisto-service]
            [konfo-indeksoija-service.elastic.tools :refer [get-by-id]]
            [cheshire.core :as cheshire]
            [konfo-indeksoija-service.elastic.admin :refer [initialize-indices]]
            [konfo-indeksoija-service.test-tools :as tools]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [mocks.externals-mock :refer [with-externals-mock]]
            [midje.sweet :refer :all]))

(defn json
  [name]
  (tools/parse (str "test/resources/kouta/" name ".json")))

(defn mock-koodisto
  ([koodisto koodi-uri]
   (if koodi-uri
     { :koodiUri koodi-uri :nimi {:fi (str koodi-uri " nimi fi") :sv (str koodi-uri " nimi sv")}}))
  ([koodi-uri]
   (if koodi-uri
     (mock-koodisto (subs koodi-uri 0 (clojure.string/index-of koodi-uri "_")) koodi-uri))))

(defn mock-toteutukset
  ([oid]
   (json (str oid "-toteutukset")))
  ([oid vainJulkaistut]
   (if vainJulkaistut
     (filter #(= (:tila %) "julkaistu") (mock-toteutukset oid))
     (mock-toteutukset oid))))

(defn no-timestamp
  [json]
  (dissoc json :timestamp))

(defn read
  [index id]
  (get-by-id index index id))

(defn debug-pretty
  [json]
  (println (cheshire/generate-string json {:pretty true})))

(against-background
  [(before :contents (init-elastic-test))
   (after :facts (tools/reset-kouta-test-data))
   (after :contents (stop-elastic-test))]

  (with-redefs [common/muokkaaja (fn [x] {:nimi "Kalle Ankka"})
                koodisto-service/get-koodi-nimi-with-cache mock-koodisto
                kouta-backend/get-toteutus-list-for-koulutus mock-toteutukset]
    (let [koulutus-oid "1.2.246.562.13.00000000000000000001"
          toteutus-oid "1.2.246.562.17.00000000000000000001"]

      (fact "Indexer should index tallennettu koulutus only to koulutus index"
        (with-redefs [kouta-backend/get-koulutus (fn [oid] (json oid))]
            (i/index-koulutus koulutus-oid)
            (read s/index-name koulutus-oid) => nil
            (no-timestamp (read k/index-name koulutus-oid)) => (no-timestamp (json "kouta-koulutus-result"))))

      (fact "Indexer should index julkaistu koulutus also to search index"
        (with-redefs [kouta-backend/get-koulutus (fn [oid] (merge (json oid) {:tila "julkaistu"}))
                      kouta-backend/get-hakutiedot-for-koulutus (fn [oid] (json (str oid "-hakutiedot")))]
            (i/index-koulutus koulutus-oid)
            (no-timestamp (read s/index-name koulutus-oid)) => (no-timestamp (json "kouta-koulutus-search-result"))
            (no-timestamp (read k/index-name koulutus-oid)) => (no-timestamp (merge (json "kouta-koulutus-result") {:tila "julkaistu"}))))

      (fact "Indexer should index toteutus to toteutus index and update koulutus indexes"
        (with-redefs [kouta-backend/get-toteutus (fn [oid] (json oid))
                      kouta-backend/list-haut-by-toteutus (fn [oid] (json (str oid "-haut")))
                      kouta-backend/get-koulutus (fn [oid] (merge (json oid) {:tila "julkaistu"}))
                      kouta-backend/get-hakutiedot-for-koulutus (fn [oid] (json (str oid "-hakutiedot")))]
          (i/index-toteutus toteutus-oid)
          (no-timestamp (read toteutus/index-name toteutus-oid)) => (no-timestamp (json "kouta-toteutus-result"))
          (:oid (read s/index-name koulutus-oid)) => koulutus-oid
          (:oid (read k/index-name koulutus-oid)) => koulutus-oid)))))