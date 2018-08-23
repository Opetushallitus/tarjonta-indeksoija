(defproject konfo-indeksoija-service "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :repositories [["oph-releases" "https://artifactory.oph.ware.fi/artifactory/oph-sade-release-local"]
                 ["oph-snapshots" "https://artifactory.oph.ware.fi/artifactory/oph-sade-snapshot-local"]
                 ["ext-snapshots" "https://artifactory.oph.ware.fi/artifactory/ext-snapshot-local"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [metosin/compojure-api "1.1.10" :exclusions [cheshire
                                                              com.fasterxml.jackson.core/jackson-core
                                                              com.fasterxml.jackson.dataformat/jackson-dataformat-smile
                                                              com.fasterxml.jackson.dataformat/jackson-dataformat-cbor
                                                              ring/ring-codec
                                                              clj-time
                                                              joda-time]]
                 [clojurewerkz/quartzite "2.0.0" :exclusions [clj-time]]
                 [cheshire "5.8.0"]
                 [clj-http "2.3.0"]
                 [cprop "0.1.10"]
                 [mount "0.1.11"]
                 [environ "1.1.0"]
                 [org.clojure/java.jdbc "0.7.0-alpha1"]
                 [org.postgresql/postgresql "9.4-1200-jdbc41"]
                 [base64-clj "0.1.1"]
                 [clj-time "0.14.3"]
                 ;Elasticsearch + s3
                 [oph/clj-elasticsearch "0.1.0-SNAPSHOT"]
                 [oph/clj-s3 "0.1.0-SNAPSHOT"]
                 ;;Logging
                 [oph/clj-log "0.1.0-SNAPSHOT"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.apache.logging.log4j/log4j-api "2.9.0"]
                 [org.apache.logging.log4j/log4j-core "2.9.0"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.9.0"]
                 [clj-log4j2 "0.2.0"]
                 [ring-cors "0.1.11"]]
  :ring {:handler konfo-indeksoija-service.api/app
         :init konfo-indeksoija-service.api/init
         :destroy konfo-indeksoija-service.api/stop
         :browser-uri "konfo-indeksoija"}
  :uberjar-name "konfo-indeksoija.jar"
  :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                                  [ring/ring-mock "0.3.0"]
                                  [oph/clj-test-utils "0.1.0-SNAPSHOT"]
                                  [midje "1.8.3"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [criterium "0.4.4"]]
                   :plugins [[lein-ring "0.10.0"]
                             [lein-midje "3.2"]
                             [jonase/eastwood "0.2.3"]
                             [lein-kibit "0.1.3" :exclusions [org.clojure/clojure]]
                             [lein-environ "1.1.0"]
                             [lein-cloverage "1.0.9" :exclusions [org.clojure/clojure]]]
                   :resource-paths ["dev_resources"]
                   :env {:dev "true"}
                   :ring {:reload-paths ["src"]}}
             :test {:env {:test "true"}}
             :uberjar {:ring {:port 8080}}}
  :aliases {"run" ["ring" "server"]
            "test" ["with-profile" "+test" "midje"]
            "ci-test" ["with-profile" "+test" "midje" ":config" "ci/test_conf.clj"]
            "autotest" ["with-profile" "+test" "midje" ":autotest"]
            "eastwood" ["with-profile" "+test" "eastwood"]
            "cloverage" ["with-profile" "+test" "cloverage" "--runner" ":midje"]
            "create-uberjar" ["do" "clean" ["ring" "uberjar"]]}
  :jvm-opts ["-Dlog4j.configurationFile=dev_resources/log4j2.properties"])
