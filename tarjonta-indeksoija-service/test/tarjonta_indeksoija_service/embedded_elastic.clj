(ns tarjonta-indeksoija-service.embedded-elastic
  (:import [pl.allegro.tech.embeddedelasticsearch EmbeddedElastic]))

(defn get-embedded-elastic
  []
  (let [server (-> (EmbeddedElastic/builder)
                   (.withElasticVersion "5.0.0")
                   (.build)
                   (.start))]
    (println (str "Started embedded elasticsearch instance in port: "
                  (.getHttpPort server)))
    server))

(defn stop-embedded-elastic
  [server]
  (println (str "Stopping embedded elasticsearch instance in port: "
                (.getHttpPort server)))
  (.stop server))
