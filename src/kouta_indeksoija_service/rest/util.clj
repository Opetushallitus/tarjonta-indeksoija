(ns kouta-indeksoija-service.rest.util
  (:require [clj-http.client :as client]
            [clojure.tools.logging :as log]))

(defn add-callerinfo [options]
  (update-in options [:headers] assoc
             "Caller-Id" "1.2.246.562.10.00000000001.kouta-indeksoija"))

(defn get [url opts]
  (let [options (add-callerinfo opts)]
    ;(log/info "Making get call to " url " with options: " options)
    (client/get url options)))

(defn put [url opts]
  (let [options (add-callerinfo opts)]
    ;(log/info "Making put call to " url " with options: " options)
    (client/put url options)))

(defn post [url opts]
  (let [options (add-callerinfo opts)]
    (client/post url options)))

(defn request [opts]
  (-> opts
      add-callerinfo
      client/request))

(defn get->json-body
  ([url query-params]
   (:body (get url {:as :json :query-params query-params})))
  ([url]
   (:body (get url {:as :json}))))