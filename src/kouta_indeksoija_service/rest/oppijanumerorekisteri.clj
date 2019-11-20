(ns kouta-indeksoija-service.rest.oppijanumerorekisteri
  (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [kouta-indeksoija-service.rest.cas.session :refer [init-session cas-authenticated-request]]
            [clojure.tools.logging :as log]
            [clojure.core.memoize :as memo]))

(defonce cas-session (init-session (resolve-url :oppijanumerorekisteri-service.internal.base) true))

(defonce cas-authenticated-get (partial cas-authenticated-request cas-session :get))

(defonce henkilo_cache_time_millis (* 1000 60 60 2)) ;2 tunnin cache

(defn- cas-authenticated-get-as-json
  ([url opts]
    (log/debug (str "GET => " url))
    (let [response (cas-authenticated-get url (assoc opts :as :json :throw-exceptions false))
          status   (:status response)
          body     (:body response)]
      (cond
        (= 200 status) body
        (= 404 status) (do (log/warn  "Got " status " from GET: " url " with body " body) nil)
        :else          (do (log/error "Got " status " from GET: " url " with response " response) nil))))
 ([url]
  (cas-authenticated-get-as-json url {})))

(defn get-henkilo
  [oid]
  (-> (resolve-url :oppijanumerorekisteri-service.henkilo.oid oid)
      (cas-authenticated-get-as-json)))

(def get-henkilo-with-cache
  (memo/ttl get-henkilo {} :ttl/threshold henkilo_cache_time_millis))

(defn- parse-henkilo-nimi
  [henkilo]
  (str (or (:kutsumanimi henkilo) (:etunimet henkilo)) " " (:sukunimi henkilo)))

(defn get-henkilo-nimi-with-cache
  [oid]
  (when-let [henkilo (get-henkilo-with-cache oid)]
    (parse-henkilo-nimi henkilo)))