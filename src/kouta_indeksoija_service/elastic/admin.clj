(ns kouta-indeksoija-service.elastic.admin
  (:require [kouta-indeksoija-service.elastic.tools :as t]
            [kouta-indeksoija-service.elastic.settings :as settings]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :refer [index-name] :rename {index-name koulutus-search-index}]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :refer [index-name] :rename {index-name oppilaitos-search-index}]
            [kouta-indeksoija-service.indexer.kouta.koulutus :refer [index-name] :rename {index-name koulutus-index}]
            [kouta-indeksoija-service.indexer.kouta.toteutus :refer [index-name] :rename {index-name toteutus-index}]
            [kouta-indeksoija-service.indexer.kouta.haku :refer [index-name] :rename {index-name haku-index}]
            [kouta-indeksoija-service.indexer.kouta.hakukohde :refer [index-name] :rename {index-name hakukohde-index}]
            [kouta-indeksoija-service.indexer.kouta.valintaperuste :refer [index-name] :rename {index-name valintaperuste-index}]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos :refer [index-name] :rename {index-name oppilaitos-index}]
            [kouta-indeksoija-service.indexer.eperuste.eperuste :refer [index-name] :rename {index-name eperuste-index}]
            [kouta-indeksoija-service.indexer.eperuste.osaamisalakuvaus :refer [index-name] :rename {index-name osaamisalakuvaus-index}]
            [kouta-indeksoija-service.queuer.last-queued :refer [index-name] :rename {index-name last-queued-index}]
            [clj-log.error-log :refer [with-error-logging with-error-logging-value]]
            [clj-elasticsearch.elastic-connect :as e]
            [clj-elasticsearch.elastic-utils :as u]
            [kouta-indeksoija-service.rest.util :as http]
            [clojure.tools.logging :as log]
            [cheshire.core :refer [generate-string]]))

(defn get-cluster-health
  []
  (with-error-logging
   (e/get-cluster-health)))

(defn check-elastic-status
  []
  (log/info "Checking elastic status")
  (with-error-logging
   (e/check-elastic-status)))

(defn get-indices-info
  []
  (with-error-logging
   (e/get-indices-info)))

(defn get-elastic-status
  []
  {:cluster_health (:body (get-cluster-health))
   :indices-info (get-indices-info)})

(defn- initialize-index-settings
  []
  (let [all-index-names [eperuste-index
                         osaamisalakuvaus-index
                         "palaute"
                         last-queued-index
                         koulutus-search-index
                         oppilaitos-search-index
                         koulutus-index
                         toteutus-index
                         haku-index
                         hakukohde-index
                         valintaperuste-index
                         oppilaitos-index]
        new-indices (filter #(not (e/index-exists %)) (map t/index-name all-index-names))
        results (map #(e/create-index % settings/index-settings) new-indices)
        ack (map #(:acknowledged %) results)]
    (every? true? ack)))

(defn- update-index-mappings
  [settings index]
  (log/info "Creating mappings for" index index)
  (let [url (str u/elastic-host "/" (t/index-name index) "/_mappings/" (t/index-name index))]
    (with-error-logging
     (-> url
         (http/put {:body (generate-string settings) :as :json :content-type :json})
         :body
         :acknowledged))))

(defn- update-indices-mappings
  [settings indices]
  (let [update-fn (partial update-index-mappings settings)]
    (doall (map update-fn indices))))

(defn- initialize-index-mappings
  []
  (update-indices-mappings settings/stemmer-settings-eperuste     [eperuste-index
                                                                   osaamisalakuvaus-index])
  (update-indices-mappings settings/kouta-settings-search         [koulutus-search-index])
  (update-indices-mappings settings/kouta-settings-search-new     [oppilaitos-search-index])
  (update-indices-mappings settings/kouta-settings                [koulutus-index
                                                                   toteutus-index
                                                                   haku-index
                                                                   hakukohde-index
                                                                   valintaperuste-index
                                                                   oppilaitos-index]))

(defn initialize-indices
  []
  (log/info "Initializing indices")
  (and (initialize-index-settings)
       (initialize-index-mappings)))

(defn reset-index
  [index]
  (log/warn "WARNING! Resetting index " index "! All indexed data is lost!")
  (let [delete-res (t/delete-index index)
        init-res (initialize-indices)]
    { :delete-queue delete-res
      :init-indices init-res }))

(defn search [index query]
  (let [res (e/simple-search index query)]
    (if (= 200 (:status res))
      (:body res)
      res)))
