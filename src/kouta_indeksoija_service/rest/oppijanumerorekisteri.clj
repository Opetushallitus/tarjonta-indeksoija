(ns kouta-indeksoija-service.rest.oppijanumerorekisteri
  (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [kouta-indeksoija-service.rest.cas.session :refer [init-session cas-authenticated-request-as-json]]
            [kouta-indeksoija-service.rest.util :refer [handle-error]]
            [clojure.tools.logging :as log]
            [clojure.core.memoize :as memo]))

(defonce cas-session (init-session (resolve-url :oppijanumerorekisteri-service.internal.base) true))

(defonce cas-authenticated-get-as-json (partial cas-authenticated-request-as-json cas-session :get))

(defonce henkilo_cache_time_millis (* 1000 60 60 2)) ;2 tunnin cache

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