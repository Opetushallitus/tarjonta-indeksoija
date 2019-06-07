(ns kouta-indeksoija-service.api
  (:require [kouta-indeksoija-service.elastic.admin :as admin]
            [kouta-indeksoija-service.elastic.docs :as docs]
            [kouta-indeksoija-service.elastic.tools :refer [init-elastic-client]]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.indexer.index :as i]
            [kouta-indeksoija-service.indexer.job :as j]
            [kouta-indeksoija-service.queue.job :as qjob]
            [kouta-indeksoija-service.queue.queue :as q]
            [kouta-indeksoija-service.indexer.queue :as queue]
            [kouta-indeksoija-service.s3.s3-client :as s3-client]
            [kouta-indeksoija-service.kouta.indexer :as kouta]
            [kouta-indeksoija-service.kouta.koulutus :as koulutus]
            [kouta-indeksoija-service.kouta.toteutus :as toteutus]
            [kouta-indeksoija-service.kouta.haku :as haku]
            [kouta-indeksoija-service.kouta.hakukohde :as hakukohde]
            [kouta-indeksoija-service.kouta.valintaperuste :as valintaperuste]
            [kouta-indeksoija-service.kouta.koulutus-search :as koulutus-search]
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

(defn error-handler*
  [^Exception e data request]
  (log/error e))

(def service-api
  (api
   {:swagger {:ui "/kouta-indeksoija/swagger"
              :spec "/kouta-indeksoija/swagger.json"
              :data {:info {:title "kouta-indeksoija"
                            :description "Elasticsearch wrapper for tarjonta api."}}}
    :exceptions {:handlers {:compojure.api.exception/default error-handler*}}}
   (context "/kouta-indeksoija/api" []

     (GET "/healthcheck" []
       :summary "Healthcheck API."
       (ok "OK"))

     (context "/kouta" []
       :tags ["kouta"]

       (POST "/all" []
         :query-params [{since :- Long 0}]
         :summary "Indeksoi uudet ja muuttuneet koulutukset, toteutukset, hakukohteet, haut ja valintaperusteet kouta-backendistä. Default kaikki."
         (ok {:result (if (= 0 since)
                        (kouta/index-all)
                        (kouta/index-since since))}))

       (POST "/koulutus" []
         :summary "Indeksoi koulutuksen tiedot kouta-backendistä."
         :query-params [oid :- String]
         (ok {:result (kouta/index-koulutus oid)}))

       (POST "/koulutukset" []
         :summary "Indeksoi kaikki koulutukset kouta-backendistä."
         (ok {:result (kouta/index-all-koulutukset)}))

       (POST "/toteutus" []
         :summary "Indeksoi toteutuksen tiedot kouta-backendistä."
         :query-params [oid :- String]
         (ok {:result (kouta/index-toteutus oid)}))

       (POST "/toteutukset" []
         :summary "Indeksoi kaikki toteutukset kouta-backendistä."
         (ok {:result (kouta/index-all-toteutukset)}))

       (POST "/hakukohde" []
         :summary "Indeksoi hakukohteen tiedot kouta-backendistä."
         :query-params [oid :- String]
         (ok {:result (kouta/index-hakukohde oid)}))

       (POST "/hakukohteet" []
         :summary "Indeksoi kaikki hakukohteet kouta-backendistä."
         (ok {:result (kouta/index-all-hakukohteet)}))

       (POST "/haku" []
         :summary "Indeksoi haun tiedot kouta-backendistä."
         :query-params [oid :- String]
         (ok {:result (kouta/index-haku oid)}))

       (POST "/haut" []
         :summary "Indeksoi kaikki haut kouta-backendistä."
         (ok {:result (kouta/index-all-haut)}))

       (POST "/valintaperuste" []
         :summary "Indeksoi valintaperusteen tiedot kouta-backendistä."
         :query-params [oid :- String]
         (ok {:result (kouta/index-valintaperuste oid)})))

     (POST "/valintaperusteet" []
       :summary "Indeksoi kaikki valintaperusteet kouta-backendistä."
       (ok {:result (kouta/index-all-valintaperusteet)}))

     (context "/admin" []
       :tags ["admin"]

       (GET "/koulutus" []
         :summary "Hakee yhden koulutuksen oidin perusteella. (HUOM! Täällä on koulutuksia, jotka eivät näy oppijan koulutushaussa)"
         :query-params [oid :- String]
         (ok {:result (koulutus/get oid)}))

       (GET "/toteutus" []
         :summary "Hakee yhden toteutukset oidin perusteella."
         :query-params [oid :- String]
         (ok {:result (toteutus/get oid)}))

       (GET "/hakukohde" []
         :summary "Hakee yhden hakukohteen oidin perusteella."
         :query-params [oid :- String]
         (ok {:result (hakukohde/get oid)}))

       (GET "/haku" []
         :summary "Hakee yhden haun oidin perusteella."
         :query-params [oid :- String]
         (ok {:result (haku/get oid)}))

       (GET "/valintaperuste" []
         :summary "Hakee yhden valintaperusteen id:n perusteella."
         :query-params [id :- String]
         (ok {:result (valintaperuste/get id)}))

       (GET "/koulutus-haku" []
         :summary "Hakee yhden koulutuksen tiedot koulutusten hakuindeksistä (oppijan koulutushaku) oidin perusteella. Vain julkaistuja koulutuksia."
         :query-params [oid :- String]
         (ok {:result (koulutus-search/get oid)}))

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

       (GET "/s3/organisaatio" []
         :summary "Hakee yhden organisaation kuvat ja tallentaa ne s3:een"
         :query-params [oid :- String]
         (ok {:result (i/store-picture {:oid oid :type "organisaatio"})}))

       (GET "/query" []
         :summary "Tekee haun haluttuun indeksiin"
         :query-params [index :- String
                        query :- String]
         (ok (admin/search index query)))

       (POST "/reset" []
         :summary "Resetoi/tyhjentää halutun indeksin. HUOM! ÄLÄ KÄYTÄ, JOS ET TIEDÄ, MITÄ TEET!"
         :query-params [index :- String]
         (ok (admin/reset-index index))))

     (context "/indexer" []
       :tags ["indexer"]
       (GET "/all" []
         :summary "Lisää kaikki organisaatiot ja eperusteet indeksoitavien listalle."
         (ok {:result (queue/queue-all)}))

       (GET "/eperusteet" []
         :summary "Lisää kaikki eperusteet indeksoitavien listalle"
         (ok {:result (queue/queue-all-eperusteet)}))

       (GET "/organisaatiot" []
         :summary "Lisää kaikki organisaatiot indeksoitavien listalle"
         (ok {:result (queue/queue-all-organisaatiot)}))

       (GET "/eperuste" []
         :summary "Lisää ePerusteen indeksoitavien listalle. (oid==id)"
         :query-params [oid :- String]
         (ok {:result (queue/queue "eperuste" oid)}))

       (GET "/organisaatio" []
         :summary "Lisää organisaation indeksoitavien listalle."
         :query-params [oid :- String]
         (ok {:result (queue/queue "organisaatio" oid)}))

       (GET "/empty" []
         :summary "Tyhjentää indeksoijan jonon. HUOM! ÄLÄ KÄYTÄ, JOS ET TIEDÄ, MITÄ TEET!"
         (ok {:result (queue/empty-queue)}))

       (GET "/start" []
         :summary "Käynnistää indeksoinnin taustaoperaation."
         (ok {:result (j/start-stop-indexer true)}))

       (GET "/stop" []
         :summary "Sammuttaa indeksoinnin taustaoperaation."
         (ok {:result (j/start-stop-indexer false)}))))

   (undocumented
    ;; Static resources path. (resources/public, /public path is implicit for route/resources.)
    (route/resources "/kouta-indeksoija/"))))

(def app
  (-> service-api
      ;TODO REMOVE CORS SUPPORT WHEN ui APIs are moved to another project
      (wrap-cors :access-control-allow-origin [#"http://localhost:3005"] :access-control-allow-methods [:get])))
