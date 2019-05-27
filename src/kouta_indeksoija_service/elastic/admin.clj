(ns kouta-indeksoija-service.elastic.admin
  (:require [kouta-indeksoija-service.elastic.tools :as t]
            [kouta-indeksoija-service.elastic.settings :as settings]
            [kouta-indeksoija-service.kouta.koulutus-search :refer [index-name] :rename {index-name koulutus-search-index}]
            [kouta-indeksoija-service.kouta.koulutus :refer [index-name] :rename {index-name koulutus-index}]
            [kouta-indeksoija-service.kouta.toteutus :refer [index-name] :rename {index-name toteutus-index}]
            [kouta-indeksoija-service.kouta.haku :refer [index-name] :rename {index-name haku-index}]
            [kouta-indeksoija-service.kouta.hakukohde :refer [index-name] :rename {index-name hakukohde-index}]
            [kouta-indeksoija-service.kouta.valintaperuste :refer [index-name] :rename {index-name valintaperuste-index}]
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
  (let [all-index-names ["eperuste" "osaamisalakuvaus" "organisaatio" "palaute"
                         "indexdata" "lastindex"
                         koulutus-search-index
                         koulutus-index
                         toteutus-index
                         haku-index
                         hakukohde-index
                         valintaperuste-index]
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
  (update-indices-mappings settings/stemmer-settings-eperuste     ["eperuste" "osaamisalakuvaus"])
  (update-indices-mappings settings/stemmer-settings-organisaatio ["organisaatio"])
  (update-indices-mappings settings/kouta-settings-search         [koulutus-search-index])
  (update-indices-mappings settings/kouta-settings                [koulutus-index
                                                                   toteutus-index
                                                                   haku-index
                                                                   hakukohde-index
                                                                   valintaperuste-index]))

(defn initialize-indices
  []
  (log/info "Initializing indices")
  (and (initialize-index-settings)
       (initialize-index-mappings)
       (update-index-mappings settings/indexdata-mappings "indexdata")))

(defn search [index query]
  (let [res (e/simple-search index query)]
    (if (= 200 (:status res))
      (:body res)
      res)))
