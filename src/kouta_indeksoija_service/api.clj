(ns kouta-indeksoija-service.api
  (:require [kouta-indeksoija-service.elastic.admin :as admin]
            [kouta-indeksoija-service.elastic.tools :refer [init-elastic-client]]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.s3.s3-client :as s3-client]
            [kouta-indeksoija-service.indexer.indexer :as indexer]
            [kouta-indeksoija-service.indexer.indexer-api :as indexer-api]
            [kouta-indeksoija-service.indexer.kouta.koulutus :as koulutus]
            [kouta-indeksoija-service.indexer.kouta.toteutus :as toteutus]
            [kouta-indeksoija-service.indexer.kouta.haku :as haku]
            [kouta-indeksoija-service.indexer.kouta.hakukohde :as hakukohde]
            [kouta-indeksoija-service.indexer.kouta.valintaperuste :as valintaperuste]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos :as oppilaitos]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus-search]
            [kouta-indeksoija-service.indexer.eperuste.eperuste :as eperuste]
            [kouta-indeksoija-service.indexer.eperuste.osaamisalakuvaus :as osaamisalakuvaus]
            [clj-log.error-log :refer [with-error-logging]]
            [ring.middleware.cors :refer [wrap-cors]]
            [compojure.api.sweet :refer :all]
            [compojure.route :as route]
            [ring.util.http-response :refer :all]
            [mount.core :as mount]
            [clojure.tools.logging :as log]
            [environ.core]
            [kouta-indeksoija-service.scheduled.jobs :as jobs]
            [kouta-indeksoija-service.queuer.queuer :as queuer]))

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
      (jobs/schedule-jobs))
    (do
      (log/error "Application startup canceled due to Elastic client error or absence.")
      (System/exit 0))))

(defn stop []
  (jobs/reset-jobs)
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
         :query-params [{since :- Long 0}
                        {notify :- Boolean false}]
         :summary "Indeksoi uudet ja muuttuneet koulutukset, toteutukset, hakukohteet, haut ja valintaperusteet kouta-backendistä. Default kaikki."
         (ok {:result (if (= 0 since)
                        (indexer/index-all-kouta)
                        (indexer-api/index-since-kouta since notify))}))

       (POST "/koulutus" []
         :summary "Indeksoi koulutuksen tiedot kouta-backendistä."
         :query-params [oid :- String
                        {notify :- Boolean true}]
         (ok {:result (indexer-api/index-koulutus oid notify)}))

       (POST "/koulutukset" []
         :summary "Indeksoi kaikki koulutukset kouta-backendistä."
         :query-params [{notify :- Boolean false}]
         (ok {:result (indexer-api/index-all-koulutukset notify)}))

       (POST "/toteutus" []
         :summary "Indeksoi toteutuksen tiedot kouta-backendistä."
         :query-params [oid :- String
                        {notify :- Boolean true}]
         (ok {:result (indexer-api/index-toteutus oid notify)}))

       (POST "/toteutukset" []
         :summary "Indeksoi kaikki toteutukset kouta-backendistä."
         :query-params [{notify :- Boolean false}]
         (ok {:result (indexer-api/index-all-toteutukset notify)}))

       (POST "/hakukohde" []
         :summary "Indeksoi hakukohteen tiedot kouta-backendistä."
         :query-params [oid :- String
                        {notify :- Boolean true}]
         (ok {:result (indexer-api/index-hakukohde oid notify)}))

       (POST "/hakukohteet" []
         :summary "Indeksoi kaikki hakukohteet kouta-backendistä."
         :query-params [{notify :- Boolean false}]
         (ok {:result (indexer-api/index-all-hakukohteet notify)}))

       (POST "/haku" []
         :summary "Indeksoi haun tiedot kouta-backendistä."
         :query-params [oid :- String
                        {notify :- Boolean true}]
         (ok {:result (indexer-api/index-haku oid notify)}))

       (POST "/haut" []
         :summary "Indeksoi kaikki haut kouta-backendistä."
         :query-params [{notify :- Boolean false}]
         (ok {:result (indexer-api/index-all-haut notify)}))

       (POST "/valintaperuste" []
         :summary "Indeksoi valintaperusteen tiedot kouta-backendistä."
         :query-params [oid :- String
                        {notify :- Boolean true}]
         (ok {:result (indexer-api/index-valintaperuste oid notify)}))

       (POST "/valintaperusteet" []
         :summary "Indeksoi kaikki valintaperusteet kouta-backendistä."
         :query-params [{notify :- Boolean false}]
         (ok {:result (indexer-api/index-all-valintaperusteet notify)})))

     (context "/indexed" []
       :tags ["indexed"]

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

       (GET "/oppilaitos" []
         :summary "Hakee yhden oppilaitoksen oidin perusteella."
         :query-params [oid :- String]
         (ok {:result (oppilaitos/get oid)}))

       (GET "/eperuste" []
         :summary "Hakee yhden ePerusteen oidin (idn) perusteella."
         :query-params [oid :- String]
         (ok {:result (eperuste/get oid)}))

       (GET "/osaamisalakuvaus" []
         :summary "Hakee yhden osaamisalakuvaus oidin (idn) perusteella."
         :query-params [oid :- String]
         (ok {:result (osaamisalakuvaus/get oid)})))

     (context "/admin" []
       :tags ["admin"]

       (GET "/status" []
         :summary "Hakee klusterin ja indeksien tiedot."
         (ok {:result (admin/get-elastic-status)}))

       (POST "/query" []
         :summary "Tekee haun haluttuun indeksiin"
         :query-params [index :- String
                        query :- String]
         (ok (admin/search index query)))

       (POST "/reset" []
         :summary "Resetoi/tyhjentää halutun indeksin. HUOM! ÄLÄ KÄYTÄ, JOS ET TIEDÄ, MITÄ TEET!"
         :query-params [index :- String]
         (ok (admin/reset-index index))))

     (context "/jobs" []
       :tags ["jobs"]

       (GET "/list" []
         :summary "Listaa tiedot järjestelmän ajastetuista prosesseista"
         (ok (jobs/get-jobs-info)))

       (POST "/pause-dlq" []
         :summary "Keskeyttää dlq-jonoa siivoavan prosessin"
         (ok (jobs/pause-dlq-job)))

       (POST "/resume-dlq" []
         :summary "Käynnistää dlq-jonoa siivoavan prosessin"
         (ok (jobs/pause-dlq-job)))

       (POST "/pause-sqs" []
         :summary "Keskeyttää prosessin, joka lukee muutoksia sqs-jonosta indeksoitavaksi. HUOM!! Jos prosessin keskeyttää, kouta-tarjonnan muutokset eivät välity oppijan puolelle ja esikatseluun!"
         (ok (jobs/pause-dlq-job)))

       (POST "/resume-sqs" []
         :summary "Käynnistää prosessin, joka lukee muutoksia sqs-jonosta indeksoitavaksi"
         (ok (jobs/resume-sqs-job)))

       (POST "/pause-queueing" []
         :summary "Keskeyttää prosessin, joka siirtää mm. ePerusteiden ja organisaatioden muutokset sqs-jonoon odottamaan indeksointia"
         (ok (jobs/pause-queueing-job)))

       (POST "/resume-queueing" []
         :summary "Käynnistää prosessin, joka siirtää mm. ePerusteiden ja organisaatioden muutokset sqs-jonoon odottamaan indeksointia"
         (ok (jobs/pause-queueing-job))))

     (context "/indexer" []
       :tags ["indexer"]

       (POST "/eperusteet" []
         :summary "Indeksoi kaikki ePerusteet ja niiden osaamisalat"
         (ok {:result (indexer-api/index-all-eperusteet)}))

       (POST "/organisaatiot" []
         :summary "Indeksoi kaikki oppilaitokset"
         (ok {:result (indexer-api/index-all-oppilaitokset)}))

       (POST "/eperuste" []
         :summary "Indeksoi ePerusteen ja sen osaamisalat (oid==id)"
         :query-params [oid :- String]
         (ok {:result (indexer-api/index-eperuste oid)}))

       (POST "/organisaatio" []
         :summary "Indeksoi oppilaitoksen"
         :query-params [oid :- String]
         (ok {:result (indexer-api/index-oppilaitos oid)})))

     (context "/queuer" []
       :tags ["queuer"]

       (POST "/eperusteet" []
         :summary "Lisää kaikki ePerusteet ja niiden osaamisalat indeksoinnin jonoon"
         (ok {:result (queuer/queue-all-eperusteet)}))

       (POST "/oppilaitokset" []
         :summary "Lisää kaikki aktiiviset oppilaitokset organisaatiopalvelusta  indeksoinnin jonoon"
         (ok {:result (queuer/queue-all-oppilaitokset-from-organisaatiopalvelu)}))

       (POST "/eperuste" []
         :summary "Lisää ePeruste ja sen osaamisalat (oid==id)  indeksoinnin jonoon"
         :query-params [oid :- String]
         (ok {:result (queuer/queue-eperuste oid)}))

       (POST "/oppilaitos" []
         :summary "Lisää oppilaitos/organisaatio indeksoinnin jonoon"
         :query-params [oid :- String]
         (ok {:result (queuer/queue-oppilaitos oid)}))))

   (undocumented
    ;; Static resources path. (resources/public, /public path is implicit for route/resources.)
    (route/resources "/kouta-indeksoija/"))))

(def app
  (-> service-api
      ;TODO REMOVE CORS SUPPORT WHEN ui APIs are moved to another project
      (wrap-cors :access-control-allow-origin [#"http://localhost:3005"] :access-control-allow-methods [:get])))
