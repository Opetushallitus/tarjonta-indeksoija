(ns tarjonta-indeksoija-service.api
  (:require [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [tarjonta-indeksoija-service.conf :refer [env]]
            [tarjonta-indeksoija-service.util.logging :as logging]
            [tarjonta-indeksoija-service.indexer :as indexer]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta-client]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [mount.core :as mount]
            [taoensso.timbre :as timbre]
            [ring.logger.timbre :as logger.timbre]
            [compojure.api.exception :as ex]))

(defn init []
  (mount/start)
  (timbre/set-config! (logging/logging-config))

  (indexer/start-indexer-job))

(defn stop []
  (mount/stop))

(defn- reindex
  [index params]
  (let [docs (tarjonta-client/find-docs index params)]
    (elastic-client/bulk-upsert "indexdata" "indexdata" docs)))

(def app
  (logger.timbre/wrap-with-logger
    (api
      {:swagger {:ui   "/tarjonta-indeksoija"
                 :spec "/tarjonta-indeksoija/swagger.json"
                 :data {:info {:title       "Tarjonta-indeksoija"
                               :description "Elasticsearch wrapper for tarjonta api."}}}
       :exceptions {:handlers {:compojure.api.exception/default logging/error-handler*}}}

      (context "/tarjonta-indeksoija/api" []
        (GET "/hakukohde" []
          :query-params [oid :- String]
          (ok {:result (elastic-client/get-by-id "hakukohde" "hakukohde" oid)}))

        (context "/indexer" []
          (GET "/start" []
            (ok {:result (indexer/start-stop-indexer true)}))

          (GET "/stop" []
            (ok {:result (indexer/start-stop-indexer false)})))

        (context "/reindex" []
          (GET "/koulutus" {params :params}
            (ok {:result (reindex "koulutus" params)}))

          (GET "/hakukohde" {params :params}
            (ok {:result (reindex "hakukohde" params)}))

          (GET "/haku" {params :params}
            (ok {:result (reindex "haku" params)})))))))