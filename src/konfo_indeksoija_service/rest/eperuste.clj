(ns konfo-indeksoija-service.rest.eperuste
  (:require [konfo-indeksoija-service.util.conf :refer [env]]
            [clj-log.error-log :refer [with-error-logging]]
            [konfo-indeksoija-service.rest.util :as client]
            [clojure.tools.logging :as log]
            [konfo-indeksoija-service.util.time :as time]))

(defn- to-queue-entries [data]
  (map (fn [x] {:oid (str (:id x)) :type "eperuste"}) (:data data)))


(defn- get-perusteet-page [page-nr last-modified]
  (let [url (str (:eperusteet-service-url env))
        base-params {:sivu page-nr :sivukoko 100 :tuleva true :siirtyma true :voimassaolo true :poistunut true}
        params (if (nil? last-modified) base-params (merge base-params {:muokattu last-modified}))]
    (:body (client/get url {:query-params params :as :json}))))

(defn- find
  ([last-modified]
   (with-error-logging
    (loop [page-nr 0 result []]
      (let [data (get-perusteet-page page-nr last-modified)
            total-result (conj result (to-queue-entries data))]
        (if (<= (:sivuja data) (+ 1 page-nr))
          (flatten total-result)
          (recur (+ 1 page-nr) total-result))))))
  ([] (find nil)))

(defn get-doc [entry]
  (with-error-logging
    (let [url (str (:eperusteet-service-url env) (:oid entry) "/kaikki" )]
      (:body (client/get url {:as :json})))))

(defn find-all []
  (let [res (find)]
    (log/info (str "Found total " (count res) " docs from ePerusteet"))
    res))

(defn find-changes [last-modified]
  (let [res (find last-modified)]
    (log/info (str "Found " (count res) " changes since " (time/format-long-with-time (long last-modified)) " from ePerusteet"))
    res))