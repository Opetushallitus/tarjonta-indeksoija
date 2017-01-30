(ns tarjonta-indeksoija-service.api-test
  (:require [tarjonta-indeksoija-service.api :refer :all]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [tarjonta-indeksoija-service.test-tools :refer [parse-body]]
            [mocks.tarjonta-mock :refer [get-doc]]
            [cheshire.core :as cheshire]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]))

(facts "Api should"
  (against-background [(before :contents (elastic-client/delete-index "hakukohde"))]
    (fact "fetch hakukohde"
      (elastic-client/bulk-upsert "hakukohde" "hakukohde" [(get-doc {:type "hakukohde" :oid "1.2.246.562.20.28810946823"})])
      (elastic-client/refresh-index "hakukohde")
      (let [response (app (-> (mock/request :get  "/tarjonta-indeksoija/api/hakukohde?oid=1.2.246.562.20.28810946823")))
            body     (parse-body (:body response))]
        (println response)
        (:hakuOid body) => "1.2.246.562.29.44465499083"))))