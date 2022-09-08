(ns kouta-indeksoija-service.queue.localstack
  (:import (cloud.localstack.docker.annotation LocalstackDockerConfiguration)
           (cloud.localstack.docker LocalstackDocker)))

(defn- docker-config []
  (let [builder (LocalstackDockerConfiguration/builder)]
    (.randomizePorts builder true)
    (.pullNewImage builder false)
    (.environmentVariables builder {"SERVICES" "sqs"})
    (.imageTag builder "1.0.4")
    (.build builder)))


(defn start [] (.startup LocalstackDocker/INSTANCE (docker-config)))

(defn stop [] (.stop LocalstackDocker/INSTANCE))

(defn sqs-endpoint [] (.getEndpointSQS LocalstackDocker/INSTANCE))
