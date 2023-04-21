(ns mocks.export-elastic-data
  (:require [clj-elasticsearch.elastic-utils :as e-utils]
            [clj-test-utils.generic :refer [run-proc]]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn export-elastic-data [service-name]
  (println "Starting elasticdump...")
  (.mkdirs (io/file (str "elasticdump/" service-name)))
  (let [e-host (string/replace e-utils/elastic-host #"127\.0\.0\.1|localhost" "host.docker.internal")
        pwd (System/getProperty "user.dir")]
    (run-proc "./tools/dump_elastic_data.sh" (str pwd "/elasticdump/" service-name) e-host)))
