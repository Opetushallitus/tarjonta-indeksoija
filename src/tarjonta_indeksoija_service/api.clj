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
            [compojure.api.exception :as ex]
            [tarjonta-indeksoija-service.organisaatio-client :as organisaatio-client]))

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



(defn find-docs [index params]
  (cond
    (= "organisaatio" index) (organisaatio-client/find-docs params)
    :else (tarjonta-client/find-docs index params)))

(defn reindex
  [index params]
  (let [docs (find-docs index params)]
    (elastic-client/upsert-indexdata docs)))

(defn get-koulutus-tulos
  [koulutus-oid]
  (let [koulutus (elastic-client/get-koulutus koulutus-oid)
        hakukohteet (elastic-client/get-hakukohteet-by-koulutus koulutus-oid)
        haut (elastic-client/get-haut-by-oids (map :hakuOid hakukohteet))]
    {:koulutus koulutus
     :haut haut
     :hakukohteet hakukohteet}))

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
          :tags ["august"]

          (GET "/koulutus" []
            :summary "Hakee yhden koulutuksen oidin perusteella."
            :query-params [oid :- String]
            (ok {:result (elastic-client/get-koulutus oid)}))

          (GET "/hakukohde" []
            :summary "Hakee yhden hakukohteen oidin perusteella."
            :query-params [oid :- String]
            (ok {:result (elastic-client/get-hakukohde oid)}))

          (GET "/haku" []
            :summary "Hakee yhden haun oidin perusteella."
            :query-params [oid :- String]
            (ok {:result (elastic-client/get-haku oid)}))

          (GET "/orgaisaatio" []
            :summary "Hakee yhden organisaation oidin perusteella."
            :query-params [oid :- String]
            (ok {:result (elastic-client/get-organisaatio oid)})))

        (context "/indexer" []
          :tags ["indexer"]
          (GET "/start" []
            :summary "Käynnistää indeksoinnin taustaoperaation."
            (ok {:result (indexer/start-stop-indexer true)}))

          (GET "/stop" []
            :summary "Sammuttaa indeksoinnin taustaoperaation."
            (ok {:result (indexer/start-stop-indexer false)})))

        (context "/reindex" []
          :tags ["reindex"]
          (GET "/koulutus" {params :params}
            :summary "Lisää koulutuksen indeksoitavien listalle."
            :query-params [koulutusOid :- String]
            (ok {:result (reindex "koulutus" params)}))

          (GET "/hakukohde" {params :params}
            :summary "Lisää hakukohteen indeksoitavien listalle."
            :query-params [hakukohdeOid :- String]
            (ok {:result (reindex "hakukohde" params)}))

          (GET "/haku" {params :params}
            :summary "Lisää haun indeksoitavien listalle."
            :query-params [oid :- String]
            (ok {:result (reindex "haku" params)}))

          (GET "/organisaatio" {params :params}
            :summary "Lisää organisaation indeksoitavien listalle."
            :query-params [oid :- String]
            (ok {:result (reindex "organisaatio" params)})))

        (context "/ui" []
          :tags ["ui"]
          (GET "/koulutus/:oid" []
            :summary "Koostaa koulutuksen sekä siihen liittyien hakukohteiden ja hakujen tiedot."
            :path-params [oid :- String]
            (ok {:result (get-koulutus-tulos oid)}))))


      (undocumented
        (route/resources "/tarjonta-indeksoija/")))))