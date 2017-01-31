(ns tarjonta-indeksoija-service.api-test
  (:require [tarjonta-indeksoija-service.api :refer :all]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [tarjonta-indeksoija-service.test-tools :as tools :refer [parse-body]]
            [tarjonta-indeksoija-service.indexer :as indexer]
            [mocks.tarjonta-mock :refer [get-doc]]
            [mocks.index-mock :refer [reindex-mock]]
            [cheshire.core :as cheshire]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]))

(facts "Api should"
  (against-background [(before :contents (elastic-client/delete-index "hakukohde"))
                       (after :contents [(indexer/reset-jobs)])]
    (fact "reindex hakukohde"
      (with-redefs [tarjonta-indeksoija-service.api/reindex reindex-mock]
        (indexer/start-indexer-job)
        (let [response (app (-> (mock/request :get  "/tarjonta-indeksoija/api/reindex/hakukohde?hakukohdeOid=1.2.246.562.20.28810946823")))
              body     (parse-body (:body response))]
          (:status response) => 200))
      (tools/block-until-indexed 10000)
      (elastic-client/get-queue) => [])

    (tools/refresh-and-wait "hakukohde" 2000)
    (fact "fetch hakukohde"
      (let [response (app (-> (mock/request :get  "/tarjonta-indeksoija/api/hakukohde?oid=1.2.246.562.20.28810946823")))
            body     (parse-body (:body response))]
        (println response)
        (:hakuOid body) => "1.2.246.562.29.44465499083"))))