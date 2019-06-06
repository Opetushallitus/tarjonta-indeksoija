(ns kouta-indeksoija-service.rest.eperuste
  (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [clj-log.error-log :refer [with-error-logging]]
            [kouta-indeksoija-service.rest.util :refer [get->json-body]]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.util.time :as time]))

(defn- get-perusteet-page [page-nr last-modified]
  (let [params (cond-> {:sivu page-nr :sivukoko 100 :tuleva true :siirtyma true :voimassaolo true :poistunut true}
                       (not (nil? last-modified)) (assoc :muokattu last-modified))]
    (get->json-body (resolve-url :eperusteet-service.perusteet) params)))

(defn- to-queue-entries [data]
  (map (fn [x] {:oid (str (:id x)) :type "eperuste"}) (:data data)))

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
   (get->json-body
    (resolve-url :eperusteet-service.peruste.kaikki (:oid entry)))))

(defn get-osaamisalakuvaukset [eperuste-id]
  (with-error-logging
   (let [res (get->json-body (resolve-url :eperusteet-service.peruste.osaamisalakuvaukset eperuste-id))
         suoritustavat (keys res)
         osaamisalat (fn [suoritustapa] (apply concat (-> res suoritustapa vals)))
         assoc-values (fn [suoritustapa osaamisala] (assoc osaamisala :suoritustapa suoritustapa
                                                                      :type "osaamisalakuvaus"
                                                                      :oid (:id osaamisala)
                                                                      :eperuste-oid eperuste-id))
         docs (vec (flatten (map (fn [st] (map (partial assoc-values st) (osaamisalat st))) suoritustavat)))]
     (if (empty? docs)
       nil
       {:docs docs}))))

(defn find-all []
  (let [res (find)]
    (log/info (str "Found total " (count res) " docs from ePerusteet"))
    res))

(defn find-changes [last-modified]
  (let [res (find last-modified)]
    (when (< 0 (count res))
      (log/info (str "Found " (count res) " changes since " (time/long->date-time-string (long last-modified)) " from ePerusteet")))
    res))