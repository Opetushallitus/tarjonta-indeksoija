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
            [kouta-indeksoija-service.indexer.koodisto.koodisto :refer [index-name] :rename {index-name koodisto-index}]
            [kouta-indeksoija-service.queuer.last-queued :refer [index-name] :rename {index-name last-queued-index}]
            [clj-log.error-log :refer [with-error-logging with-error-logging-value]]
            [clj-elasticsearch.elastic-connect :as e]
            [clj-elasticsearch.elastic-utils :as u]
            [kouta-indeksoija-service.rest.util :as http]
            [clojure.tools.logging :as log]
            [cheshire.core :refer [generate-string]]))

(defn get-cluster-health
  []
  (e/get-cluster-health))

(defn check-elastic-status
  []
  (log/info "Checking elastic status")
  (with-error-logging
   (e/check-elastic-status)))

(defn get-indices-info
  []
  (e/get-indices-info))

(defn get-elastic-status
  []
  {:cluster_health (:body (get-cluster-health))
   :indices-info (get-indices-info)})

(defn- healthy?
  [x]
  (or (= "green" x)
      (= "yellow" x)))

(defn healthcheck
  []
  (try
    (let [response       (get-elastic-status)
          cluster-health (let [cluster-status (get-in response [:cluster_health :status])
                               cluster-health (healthy? cluster-status)]
                           [cluster-health {:cluster   (get-in response [:cluster_health :cluster_name])
                                            :status    cluster-status
                                            :healthy   cluster-health}])
          indices-health (for [index-info (:indices-info response)
                               :let [index-status (:health index-info)
                                     index-health (healthy? index-status)]]
                           [index-health {:index   (:index index-info)
                                          :status  index-status
                                          :healthy index-health}])]
      [(and (first cluster-health) (not-any? false? (map first indices-health)))
       {:cluster_health (second cluster-health)
        :indices_health (vec (map second indices-health))}])
    (catch Exception e
      (log/error e)
      [false {:error (.getMessage e)}])))

(defn- get-index-settings
  [index]
  (if (= index eperuste-index)
    settings/index-settings-eperuste
    settings/index-settings))

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
                         oppilaitos-index
                         koodisto-index]
        new-indices (filter #(not (e/index-exists %)) (map t/index-name all-index-names))
        results (map (fn [i] (e/create-index i (get-index-settings i))) new-indices)
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
  (update-indices-mappings settings/kouta-settings-search         [koulutus-search-index
                                                                   oppilaitos-search-index])
  (update-indices-mappings settings/kouta-settings                [koulutus-index
                                                                   toteutus-index
                                                                   haku-index
                                                                   hakukohde-index
                                                                   valintaperuste-index
                                                                   oppilaitos-index])
  (update-indices-mappings settings/settings-koodisto              [koodisto-index]))

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
