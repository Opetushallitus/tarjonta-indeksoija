(ns kouta-indeksoija-service.rest.organisaatio
  (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [clj-log.error-log :refer [with-error-logging]]
            [kouta-indeksoija-service.rest.util :refer [get->json-body post]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.util.time :refer :all]
            [clojure.core.memoize :as memoize]))

(defn get-doc
  ([oid include-image]
   (with-error-logging
      (-> (resolve-url :organisaatio-service.organisaatio oid)
          (get->json-body {:query-params {:includeImage include-image}}))))
  ([oid]
   (get-doc oid false)))

(defn find-docs
  [oid]
  (with-error-logging
    (if (nil? oid)
      (vec (get->json-body (resolve-url :organisaatio-service.rest.organisaatio)))
      (->> (get->json-body (resolve-url :organisaatio-service.v2.hae)
                           {:aktiiviset true :suunnitellut true :lakkautetut true :oid oid})
           :organisaatiot
           (map #(conj (string/split (:parentOidPath %) #"/") (:oid %)))
           flatten
           distinct
           vec))))

(defn get-all-oids
  []
  (find-docs nil))

(defn get-tyyppi-hierarkia
  [oid]
  (with-error-logging
   (get->json-body (resolve-url :organisaatio-service.v2.hierarkia.tyyppi)
                   {:aktiiviset true :suunnitellut true :lakkautetut true :oid oid})))

(defn find-last-changes
  [last-modified]
  (with-error-logging
    (let [date-string (long->date-time-string last-modified)]
      (let [res (->> (get->json-body (resolve-url :organisaatio-service.v2.muutetut.oid) {:lastModifiedSince date-string})
                     (:oids)
                     (remove clojure.string/blank?)
                     (vec))]
        (when (seq res)
         (log/info "Found " (count res) " changes since " date-string " from organisaatiopalvelu"))
        res))))

(defn find-by-oids
  [oids]
  (if (empty? oids) []
    (with-error-logging
     (log/debug (str "Calling organisaatio service find-by-oids") )
     (let [url (resolve-url :organisaatio-service.v4.find-by-oids)
           body (str "[\"" (string/join "\", \"" oids) "\"]")]
       (:body (post url {:body body :content-type :json :as :json}))))))

(defn get-by-oid
  [oid]
  (with-error-logging
   (log/debug (str "Calling organisaatio service get-by-oid " oid) )
   (get->json-body (resolve-url :organisaatio-service.v4.oid oid))))

(def get-by-oid-cached
  (memoize/ttl get-by-oid {} :ttl/threshold 86400000))
