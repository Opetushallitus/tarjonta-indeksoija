(ns konfo-indeksoija-service.api
  (:require [konfo-indeksoija-service.elastic.perf :as perf]
            [konfo-indeksoija-service.elastic.admin :as admin]
            [konfo-indeksoija-service.elastic.docs :as docs]
            [konfo-indeksoija-service.elastic.tools :refer [init-elastic-client]]
            [konfo-indeksoija-service.util.conf :refer [env]]
            [konfo-indeksoija-service.util.logging :as logging]
            [konfo-indeksoija-service.indexer.index :as i]
            [konfo-indeksoija-service.indexer.job :as j]
            [konfo-indeksoija-service.queue.job :as qjob]
            [konfo-indeksoija-service.queue.queue :as q]
            [konfo-indeksoija-service.indexer.queue :as queue]
            [konfo-indeksoija-service.s3.s3-client :as s3-client]
            [konfo-indeksoija-service.kouta.indexer :as kouta]
            [clj-log.error-log :refer [with-error-logging]]
            [ring.middleware.cors :refer [wrap-cors]]
            [compojure.api.sweet :refer :all]
            [compojure.route :as route]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [mount.core :as mount]
            [clojure.tools.logging :as log]
            [compojure.api.exception :as ex]
            [environ.core]))

(defn init []
  (mount/start)
  (log/info "Running init")
  (intern 'clj-log.error-log 'test false)
  (if (not= (:s3-dev-disabled env) "true")
    (s3-client/init-s3-connection)
    (log/info "s3 bucket disabled for dev usage - no pictures will be saved."))
  (init-elastic-client)
  (if (and (admin/check-elastic-status)
           (admin/initialize-indices))
    (do
      (j/start-indexer-job)
      (qjob/start-handle-dlq-job)
      (q/index-from-queue!))
    (do
      (log/error "Application startup canceled due to Elastic client error or absence.")
      (System/exit 0))))

(defn stop []
  (j/reset-jobs)
  (mount/stop))

(def service-api
  (api
   {:swagger {:ui "/konfo-indeksoija"
              :spec "/konfo-indeksoija/swagger.json"
              :data {:info {:title "konfo-indeksoija"
                            :description "Elasticsearch wrapper for tarjonta api."}}}
    :exceptions {:handlers {:compojure.api.exception/default logging/error-handler*}}}
   (context "/konfo-indeksoija/api" []

     (GET "/healthcheck" []
       :summary "Healthcheck API."
       (ok "OK"))

     (context "/kouta" []
       :tags ["kouta"]

       (POST "/all" []
         :query-params [{since :- Long 0}]
         :summary "Indeksoi uudet ja muuttuneet koulutukset, hakukohteet, haut ja organisaatiot kouta-backendistä. Default kaikki."
         (ok {:result (if (= 0 since)
                        (kouta/index-all)
                        (kouta/index-since since))})))

     (context "/admin" []
       :tags ["admin"]

       (GET "/koulutus" []
         :summary "Hakee yhden koulutuksen oidin perusteella."
         :query-params [oid :- String]
         (ok {:result (docs/get-koulutus oid)}))

       (GET "/hakukohde" []
         :summary "Hakee yhden hakukohteen oidin perusteella."
         :query-params [oid :- String]
         (ok {:result (docs/get-hakukohde oid)}))

       (GET "/haku" []
         :summary "Hakee yhden haun oidin perusteella."
         :query-params [oid :- String]
         (ok {:result (docs/get-haku oid)}))

       (GET "/organisaatio" []
         :summary "Hakee yhden organisaation oidin perusteella."
         :query-params [oid :- String]
         (ok {:result (docs/get-organisaatio oid)}))

       (GET "/eperuste" []
         :summary "Hakee yhden ePerusteen oidin (idn) perusteella."
         :query-params [oid :- String]
         (ok {:result (docs/get-eperuste oid)}))

       (GET "/status" []
         :summary "Hakee klusterin ja indeksien tiedot."
         (ok {:result (admin/get-elastic-status)}))

       (GET "/performance_info" []
         :summary "Hakee tietoja performanssista"
         :query-params [{since :- Long 0}]
         (ok {:result (perf/get-elastic-performance-info since)}))

       (GET "/s3/koulutus" []
         :summary "Hakee yhden koulutuksen kuvat ja tallentaa ne s3:een"
         :query-params [oid :- String]
         (ok {:result (i/store-picture {:oid oid :type "koulutus"})}))

       (GET "/s3/organisaatio" []
         :summary "Hakee yhden koulutuksen kuvat ja tallentaa ne s3:een"
         :query-params [oid :- String]
         (ok {:result (i/store-picture {:oid oid :type "organisaatio"})}))

       (GET "/query" []
         :summary "Tekee haun haluttuun indeksiin"
         :query-params [index :- String
                        query :- String]
         (ok (admin/search index query))))

     (context "/indexer" []
       :tags ["indexer"]
       (GET "/start" []
         :summary "Käynnistää indeksoinnin taustaoperaation."
         (ok {:result (j/start-stop-indexer true)}))

       (GET "/stop" []
         :summary "Sammuttaa indeksoinnin taustaoperaation."
         (ok {:result (j/start-stop-indexer false)})))

     (context "/queue" []
       :tags ["queue"]
       (GET "/all" []
         :summary "Lisää kaikki vanhan tarjonnan koulutukset, haut ja hakukohteet sekä organisaatiot ja eperusteet indeksoitavien listalle."
         (ok {:result (queue/queue-all)}))

       (GET "/eperusteet" []
         :summary "Lisää kaikki eperusteet indeksoitavien listalle"
         (ok {:result (queue/queue-all-eperusteet)}))

       (GET "/eperusteet" []
         :summary "Lisää kaikki organisaatiot indeksoitavien listalle"
         (ok {:result (queue/queue-all-organisaatiot)}))

       (GET "/koulutus" []
         :summary "Lisää koulutuksen indeksoitavien listalle."
         :query-params [oid :- String]
         (ok {:result (queue/queue "koulutus" oid)}))

       (GET "/hakukohde" []
         :summary "Lisää hakukohteen indeksoitavien listalle."
         :query-params [oid :- String]
         (ok {:result (queue/queue "hakukohde" oid)}))

       (GET "/haku" []
         :summary "Lisää haun indeksoitavien listalle."
         :query-params [oid :- String]
         (ok {:result (queue/queue "haku" oid)}))

       (GET "/eperuste" []
         :summary "Lisää ePerusteen indeksoitavien listalle. (oid==id)"
         :query-params [oid :- String]
         (ok {:result (queue/queue "eperuste" oid)}))

       (GET "/organisaatio" []
         :summary "Lisää organisaation indeksoitavien listalle."
         :query-params [oid :- String]
         (ok {:result (queue/queue "organisaatio" oid)}))

       (GET "/koulutusmoduuli" []
         :summary "Lisää koulutusmoduulin indeksoitavien listalle."
         :query-params [oid :- String]
         (ok {:result (queue/queue "koulutusmoduuli" oid)}))

       (GET "/empty" []
         :summary "Tyhjentää indeksoijan jonon. HUOM! ÄLÄ KÄYTÄ, JOS ET TIEDÄ, MITÄ TEET!"
         (ok {:result (queue/empty-queue)}))))

   (undocumented
    ;; Static resources path. (resources/public, /public path is implicit for route/resources.)
    (route/resources "/konfo-indeksoija/"))))

(def app
  (-> service-api
      ;TODO REMOVE CORS SUPPORT WHEN ui APIs are moved to another project
      (wrap-cors :access-control-allow-origin [#"http://localhost:3005"] :access-control-allow-methods [:get])))