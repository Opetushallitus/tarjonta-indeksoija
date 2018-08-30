(ns konfo-indeksoija-service.elastic.admin
  (:require [konfo-indeksoija-service.elastic.tools :as t]
            [konfo-indeksoija-service.elastic.settings :as settings]
            [clj-log.error-log :refer [with-error-logging with-error-logging-value]]
            [clj-elasticsearch.elastic-connect :as e]
            [clj-elasticsearch.elastic-utils :as u]
            [konfo-indeksoija-service.rest.util :as http]
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

(defn initialize-index-settings []
  (let [index-names ["hakukohde" "koulutus" "organisaatio" "haku" "indexdata"
                     "lastindex" "indexing_perf" "query_perf" "palaute" "koulutusmoduuli", "eperuste"]
        new-indexes (filter #(not (e/index-exists %)) (map t/index-name index-names))
        results (map #(e/create-index % settings/index-settings) new-indexes)
        ack (map #(:acknowledged %) results)]
    (every? true? ack)))

(defn- update-index-mappings
  [index type settings]
  (log/info "Creating mappings for" index type)
  (let [url (str u/elastic-host "/" (t/index-name index) "/_mappings/" (t/index-name type))]
    (with-error-logging
     (-> url
         (http/put {:body (generate-string settings) :as :json :content-type :json})
         :body
         :acknowledged))))

(defn initialize-index-mappings []
  (let [index-names ["hakukohde" "koulutus" "haku" "koulutusmoduuli", "eperuste"]]
    (update-index-mappings "organisaatio" "organisaatio" settings/stemmer-settings-organisaatio)
    (every? true? (doall (map #(update-index-mappings % % settings/stemmer-settings) index-names)))))

(defn initialize-indices []
  (log/info "Initializing indices")
  (and (initialize-index-settings)
       (initialize-index-mappings)
    (update-index-mappings "indexdata" "indexdata" settings/indexdata-mappings)))