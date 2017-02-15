(ns tarjonta-indeksoija-service.api
  (:require [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [tarjonta-indeksoija-service.conf :refer [env]]
            [tarjonta-indeksoija-service.util.logging :as logging]
            [tarjonta-indeksoija-service.indexer :as indexer]
            [tarjonta-indeksoija-service.tarjonta-client :as tarjonta-client]
            [tarjonta-indeksoija-service.organisaatio-client :as organisaatio-client]
            [tarjonta-indeksoija-service.util.tools :refer [with-error-logging]]
            [compojure.api.sweet :refer :all]
            [compojure.route :as route]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [mount.core :as mount]
            [taoensso.timbre :as timbre]
            [ring.logger.timbre :as logger.timbre]
            [compojure.api.exception :as ex]
            [environ.core]))

(defn init []
  (mount/start)
  (timbre/set-config! (logging/logging-config))
  (if (and (elastic-client/check-elastic-status)
           (elastic-client/initialize-indices))
    (indexer/start-indexer-job)
    (do
      (timbre/error "Application startup canceled due to Elastic client error or absence.")
      (System/exit 0))))

(defn stop []
  (indexer/reset-jobs)
  (mount/stop))

(defn find-docs
  [index oid]
  (cond
    (= "organisaatio" index) (organisaatio-client/find-docs oid)
    (nil? oid) (tarjonta-client/find-docs index)
    :else [{:type index :oid oid}]))

(defn reindex
  [index oid]
  (let [docs (find-docs index oid)
        related-koulutus (flatten (map tarjonta-client/get-related-koulutus docs))
        docs-with-related-koulutus (clojure.set/union docs related-koulutus)]
    (elastic-client/upsert-indexdata docs-with-related-koulutus)))

(defn get-koulutus-tulos
  [koulutus-oid]
  (with-error-logging
    (let [koulutus (#(assoc {} (:oid %) %) (elastic-client/get-koulutus koulutus-oid))
          hakukohteet-list (elastic-client/get-hakukohteet-by-koulutus koulutus-oid)
          hakukohteet (reduce-kv (fn [m k v] (assoc m (:oid v) v)) {} (vec hakukohteet-list))
          haut-list (elastic-client/get-haut-by-oids (map :hakuOid (vals hakukohteet)))
          haut (reduce-kv (fn [m k v] (assoc m (:oid v) v)) {} (vec haut-list))
          organisaatiot-list (#(assoc {} (:oid %) %)  (elastic-client/get-organisaatios-by-oids [(get-in koulutus [:organisaatio :oid])]))
          organisaatiot (reduce-kv (fn [m k v] (assoc m (:oid v) v)) {} (vec organisaatiot-list))]
      {:koulutus koulutus
       :haut haut
       :hakukohteet hakukohteet
       :organisaatiot organisaatiot})))

(def service-api
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
        (GET "/koulutus" []
          :summary "Lisää koulutuksen indeksoitavien listalle."
          :query-params [oid :- String]
          (ok {:result (reindex "koulutus" oid)}))

        (GET "/koulutus/all" []
          :summary "Lisää kaikki koulutukset indeksoitavien listalle."
          (ok {:result (reindex "koulutus" nil)}))

        (GET "/hakukohde" []
          :summary "Lisää hakukohteen indeksoitavien listalle."
          :query-params [oid :- String]
          (ok {:result (reindex "hakukohde" oid)}))

        (GET "/hakukohde/all" []
          :summary "Lisää kaikki hakukohteet indeksoitavien listalle."
          (ok {:result (reindex "hakukohde" nil)}))

        (GET "/haku" []
          :summary "Lisää haun indeksoitavien listalle."
          :query-params [oid :- String]
          (ok {:result (reindex "haku" oid)}))

        (GET "/haku/all" []
          :summary "Lisää kaikki haut indeksoitavien listalle."
          (ok {:result (reindex "haku" nil)}))

        (GET "/organisaatio" []
          :summary "Lisää organisaation indeksoitavien listalle."
          :query-params [oid :- String]
          (ok {:result (reindex "organisaatio" oid)}))

        (GET "/organisaatio/all" []
          :summary "Lisää kaikki organisaatiot indeksoitavien listalle."
          (ok {:result (reindex "organisaatio" nil)})))

      (context "/ui" []
        :tags ["ui"]
        (GET "/koulutus/:oid" []
          :summary "Koostaa koulutuksen sekä siihen liittyien hakukohteiden ja hakujen tiedot."
          :path-params [oid :- String]
          (ok {:result (get-koulutus-tulos oid)}))

        (GET "/search" []
          :summary "Tekstihaku."
          :query-params [query :- String]
          (ok {:result (elastic-client/text-search query)}))))

    (undocumented
      ;; Static resources path. (resources/public, /public path is implicit for route/resources.)
      (route/resources "/tarjonta-indeksoija/"))))

(def app
  (-> service-api
      logger.timbre/wrap-with-logger))