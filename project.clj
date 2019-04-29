(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject konfo-indeksoija-service "0.1.3-SNAPSHOT"
  :description "FIXME: write description"
  :repositories [["releases" {:url "https://artifactory.opintopolku.fi/artifactory/oph-sade-release-local"
                              :username :env/artifactory_username
                              :password :env/artifactory_password
                              :sign-releases false
                              :snapshots false}]
                 ["snapshots" {:url "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"
                               :username :env/artifactory_username
                               :password :env/artifactory_password
                               :snapshots true}]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [metosin/compojure-api "1.1.10" :exclusions [cheshire
                                                              com.fasterxml.jackson.core/jackson-core
                                                              com.fasterxml.jackson.dataformat/jackson-dataformat-smile
                                                              com.fasterxml.jackson.dataformat/jackson-dataformat-cbor
                                                              ring/ring-codec
                                                              clj-time
                                                              joda-time
                                                              org.clojure/core.cache
                                                              org.clojure/core.memoize]]
                 [clojurewerkz/quartzite "2.0.0" :exclusions [clj-time]]
                 [cheshire "5.8.0"]
                 [clj-http "2.3.0"]
                 [cprop "0.1.10"]
                 [mount "0.1.11"]
                 [environ "1.1.0"]
                 [org.clojure/java.jdbc "0.7.0-alpha1"]
                 [org.clojure/core.memoize "0.7.1"]
                 [org.postgresql/postgresql "9.4-1200-jdbc41"]
                 [base64-clj "0.1.1"]
                 [clj-time "0.14.3"]
                 [org.clojure/algo.generic "0.1.3"]
                 ;Elasticsearch + s3
                 [oph/clj-elasticsearch "0.2.0-SNAPSHOT"]
                 [oph/clj-s3 "0.2.2-SNAPSHOT"]
                 ;;Logging
                 [oph/clj-log "0.2.0-SNAPSHOT"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.apache.logging.log4j/log4j-api "2.9.0"]
                 [org.apache.logging.log4j/log4j-core "2.9.0"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.9.0"]
                 [clj-log4j2 "0.2.0"]
                 [ring-cors "0.1.11"]
                 ;;SQS Handling
                 [amazonica "0.3.48" :exclusions [com.amazonaws/aws-java-sdk
                                                  com.amazonaws/amazon-kinesis-client]]
                 [com.amazonaws/aws-java-sdk-core "1.11.479"]
                 [com.amazonaws/aws-java-sdk-sqs "1.11.479"]]
  :ring {:handler konfo-indeksoija-service.api/app
         :init konfo-indeksoija-service.api/init
         :destroy konfo-indeksoija-service.api/stop
         :browser-uri "konfo-indeksoija"}
  :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                                  [ring/ring-mock "0.3.0"]
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
                   :ring {:reload-paths ["src"]}
                   :jvm-opts ["-Daws.accessKeyId=randomKeyIdForLocalstack"
                              "-Daws.secretKey=randomKeyForLocalstack"]}
             :test {:env {:test "true"} :dependencies [[cloud.localstack/localstack-utils "0.1.15"]
                                                       [fi.oph.kouta/kouta-backend "0.1-SNAPSHOT"]
                                                       [fi.oph.kouta/kouta-backend "0.1-SNAPSHOT" :classifier "tests"]
                                                       [oph/clj-test-utils "0.2.0-SNAPSHOT"]]
                    :resource-paths ["test_resources"]
                    :jvm-opts ["-Daws.accessKeyId=randomKeyIdForLocalstack"
                               "-Daws.secretKey=randomKeyForLocalstack"]}
             :ci-test {:env {:test "true"}
                       :dependencies [[ring/ring-mock "0.3.2"]
                                      [cloud.localstack/localstack-utils "0.1.15"]
                                      [fi.oph.kouta/kouta-backend "0.1-SNAPSHOT"]
                                      [fi.oph.kouta/kouta-backend "0.1-SNAPSHOT" :classifier "tests"]
                                      [oph/clj-test-utils "0.2.0-SNAPSHOT"]]
                       :jvm-opts ["-Dlog4j.configurationFile=dev_resources/log4j2.properties"
                                  "-Dconf=ci/config.edn"
                                  "-Daws.accessKeyId=randomKeyIdForLocalstack"
                                  "-Daws.secretKey=randomKeyForLocalstack"]}
             :uberjar {:ring {:port 8080}}
             :jar-with-test-fixture {:source-paths ["src", "test"]
                                     :jar-exclusions [#"perf|resources|mocks"
                                                      #"konfo_indeksoija_service/\w*_test.clj"
                                                      #"konfo_indeksoija_service/converter/\w*_test.clj"]}} ;TODO: Better regexp
  :aliases {"run" ["ring" "server"]
            "test" ["with-profile" "+test" "midje"]
            "deploy" ["with-profile" "+jar-with-test-fixture" "deploy"]
            "install" ["with-profile" "+jar-with-test-fixture" "install"]
            "ci-test" ["with-profile" "+ci-test" "midje"]
            "autotest" ["with-profile" "+test" "midje" ":autotest"]
            "eastwood" ["with-profile" "+test" "eastwood"]
            "cloverage" ["with-profile" "+test" "cloverage" "--runner" ":midje"]
            "uberjar" ["do" "clean" ["ring" "uberjar"]]
            "testjar" ["with-profile" "+jar-with-test-fixture" "jar"]}
  :jvm-opts ["-Dlog4j.configurationFile=dev_resources/log4j2.properties"])
