(ns konfo-indeksoija-service.localstack
  (:import (cloud.localstack Localstack LocalstackWrapper)))

(defn start [] (LocalstackWrapper/start))

(defn stop [] (Localstack/teardownInfrastructure))

(defn sqs-endpoint [] (Localstack/getEndpointSQS))
