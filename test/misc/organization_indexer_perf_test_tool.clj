(ns misc.organization-indexer-perf-test-tool
  (:gen-class)
  (:require [kouta-indeksoija-service.indexer.indexer :as indexer]
            [kouta-indeksoija-service.elastic.admin :as a]
            [kouta-indeksoija-service.indexer.indexable :refer [->index-entry]]
            [kouta-indeksoija-service.test-tools :refer [debug-pretty]]
            [clj-test-utils.elasticsearch-mock-utils :as mock]))

(defonce port 9237)
(defonce test-index-name "analyze-settings-test-index")
(defonce elastic-url (str "http://localhost:" port))
(defonce config {:hosts {:kouta-backend "https://virkailija.untuvaopintopolku.fi"
                         :virkailija-internal "https://virkailija.untuvaopintopolku.fi"
                         :cas "https://virkailija.untuvaopintopolku.fi"}})

(defn -main
  []
  (intern 'clj-elasticsearch.elastic-utils 'elastic-host elastic-url)
  (try
    (mock/start-embedded-elasticsearch port)
    (a/initialize-indices)
    ;(indexer/index-oppilaitos "1.2.246.562.10.81934895871")
    ;(indexer/index-oppilaitokset (take 200 (organisaatio/get-all-oppilaitos-oids)))
    (indexer/index-all-oppilaitokset)
    ;(debug-pretty (oppilaitos/get "1.2.246.562.10.81934895871"))
    (debug-pretty (a/get-indices-info))
    (finally (println "Finally") (mock/stop-elastic-test))))