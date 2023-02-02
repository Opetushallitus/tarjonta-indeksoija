(ns kouta-indeksoija-service.rest.organisaatio
  (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [kouta-indeksoija-service.rest.util :refer [get->json-body]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.util.time :refer :all]
            [clojure.core.memoize :as memoize]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as o]
            [clojure.string :as s]))


(comment

 (defn clear-get-all-organisaatiot-cache
   []
   (memoize/memo-clear! get-all-organisaatiot))

 (def get-all-organisaatiot-with-cache
   (memoize/ttl get-all-organisaatiot :ttl/threshold (* 1000 60 30))) ;;30 minuutin cache

 (defn get-hierarkia-for-oid-from-cache
   ;; With parents
   [oid]
   (some-> (get-all-organisaatiot-with-cache) (o/find-hierarkia oid))))

(defn get-all-oppilaitos-oids
  []
  (->> (get->json-body (resolve-url :organisaatio-service.api.hae.tyyppi)
                       {:aktiiviset true :suunnitellut false :lakkautetut false :organisaatiotyyppi "organisaatiotyyppi_02"})
       :items
       (map #(:oid %))
       vec))

(defonce oph-oid "1.2.246.562.10.00000000001")

(defn get-all-organisaatiot
  []
  (let [url (resolve-url :organisaatio-service.api.oid.jalkelaiset oph-oid)]
    (log/info "Fetching all organisaatiot from organisaatio service " url)
    (get->json-body url)))

(defn find-last-changes
  [last-modified]
  (let [date-string (long->date-time-string last-modified)]
    (let [res (->> (get->json-body (resolve-url :organisaatio-service.api.muutetut) {:lastModifiedSince date-string})
                   (filter #(not (s/starts-with? (:oid %) "1.2.246.562.28"))))]
      (when (seq res)
        (log/info "Found " (count res) " changes since " date-string " from organisaatiopalvelu"))
      res)))

(defn get-by-oid
  [oid]
  (get->json-body (resolve-url :organisaatio-service.api.oid oid)))
