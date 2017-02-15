(defproject tarjonta-indeksoija-service "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [metosin/compojure-api "1.1.10" :exclusions [cheshire
                                                              com.fasterxml.jackson.core/jackson-core
                                                              com.fasterxml.jackson.dataformat/jackson-dataformat-smile
                                                              com.fasterxml.jackson.dataformat/jackson-dataformat-cbor
                                                              ring/ring-codec
                                                              clj-time
                                                              org.yaml/snakeyaml
                                                              joda-time]]
                 [clojurewerkz/elastisch "2.2.2"]
                 [clojurewerkz/quartzite "2.0.0" :exclusions [clj-time]]
                 [clj-http "2.3.0"]
                 [cprop "0.1.10"]
                 [mount "0.1.11"]
                 [environ "1.1.0"]

                 ;;Logging
                 [ring-logger "0.7.6"]
                 [ring-logger-timbre "0.7.5"]
                 [com.taoensso/timbre "4.8.0"]]
  :ring {:handler tarjonta-indeksoija-service.api/app
         :init tarjonta-indeksoija-service.api/init
         :destroy tarjonta-indeksoija-service.api/stop
         :browser-uri "tarjonta-indeksoija"}
  :uberjar-name "tarjonta-indeksoija.jar"
  :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                                  [cheshire "5.5.0"]
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
                   :ring {:reload-paths ["src"]}}
             :test {:env {:test "true"}}
             :uberjar {:ring {:port 8080}}}
  :aliases {"run" ["ring" "server"]
            "test" ["with-profile" "+test" "midje"]
            "ci-test" ["with-profile" "+test" "midje" ":config" "ci/test_conf.clj" ":filter" "-external-api"]
            "autotest" ["with-profile" "+test" "midje" ":autotest"]
            "eastwood" ["with-profile" "+test" "eastwood"]
            "cloverage" ["with-profile" "+test" "cloverage" "--runner" ":midje"]
            "create-uberjar" ["do" "clean" ["ring" "uberjar"]]})
