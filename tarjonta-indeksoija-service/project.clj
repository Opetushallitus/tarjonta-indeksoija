(defproject tarjonta-indeksoija-service "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [metosin/compojure-api "1.1.10"]
                 [clojurewerkz/elastisch "2.2.2"]
                 [clj-http "2.3.0"]
                 [cprop "0.1.10"]
                 [mount "0.1.11"]]
  :ring {:handler tarjonta-indeksoija-service.api/app
         :init tarjonta-indeksoija-service.api/init
         :destroy tarjonta-indeksoija-service.api/stop}
  :uberjar-name "server.jar"
  :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                                  [cheshire "5.5.0"]
                                  [ring/ring-mock "0.3.0"]
                                  [midje "1.8.3"]
                                  [org.clojure/tools.namespace "0.2.11"]]
                   :plugins [[lein-ring "0.10.0"]
                             [lein-midje "3.2"]
                             [jonase/eastwood "0.2.3"]
                             [lein-kibit "0.1.3"]]
                   :resource-paths ["dev_resources"]}}
  :aliases {"run" ["ring" "server"]
            "test" ["midje"]
            "autotest" ["midje" ":autotest"]
            "create-uberjar" ["ring" "uberjar"]})
