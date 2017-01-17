(ns tarjonta-indeksoija-service.api
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [tarjonta-indeksoija-service.elastic-client :as ec]
            [mount.core :as mount]))

(defn init []
  (mount/start))

(defn stop []
  (mount/stop))

(def app
  (api
    {:swagger
     {:ui   "/"
      :spec "/swagger.json"
      :data {:info {:title "Tarjonta-indeksoija-service"
                    :description "TODO kunnon kuvaus"}}}}

    (context "/api" []
      (GET "/hakukohde" []
        :query-params [oid :- String]
        (ok {:result (ec/query "hakukohde_test" "hakukohde_test" :oid oid)}))

      ;; TODO poista
      (POST "/hakukohde" []
        :body [body s/Any]
        (ok {:result (ec/index "hakukohde_test" "hakukohde_test" body)})))))