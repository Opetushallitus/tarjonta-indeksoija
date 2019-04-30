(ns kouta-indeksoija-service.rest.util
  (:require [clj-http.client :as client]
            [clojure.tools.logging :as log]))

;Adds Caller-Id and clientSubSystemCode to header
(defn add-callerinfo [options]
  (update-in options [:headers] assoc
             "Caller-Id" "fi.opintopolku.kouta-indeksoija"
             "clientSubSystemCode" "fi.opintopolku.kouta-indeksoija")
  )

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
