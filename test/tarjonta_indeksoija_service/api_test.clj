(ns tarjonta-indeksoija-service.api-test
  (:require [tarjonta-indeksoija-service.api :refer :all]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [tarjonta-indeksoija-service.test-tools :as tools :refer [parse-body reset-test-data]]
            [tarjonta-indeksoija-service.indexer :as indexer]
            [mocks.tarjonta-mock :refer [get-doc with-tarjonta-mock]]
            [mocks.index-mock :refer [reindex-mock]]
            [cheshire.core :as cheshire]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]))

(facts "Api should"
  (against-background [(after :contents (reset-test-data))]
    (fact "reindex hakukohde"
      ;; This test uses tarjonta QA
      ;; TODO: try to mock tarjonta in this test..
      (with-tarjonta-mock
        (with-redefs [tarjonta-indeksoija-service.api/reindex reindex-mock]
          (indexer/start-indexer-job)
          (let [response (app (mock/request :get  "/tarjonta-indeksoija/api/reindex/hakukohde?hakukohdeOid=1.2.246.562.20.28810946823"))
                body     (parse-body (:body response))]
            (:status response) => 200)))
      (tools/block-until-indexed 10000)
      (elastic-client/get-queue) => [])

    (tools/refresh-and-wait "hakukohde" 2000)
    (fact "fetch hakukohde"
      ;; uses result from previous test.
      (let [response (app (mock/request :get  "/tarjonta-indeksoija/api/august/hakukohde?oid=1.2.246.562.20.28810946823"))
            body     (parse-body (:body response))]
        (:hakuOid body) => "1.2.246.562.29.44465499083"))

    (fact "fetch koulutus tulos"
      (elastic-client/delete-index "hakukohde")
      (with-tarjonta-mock
        (elastic-client/upsert-indexdata [{:type "koulutus" :oid "1.2.246.562.17.53874141319"}
                                          {:type "hakukohde" :oid "1.2.246.562.20.67506762722"}
                                          {:type "hakukohde" :oid "1.2.246.562.20.715691882710"}
                                          {:type "hakukohde" :oid "1.2.246.562.20.82790530479"}
                                          {:type "hakukohde" :oid "1.2.246.562.20.17663370199"}
                                          {:type "haku" :oid "1.2.246.562.29.86197271827"}
                                          {:type "haku" :oid "1.2.246.562.29.59856749474"}
                                          {:type "haku" :oid "1.2.246.562.29.53522498558"}
                                          {:type "organisaatio" :oid "1.2.246.562.10.39920288212"}])
        (tools/block-until-indexed 10000))

      (tools/refresh-and-wait "hakukohde" 0)
      (tools/refresh-and-wait "haku" 0)
      (tools/refresh-and-wait "organisaatio" 0)
      (tools/refresh-and-wait "koulutus" 1000)
      (let [response (app (mock/request :get  "/tarjonta-indeksoija/api/ui/koulutus/1.2.246.562.17.53874141319"))
            body (parse-body (:body response))
            koulutus (:koulutus body)
            haut (:haut body)
            hakukohteet (:hakukohteet body)
            organisaatiot (:organisaatiot body)]
        (:status response) => 200
        (count hakukohteet) => 4
        (count haut) => 3
        (count organisaatiot) => 1
        (empty? koulutus) => false?

        (:oid koulutus) => "1.2.246.562.17.53874141319"

        (doseq [x (map :koulutukset hakukohteet)]
          x => (contains "1.2.246.562.17.53874141319"))

        (sort (distinct (map :hakuOid hakukohteet))) => (sort (map :oid haut))))))