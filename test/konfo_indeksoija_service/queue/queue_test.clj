(ns konfo-indeksoija-service.queue.queue-test
  (:require [clojure.string :as str]
            [midje.sweet :refer :all]
            [cheshire.core :as json]
            [konfo-indeksoija-service.queue.queue :refer :all]))

(defn- mock-receive [queue] (seq (reverse queue)))

(defn- mock-receive2 [queue] (seq (str/upper-case queue)))

(defn- sqs-message
  [body]
  (let [msg (new com.amazonaws.services.sqs.model.Message)]
    (.setBody msg body)
    msg))

(fact "'receive' response should have :queue and :messages keys"
      (set (keys (receive {:q "queue" :f mock-receive}))) => (set [:messages :queue ]))

(fact "'receive' should call parameter ':f' with value ':q' and return response in ':messages'"
      (:messages (receive {:q "queue" :f mock-receive})) => (mock-receive "queue")
      (:messages (receive {:q "foobar" :f mock-receive2})) => (mock-receive2 "foobar"))

(fact "'receive' should return input ':q' parameter as ':queue'"
      (:queue (receive {:q "queue" :f mock-receive})) => "queue"
      (:queue (receive {:q "foobar" :f mock-receive})) => "foobar")

(fact "'body-json->map' should return json string from message body as map"
      (let [content {:oid "123.123.123"
                     :foobar "bar"}
            msg (sqs-message (json/generate-string content))]
        (body-json->map msg) => content))

(fact "'body-json->map' should fail on non-json body"
      (let [msg (sqs-message "non-json-stuff")]
        (body-json->map msg) => (throws com.fasterxml.jackson.core.JsonParseException)))

