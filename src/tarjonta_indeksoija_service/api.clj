(ns tarjonta-indeksoija-service.api
  (:require [tarjonta-indeksoija-service.elastic-client :as ec]
            [tarjonta-indeksoija-service.conf :refer [env]]
            [tarjonta-indeksoija-service.util.logging :as logging]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [mount.core :as mount]
            [taoensso.timbre :as timbre]
            [ring.logger.timbre :as logger.timbre]
            [compojure.api.exception :as ex]))

(defn init []
  (mount/start)
  (timbre/set-config! (logging/logging-config)))

(defn stop []
  (mount/stop))

(def app
  (logger.timbre/wrap-with-logger
    (api
      {:swagger
                   {:ui   "/tarjonta-indeksoija"
                    :spec "/tarjonta-indeksoija/swagger.json"
                    :data {:info {:title       "Tarjonta-indeksoija-service"
                                  :description "TODO kunnon kuvaus"}}}
       :exceptions {:handlers {:compojure.api.exception/default logging/error-handler*}}}

      (context "/tarjonta-indeksoija/api" []
        (GET "/hakukohde" []
          :query-params [oid :- String]
          (ok {:result (ec/query "hakukohde_test" "hakukohde_test" :oid oid)}))

        ;; TODO poista
        (POST "/hakukohde" []
          :body [body s/Any]
          (ok {:result (ec/index "hakukohde_test" "hakukohde_test" body)}))))))