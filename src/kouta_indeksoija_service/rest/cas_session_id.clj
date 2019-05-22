(ns kouta-indeksoija-service.rest.cas-session-id
  (:require [kouta-indeksoija-service.util.conf :refer [env]]
            [kouta-indeksoija-service.rest.util :refer [request]]
            [jsoup.soup :refer :all]))

(defn- send-form
  [url form]
  (request {:form-params      form
            :method           :post
            :url              url
            :throw-exceptions false
            :content-type     "application/x-www-form-urlencoded"}))

(defn- parse-ticket-granting-ticket
  [response]
  (let [parsed-body (-> response :body parse)]
    (first (attr "action" ($ parsed-body "form")))))

(defn- get-ticket-granting-ticket
  []
  (let [username (-> env :cas :username)
        password (-> env :cas :password)
        tgt-url  (format "%s/cas/v1/tickets" (-> env :cas :host))
        response (send-form tgt-url {:username username :password password})
        tgt (parse-ticket-granting-ticket response)]
    (if (clojure.string/blank? tgt)
      (RuntimeException. (format "Unable to read tgt on CAS response: %s" response))
      tgt)))

(defn- get-service-ticket
  [service tgt]
  (let [service-url (format "%s%s" (-> env :cas :host) service)]
    (let [response (send-form tgt {:service service-url})]
      (if-let [st (:body response)]
        st
        (RuntimeException. (format "Unable to parse service ticket for service %s on responce: %s!" service response))))))

(defn- parse-jsession-id
  [response]
  (when-let [cookie (-> response :headers :set-cookie)]
    (nth (re-find #"JSESSIONID=(\w*);" cookie) 1)))

(defn- get-jsession-id
  [service st]
  (let [url (str (-> env :cas :host) service)
        response (request {:headers {"CasSecurityTicket" st} :url url :method :get :throw-exceptions false})]
    (if-let [jsession-id (parse-jsession-id response)]
      jsession-id
      (RuntimeException. (format "Unable to parse session ID from %s on response: %s" url response)))))

(defn- get-session-id
  [service st]
  (let [url (str (-> env :cas :host) service "?ticket=" st)
        response (request {:url url :method :get :throw-exceptions false :follow-redirects false})]
    (if-let [session-id (-> response :cookies (get "session") :value)]
      session-id
      (RuntimeException. (format "Unable to parse session ID! Uri = %s got status code %s" url (:status response))))))

(defn get
  [service jsession?]
  (if jsession?
    (->> (get-ticket-granting-ticket)
         (get-service-ticket (str service "/j_spring_cas_security_check"))
         (get-jsession-id service))
    (->> (get-ticket-granting-ticket)
         (get-service-ticket service)
         (get-session-id service))))