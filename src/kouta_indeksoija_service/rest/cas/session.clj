(ns kouta-indeksoija-service.rest.cas.session
  (:require [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.rest.util :refer [request]]
            [kouta-indeksoija-service.rest.cas.session-id :as cas-session-id]))

(defrecord CasSession [service session-id jsession?])

(defn init-session
  [service-url jsession?]
   (map->CasSession {:service service-url :session-id (atom nil) :jsession? jsession?}))

(defn- empty?
  [cas-session]
  (let [session-id (:session-id cas-session)]
    (nil? @session-id)))

(defn- reset
  [cas-session]
  (let [session-id (:session-id cas-session)]
    (reset! session-id (cas-session-id/get (:service cas-session) (:jsession? cas-session)))))

(defn- assoc-cas-session-params
  [cas-session opts]
  (let [session-id (:session-id cas-session)]
    (-> opts
        (assoc :follow-redirects false :throw-exceptions false)
        (update-in [:headers] assoc "Cookie" (if (:jsession cas-session) (str "JSESSIONID=" @session-id) (str "session=" @session-id))))))

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