(ns konfo-indeksoija-service.api
  (:require [konfo-indeksoija-service.elastic-client :as elastic-client]
            [konfo-indeksoija-service.conf :refer [env]]
            [konfo-indeksoija-service.util.logging :as logging]
            [konfo-indeksoija-service.indexer :as indexer]
            [konfo-indeksoija-service.s3-client :as s3-client]
            [konfo-indeksoija-service.tarjonta-client :as tarjonta-client]
            [konfo-indeksoija-service.organisaatio-client :as organisaatio-client]
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
  (elastic-client/init-elastic-client)
  (if (and (elastic-client/check-elastic-status)
           (elastic-client/initialize-indices))
    (indexer/start-indexer-job)
    (do
      (log/error "Application startup canceled due to Elastic client error or absence.")
      (System/exit 0))))

(defn stop []
  (indexer/reset-jobs)
  (mount/stop))

(defn find-docs
  [index oid]
  (cond
    (= "organisaatio" index) (organisaatio-client/find-docs oid)
    :else [{:type index :oid oid}]))

(defn reindex-all
  []
  (log/info "Tyhjennetään indeksointijono ja uudelleenindeksoidaan kaikki data Tarjonnasta.")
  (elastic-client/delete-index "indexdata")
  (elastic-client/initialize-indices)
  (let [tarjonta-docs (tarjonta-client/find-all-tarjonta-docs)
        organisaatio-docs (organisaatio-client/find-docs nil)
        docs (clojure.set/union tarjonta-docs organisaatio-docs)]
    (elastic-client/upsert-indexdata docs)))

(defn reindex
  [index oid]
  (let [docs (find-docs index oid)
        related-koulutus (flatten (map tarjonta-client/get-related-koulutus docs))
        docs-with-related-koulutus (clojure.set/union docs related-koulutus)]
    (elastic-client/upsert-indexdata docs-with-related-koulutus)))

(defn- empty-queue []
  (let [delete-res (elastic-client/delete-index "indexdata")
        init-res (elastic-client/initialize-indices)]
    { :delete-queue delete-res
      :init-indices init-res }))

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

     (context "/admin" []
       :tags ["admin"]

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
         (ok {:result (elastic-client/get-organisaatio oid)}))

       (GET "/status" []
         :summary "Hakee klusterin ja indeksien tiedot."
         (ok {:result (elastic-client/get-elastic-status)}))

       (GET "/performance_info" []
         :summary "Hakee tietoja performanssista"
         :query-params [{since :- Long 0}]
         (ok {:result (elastic-client/get-elastic-performance-info since)}))

       (GET "/empty_queue" []
         :summary "Tyhjentää indeksoijan jonon. HUOM! ÄLÄ KÄYTÄ, JOS ET TIEDÄ, MITÄ TEET!"
         (ok {:result (empty-queue)}))

       (GET "/s3/koulutus" []
         :summary "Hakee yhden koulutuksen kuvat ja tallentaa ne s3:een"
         :query-params [oid :- String]
         (ok {:result (indexer/store-koulutus-pics {:oid oid :type "koulutus"})}))

       (GET "/s3/organisaatio" []
         :summary "Hakee yhden koulutuksen kuvat ja tallentaa ne s3:een"
         :query-params [oid :- String]
         (ok {:result (indexer/store-organisaatio-pic {:oid oid :type "organisaatio"})})))

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
       (GET "/all" []
         :summary "Indeksoi kaikki koulutukset, hakukohteet, haut ja organisaatiot."
         (ok {:result (reindex-all)}))

       (GET "/koulutus" []
         :summary "Lisää koulutuksen indeksoitavien listalle."
         :query-params [oid :- String]
         (ok {:result (reindex "koulutus" oid)}))

       (GET "/hakukohde" []
         :summary "Lisää hakukohteen indeksoitavien listalle."
         :query-params [oid :- String]
         (ok {:result (reindex "hakukohde" oid)}))

       (GET "/haku" []
         :summary "Lisää haun indeksoitavien listalle."
         :query-params [oid :- String]
         (ok {:result (reindex "haku" oid)}))

       (GET "/organisaatio" []
         :summary "Lisää organisaation indeksoitavien listalle."
         :query-params [oid :- String]
         (ok {:result (reindex "organisaatio" oid)}))))

   (undocumented
    ;; Static resources path. (resources/public, /public path is implicit for route/resources.)
    (route/resources "/konfo-indeksoija/"))))

(def app
  (-> service-api
      ;TODO REMOVE CORS SUPPORT WHEN ui APIs are moved to another project
      (wrap-cors :access-control-allow-origin [#"http://localhost:3005"] :access-control-allow-methods [:get])))