(ns kouta-indeksoija-service.api
  (:require [kouta-indeksoija-service.elastic.admin :as admin]
            [kouta-indeksoija-service.elastic.tools :refer [init-elastic-client]]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.indexer.indexer :as indexer]
            [kouta-indeksoija-service.indexer.kouta.koulutus :as koulutus]
            [kouta-indeksoija-service.indexer.kouta.toteutus :as toteutus]
            [kouta-indeksoija-service.indexer.kouta.haku :as haku]
            [kouta-indeksoija-service.indexer.kouta.hakukohde :as hakukohde]
            [kouta-indeksoija-service.indexer.kouta.valintaperuste :as valintaperuste]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos :as oppilaitos]
            [kouta-indeksoija-service.indexer.kouta.sorakuvaus :as sorakuvaus]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus-search]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos-search]
            [kouta-indeksoija-service.indexer.eperuste.eperuste :as eperuste]
            [kouta-indeksoija-service.indexer.eperuste.osaamisalakuvaus :as osaamisalakuvaus]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as organisaatio-cache]
            [kouta-indeksoija-service.indexer.lokalisointi.lokalisointi :as lokalisointi]
            [kouta-indeksoija-service.lokalisointi.service :as lokalisointi-service]
            [clj-log.error-log :refer [with-error-logging]]
            [ring.middleware.cors :refer [wrap-cors]]
            [compojure.api.sweet :refer :all]
            [compojure.route :as route]
            [ring.util.http-response :refer :all]
            [mount.core :as mount]
            [clojure.tools.logging :as log]
            [environ.core]
            [kouta-indeksoija-service.scheduled.jobs :as jobs]
            [kouta-indeksoija-service.queuer.queuer :as queuer]
            [kouta-indeksoija-service.notifier.notifier :as notifier]
            [kouta-indeksoija-service.util.tools :refer [comma-separated-string->vec]]
            [kouta-indeksoija-service.queue.admin :as sqs]
            [schema.core :as schema]))

(schema/defschema Indices [schema/Str])

(defn init []
  (mount/start)
  (log/info "Running init")
  (init-elastic-client)
  (if (and (admin/initialize-cluster-settings)
           (admin/check-elastic-status)
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

(defn- with-notifications
  [keyword notify result]
  (if notify (notifier/notify {keyword result}))
  (ok {:result result}))

(def app
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

     (GET "/healthcheck/deep" []
       :summary "Palauttaa 500, jos sqs-jonot tai elasticsearch ei ole terveitä"
       (let [[sqs-healthy? sqs-body] (sqs/healthcheck)
             [els-healthy? els-body] (admin/healthcheck)
             body {:sqs-health sqs-body :elasticsearch-health els-body}]
         (if (and sqs-healthy? els-healthy?)
           (ok body)
           (internal-server-error body))))

     (context "/rebuild" []
       :tags ["rebuild"]

       (GET "/indices/list" []
         :summary "Listaa kaikki indeksit ja niihin liitetyt aliakset"
         (ok (admin/list-indices-and-aliases)))

       (GET "/indices/list/virkailija" []
         :summary "Listaa kaikki virkailija-puolen käyttämät indeksit"
         (ok (admin/list-virkailija-indices)))

       (GET "/indices/list/oppija" []
         :summary "Listaa kaikki oppija-puolen käyttämät indeksit"
         (ok (admin/list-oppija-indices)))

       (GET "/indices/list/unused" []
         :summary "Listaa kaikki indeksit, joita ei enää käytetä (niissä ei ole aliaksia)"
         (ok (admin/list-unused-indices)))

       (POST "/indices/all" []
         :summary "Luo uudelleen kaikki indeksit katkotonta uudelleenindeksointia varten."
         (ok (admin/initialize-all-indices-for-reindexing)))

       (POST "/indices/one" []
         :summary "Luo uudelleen yhden indeksin katkotonta uudelleenindeksointia varten."
         :body [index-name String]
         (try (ok (admin/initialize-new-index-for-reindexing index-name))
              (catch IllegalArgumentException e (bad-request (ex-message e)))))

       (POST "/indices/kouta" []
         :summary "Luo uudelleen kaikki kouta-datan (ja oppilaitosten!) indeksit katkotonta uudelleenindeksointia varten."
         (ok (admin/initialize-kouta-indices-for-reindexing)))

       (POST "/indices/eperuste" []
         :summary "Luo uudelleen kaikki eperuste-datan indeksit katkotonta uudelleenindeksointia varten."
         (ok (admin/initialize-eperuste-indices-for-reindexing)))

       (POST "/indices/koodisto" []
         :summary "Luo uudelleen kaikki koodisto-datan indeksit katkotonta uudelleenindeksointia varten."
         (ok (admin/initialize-koodisto-indices-for-reindexing)))

       (POST "/indices/lokalisointi" []
         :summary "Luo uudelleen kaikki lokalisointi-datan indeksit katkotonta uudelleenindeksointia varten."
         (ok (admin/initialize-lokalisointi-indices-for-reindexing)))

       (DELETE "/indices/unused" []
         :summary "Poistaa kaikki indeksit, joita ei enää käytetä (niissä ei ole aliaksia)"
         (ok (admin/delete-unused-indices)))

       (DELETE "/indices" []
         :summary "Poistaa listatut indeksit."
         :body [indices Indices]
         (ok (admin/delete-indices indices)))

       (POST "/indices/aliases/sync" []
         :summary "Synkkaa oppijan aliakset osoittamaan samoihin indekseihin kuin virkailijan aliakset"
         (ok (admin/sync-all-aliases))))

     (context "/kouta" []
       :tags ["kouta"]

       (POST "/all" []
         :query-params [{since :- Long 0}]
         :summary "Indeksoi uudet ja muuttuneet koulutukset, toteutukset, hakukohteet, haut ja valintaperusteet kouta-backendistä. Default kaikki."
         (ok {:result (if (= 0 since)
                        (indexer/index-all-kouta)
                        (indexer/index-since-kouta since))}))

       (POST "/koulutus" []
         :summary "Indeksoi koulutuksen tiedot kouta-backendistä."
         :query-params [oid :- String
                        {notify :- Boolean false}]
         (with-notifications :koulutukset notify (indexer/index-koulutus oid)))

       (POST "/koulutukset" []
         :summary "Indeksoi kaikki koulutukset kouta-backendistä."
         :query-params [{notify :- Boolean false}]
         (with-notifications :koulutukset notify (indexer/index-all-koulutukset)))

       (POST "/toteutus" []
         :summary "Indeksoi toteutuksen tiedot kouta-backendistä."
         :query-params [oid :- String
                        {notify :- Boolean false}]
         (with-notifications :toteutukset notify (indexer/index-toteutus oid)))

       (POST "/toteutukset" []
         :summary "Indeksoi kaikki toteutukset kouta-backendistä."
         :query-params [{notify :- Boolean false}]
         (with-notifications :toteutukset notify (indexer/index-all-toteutukset)))

       (POST "/hakukohde" []
         :summary "Indeksoi hakukohteen tiedot kouta-backendistä."
         :query-params [oid :- String
                        {notify :- Boolean false}]
         (with-notifications :hakukohteet notify (indexer/index-hakukohde oid)))

       (POST "/hakukohteet" []
         :summary "Indeksoi kaikki hakukohteet kouta-backendistä."
         :query-params [{notify :- Boolean false}]
         (with-notifications :hakukohteet notify (indexer/index-all-hakukohteet)))

       (POST "/haku" []
         :summary "Indeksoi haun tiedot kouta-backendistä."
         :query-params [oid :- String
                        {notify :- Boolean false}]
         (with-notifications :haku notify (indexer/index-haku oid)))

       (POST "/haut" []
         :summary "Indeksoi kaikki haut kouta-backendistä."
         :query-params [{notify :- Boolean false}]
         (with-notifications :haku notify (indexer/index-all-haut)))

       (POST "/valintaperuste" []
         :summary "Indeksoi valintaperusteen tiedot kouta-backendistä."
         :query-params [oid :- String
                        {notify :- Boolean false}]
         (with-notifications :valintaperusteet notify (indexer/index-valintaperuste oid)))

       (POST "/valintaperusteet" []
         :summary "Indeksoi kaikki valintaperusteet kouta-backendistä."
         :query-params [{notify :- Boolean false}]
         (with-notifications :valintaperusteet notify (indexer/index-all-valintaperusteet)))

       (POST "/sorakuvaus" []
         :summary "Indeksoi sorakuvausten tiedot kouta-backendistä."
         :query-params [oid :- String
                        {notify :- Boolean false}]
         (with-notifications :sorakuvaukset notify (indexer/index-sorakuvaus oid)))

       (POST "/sorakuvaukset" []
         :summary "Indeksoi kaikki sorakuvaukset kouta-backendistä."
         :query-params [{notify :- Boolean false}]
         (with-notifications :sorakuvaukset notify (indexer/index-all-sorakuvaukset))))

     (context "/indexed" []
       :tags ["indexed"]

       (GET "/koulutus" []
         :summary "Hakee yhden koulutuksen oidin perusteella. (HUOM! Täällä on koulutuksia, jotka eivät näy oppijan koulutushaussa)"
         :query-params [oid :- String]
         (ok {:result (koulutus/get-from-index oid)}))

       (GET "/toteutus" []
         :summary "Hakee yhden toteutukset oidin perusteella."
         :query-params [oid :- String]
         (ok {:result (toteutus/get-from-index oid)}))

       (GET "/hakukohde" []
         :summary "Hakee yhden hakukohteen oidin perusteella."
         :query-params [oid :- String]
         (ok {:result (hakukohde/get-from-index oid)}))

       (GET "/haku" []
         :summary "Hakee yhden haun oidin perusteella."
         :query-params [oid :- String]
         (ok {:result (haku/get-from-index oid)}))

       (GET "/valintaperuste" []
         :summary "Hakee yhden valintaperusteen id:n perusteella."
         :query-params [id :- String]
         (ok {:result (valintaperuste/get-from-index id)}))

       (GET "/koulutus-haku" []
         :summary "Hakee yhden koulutuksen tiedot koulutusten hakuindeksistä (oppijan koulutushaku) oidin perusteella. Vain julkaistuja koulutuksia."
         :query-params [oid :- String]
         (ok {:result (koulutus-search/get-from-index oid)}))

       (GET "/oppilaitos" []
         :summary "Hakee yhden oppilaitoksen oidin perusteella."
         :query-params [oid :- String]
         (ok {:result (oppilaitos/get-from-index oid)}))

       (GET "/oppilaitos-haku" []
         :summary "Hakee yhden oppilaitoksen tiedot oppilaitosten hakuindeksistä (oppijan oppilaitoshaku) oidin perusteella. Vain julkaistuja oppilaitoksia."
         :query-params [oid :- String]
         (ok {:result (oppilaitos-search/get-from-index oid)}))

       (GET "/sorakuvaus" []
         :summary "Hakee yhden sorakuvauksen oidin perusteella."
         :query-params [id :- String]
         (ok {:result (sorakuvaus/get-from-index id)}))

       (GET "/eperuste" []
         :summary "Hakee yhden ePerusteen oidin (idn) perusteella."
         :query-params [oid :- String]
         (ok {:result (eperuste/get-from-index oid)}))

       (GET "/osaamisalakuvaus" []
         :summary "Hakee yhden osaamisalakuvaus oidin (idn) perusteella."
         :query-params [oid :- String]
         (ok {:result (osaamisalakuvaus/get-from-index oid)}))

       (GET "/lokalisointi" []
         :summary "Hakee lokalisoinnit annetulla kielellä."
         :query-params [lng :- String]
         (ok {:result (lokalisointi/get-from-index lng)})))

     (context "/admin" []
       :tags ["admin"]

       (GET "/index/status" []
         :summary "Hakee klusterin ja indeksien tiedot."
         (ok {:result (admin/get-elastic-status)}))

       (GET "/aliases/all" []
         :summary "Listaa kaikki indeksit ja niihin liitetyt aliakset."
         (ok (admin/list-indices-and-aliases)))

       (GET "/alias" []
         :summary "Listaa aliakseen liitetyt indeksit"
         :query-params [alias :- String]
         (ok (admin/list-indices-with-alias alias)))

       (POST "/aliases/sync" []
         :summary "Synkkaa kaikki virkailijan ja oppijan aliakset osoittamaan samoihin indekseihin"
         (ok (admin/sync-all-aliases)))

       (GET "/queue/status" []
         :summary "Palauttaa tiedon kaikkien sqs-jonojen tilasta"
         (ok (sqs/status)))

       (GET "/index/query" []
         :summary "Tekee haun haluttuun indeksiin"
         :query-params [index :- String
                        query :- String]
         (ok (admin/search index query)))

       (POST "/index/delete" []
         :summary "Poistaa halutun indeksin. HUOM! ÄLÄ KÄYTÄ, ELLET OLE VARMA, MITÄ TEET!"
         :query-params [index :- String]
         (ok (admin/delete-index index)))

       (POST "/lokalisointi/key-value-pairs" []
         :summary "Konvertoi jsonin avain-arvo-pareiksi käännösten tekemistä varten"
         :body [body (describe schema/Any "JSON-muotoiset käännökset (translation.json)")]
         (ok (lokalisointi-service/->translation-keys body)))

       (POST "/lokalisointi/json" []
         :summary "Konvertoi avain-arvo-parit jsoniksi"
         :body [body (describe schema/Any "Avain-arvo-muotoiset käännökset")]
         (ok (lokalisointi-service/->json body)))

       (POST "/lokalisointi/json/konfo/save" []
         :summary "Tallentaa konfo-ui:n käännöstiedoston (translation.json) lokalisointi-palveluun. Ei ylikirjoita olemassaolevia käännösavaimia."
         :query-params [lng :- String]
         :body [body (describe schema/Any "JSON-muotoiset käännökset")]
         (ok (lokalisointi-service/save-translation-json-to-localisation-service "konfo" lng body)))

       (POST "/lokalisointi/json/kouta/save" []
         :summary "Tallentaa kouta-ui:n käännöstiedoston json-muodossa lokalisointi-palveluun. Ei ylikirjoita olemassaolevia käännösavaimia."
         :query-params [lng :- String]
         :body [body (describe schema/Any "JSON-muotoiset käännökset")]
         (ok (lokalisointi-service/save-translation-json-to-localisation-service "kouta" lng body))))

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
         (ok (jobs/resume-dlq-job)))

       (POST "/pause-sqs" []
         :summary "Keskeyttää prosessin, joka lukee muutoksia sqs-jonosta indeksoitavaksi. HUOM!! Jos prosessin keskeyttää, kouta-tarjonnan muutokset eivät välity oppijan puolelle ja esikatseluun!"
         (ok (jobs/pause-sqs-job)))

       (POST "/resume-sqs" []
         :summary "Käynnistää prosessin, joka lukee muutoksia sqs-jonosta indeksoitavaksi"
         (ok (jobs/resume-sqs-job)))

       (POST "/pause-notification-dlq" []
         :summary "Keskeyttää notifikaatioiden dlq-jonoa siivoavan prosessin"
         (ok (jobs/pause-notification-dlq-job)))

       (POST "/resume-notification-dlq" []
         :summary "Käynnistää notifikaatioiden dlq-jonoa siivoavan prosessin"
         (ok (jobs/resume-notification-dlq-job)))

       (POST "/pause-notification-sqs" []
         :summary "Keskeyttää prosessin, joka lukee notifikaatioita sqs-jonosta ja lähettää ne ulkoisille integraatioille. HUOM!! Jos prosessin keskeyttää, kouta-tarjonnan muutokset eivät välity ulkoisille integraatiolle."
         (ok (jobs/pause-notification-job)))

       (POST "/resume-notification-sqs" []
         :summary "Käynnistää prosessin, joka lukee notifikaatioita sqs-jonosta ja lähettää ne ulkoisille integraatioille."
         (ok (jobs/resume-notification-job)))

       (POST "/pause-queueing" []
         :summary "Keskeyttää prosessin, joka siirtää mm. ePerusteiden ja organisaatioden muutokset sqs-jonoon odottamaan indeksointia"
         (ok (jobs/pause-queueing-job)))

       (POST "/resume-queueing" []
         :summary "Käynnistää prosessin, joka siirtää mm. ePerusteiden ja organisaatioden muutokset sqs-jonoon odottamaan indeksointia"
         (ok (jobs/resume-queueing-job)))

       (POST "/pause-lokalisointi-indexing" []
         :summary "Keskeyttää prosessin, joka indeksoi lokalisointeja lokalisaatiopalvelusta"
         (ok (jobs/pause-lokalisaatio-indexing-job)))

       (POST "/resume-lokalisointi-indexing" []
         :summary "Käynnistää prosessin, joka indeksoi lokalisointeja lokalisaatiopalvelusta"
         (ok (jobs/resume-lokalisaatio-indexing-job))))

     (context "/indexer" []
       :tags ["indexer"]

       (POST "/eperuste" []
         :summary "Indeksoi ePerusteen ja sen osaamisalat (oid==id)"
         :query-params [oid :- String]
         (ok {:result (indexer/index-eperuste oid)}))

       (POST "/organisaatio" []
         :summary "Indeksoi oppilaitoksen"
         :query-params [oid :- String]
         (ok {:result (do (queuer/clear-organisaatio-cache [oid])
                          (indexer/index-oppilaitos oid))}))

       (POST "/koodistot" []
         :summary "Indeksoi (filtereissä käytettävien) koodistojen uusimmat versiot."
         :query-params [{koodistot :- String "maakunta,kunta,oppilaitoksenopetuskieli,kansallinenkoulutusluokitus2016koulutusalataso1,kansallinenkoulutusluokitus2016koulutusalataso2,koulutustyyppi,opetuspaikkakk,hakutapa,valintatapajono,pohjakoulutusvaatimuskonfo"}]
         (ok {:result (indexer/index-koodistot (comma-separated-string->vec koodistot))}))

       (POST "/lokalisointi" []
         :summary "Indeksoi oppijan puolen lokalisoinnit lokalisaatiopalvelusta annetulla kielellä (fi/sv/en)"
         :query-params [lng :- String]
         (ok (indexer/index-lokalisointi lng)))

       (POST "/lokalisointi/kaikki" []
         :summary "Indeksoi kaikki oppijan puolen lokalisoinnit lokalisaatiopalvelusta"
         (ok (indexer/index-all-lokalisoinnit))))

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