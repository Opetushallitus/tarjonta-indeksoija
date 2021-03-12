(ns kouta-indeksoija-service.rest.cas.session
  (:require [kouta-indeksoija-service.rest.util :refer [request]]
            [kouta-indeksoija-service.rest.cas.session-id :as cas-session-id]
            [kouta-indeksoija-service.rest.util :refer [handle-error]]
            [clojure.string :refer [upper-case]]
            [clojure.tools.logging :as log]))

(defrecord CasSession [service session-id jsession?])

(defn init-session
  [service-url jsession?]
    (let [path (.getPath (java.net.URI. service-url))]
      (log/info "Init cas session to service " path " @ url " service-url)
      (map->CasSession {:service {:url service-url :path path} :session-id (atom nil) :jsession? jsession?})))

(defn- empty?
  [cas-session]
  (let [session-id (:session-id cas-session)]
    (nil? @session-id)))

(defn- reset
  [cas-session]
  (let [session-id (:session-id cas-session)]
    (reset! session-id (cas-session-id/get-id (:service cas-session) (:jsession? cas-session)))))

(defn- assoc-cas-session-params
  [cas-session opts]
  (let [session-id (:session-id cas-session)]
    (-> opts
        (assoc :follow-redirects false :throw-exceptions false)
        (assoc-in [:cookies (if (:jsession? cas-session) "JSESSIONID" "session")] {:value @session-id :path "/"}))))

(defn cas-authenticated-request
  ([cas-session opts]
   (when (empty? cas-session)
     (reset cas-session))
   (let [http (fn [] (request (assoc-cas-session-params cas-session opts)))
         res (http)]
     (if (<= 300 (:status res))
       (do (reset cas-session)
           (http))
       res)))
  ([cas-client method url opts]
   (cas-authenticated-request cas-client (assoc opts :url url :method method))))

(defn cas-authenticated-request-as-json
  ([cas-client method url opts]
   (let [method-name (upper-case (str method))]
     (log/debug method-name " => " url)
     (let [response (cas-authenticated-request cas-client method url (assoc opts :as :json :throw-exceptions false))]
       (handle-error url method-name response))))
  ([cas-client method url]
   (cas-authenticated-request-as-json cas-client method url {})))