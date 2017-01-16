(ns tarjonta-indeksoija-service.api
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [tarjonta-indeksoija-service.elastic-client :as ec]))

(def app
  (api
    {:swagger
     {:ui   "/"
      :spec "/swagger.json"
      :data {:info {:title       "Tarjonta-indeksoija-service"
                    :description "TODO kunnon kuvaus"}}}}

    (context "/api" []
      (GET "/koulutus" []
        :query-params [oid :- String]
        (ok {:result (ec/query oid)})))))