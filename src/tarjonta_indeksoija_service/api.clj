(ns tarjonta-indeksoija-service.api
  (:require [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [tarjonta-indeksoija-service.conf :refer [env]]
            [tarjonta-indeksoija-service.util.logging :as logging]
            [tarjonta-indeksoija-service.indexer :as indexer]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta-client]
            [compojure.api.sweet :refer :all]
            [compojure.route :as route]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [mount.core :as mount]
            [taoensso.timbre :as timbre]
            [ring.logger.timbre :as logger.timbre]
            [compojure.api.exception :as ex]))

(defn init []
  (mount/start)
  (timbre/set-config! (logging/logging-config))
  (if (elastic-client/check-elastic-status)
    (indexer/start-indexer-job)
    (do
      (timbre/error "Application startup canceled due to Elastic client error or absence.")
      (System/exit 0))))

(defn stop []
  (indexer/reset-jobs)
  (mount/stop))

(defn reindex
  [index params]
  (let [docs (tarjonta-client/find-docs index params)]
    (elastic-client/bulk-upsert "indexdata" "indexdata" docs)))

(defn get-koulutus-tulos
  [koulutus-oid]
  (let [koulutus (elastic-client/get-by-id "koulutus" "koulutus" koulutus-oid)
        hakukohteet (elastic-client/get-hakukohteet-by-koulutus koulutus-oid)
        haut (elastic-client/get-haut-by-oids (map :hakuOid hakukohteet))]
    {:koulutus koulutus
     :haut haut
     :hakukoteet hakukohteet}))

(def app
  (logger.timbre/wrap-with-logger
    (api
      {:swagger {:ui   "/tarjonta-indeksoija"
                 :spec "/tarjonta-indeksoija/swagger.json"
                 :data {:info {:title       "Tarjonta-indeksoija"
                               :description "Elasticsearch wrapper for tarjonta api."}}}
       :exceptions {:handlers {:compojure.api.exception/default logging/error-handler*}}}
      (context "/tarjonta-indeksoija/api" []
        (context "/august" []
          (GET "/hakukohde" []
            :query-params [oid :- String]
            (ok {:result (elastic-client/get-by-id "hakukohde" "hakukohde" oid)}))

          (GET "/koulutus" []
            :query-params [oid :- String]
            (ok {:result (elastic-client/get-by-id "hakukohde" "hakukohde" oid)}))

          (GET "/haku" []
            :query-params [oid :- String]
            (ok {:result (elastic-client/get-by-id "hakukohde" "hakukohde" oid)})))

        (context "/indexer" []
          (GET "/start" []
            (ok {:result (indexer/start-stop-indexer true)}))

          (GET "/stop" []
            (ok {:result (indexer/start-stop-indexer false)})))

        (context "/reindex" []
          (GET "/koulutus" {params :params}
            :query-params [koulutusOid :- String]
            (ok {:result (reindex "koulutus" params)}))

          (GET "/hakukohde" {params :params}
            :query-params [hakukohdeOid :- String]
            (ok {:result (reindex "hakukohde" params)}))

          (GET "/haku" {params :params}
            :query-params [oid :- String]
            (ok {:result (reindex "haku" params)})))

        (context "/ui" []
          (GET "/koulutus/:oid" []
            :path-params [oid :- String]
            (ok {:result (get-koulutus-tulos oid)}))))


      (undocumented
        (route/resources "/tarjonta-indeksoija/")))))