(ns kouta-indeksoija-service.rest.organisaatio
  (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [kouta-indeksoija-service.rest.util :as http]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.util.time :refer :all]
            [clojure.core.memoize :as memoize]))

(defn- get-json-and-handle-errors
  ([url query-params]
   (log/debug "GET => " url)
   (let [response (http/get url (assoc {} :throw-exceptions false :as :json :query-params query-params))
         status   (:status response)
         body     (:body response)]
     (cond
       (= 200 status) body
       (= 404 status) (do (log/warn  "Got " status " from GET: " url " with body " body) nil)
       :else          (do (log/error "Got " status " from GET: " url " with body " body) nil))))
  ([url]
   (get-json-and-handle-errors url {})))

(defn get-doc
  ([oid include-image]
   (-> (resolve-url :organisaatio-service.organisaatio oid)
       (get-json-and-handle-errors {:includeImage include-image})))
  ([oid]
   (get-doc oid false)))

(defn find-docs
  [oid]
  (->> (get-json-and-handle-errors (resolve-url :organisaatio-service.v2.hae)
                                   {:aktiiviset true :suunnitellut false :lakkautetut false :oid oid})
       :organisaatiot
       (map #(conj (string/split (:parentOidPath %) #"/") (:oid %)))
       flatten
       distinct
       vec))

(defn get-all-oppilaitos-oids
  []
  (->> (get-json-and-handle-errors (resolve-url :organisaatio-service.v2.hae.tyyppi)
                                   {:aktiiviset true :suunnitellut false :lakkautetut false :organsaatiotyyppi "Oppilaitos"})
       :organisaatiot
       (map #(:oid %))
       vec))

(defn get-tyyppi-hierarkia
  [oid]
  (get-json-and-handle-errors (resolve-url :organisaatio-service.v2.hierarkia.tyyppi)
                              {:aktiiviset true :suunnitellut true :lakkautetut true :oid oid}))

(defn get-hierarkia-v4
  [oid & {:as params}]
  (get-json-and-handle-errors (resolve-url :organisaatio-service.v4.hierarkia.hae)
                              (merge {:aktiiviset true :suunnitellut true :lakkautetut false :oid oid :skipParents false} params)))

(defn find-last-changes
  [last-modified]
  (let [date-string (long->date-time-string last-modified)]
    (let [res (->> (get-json-and-handle-errors (resolve-url :organisaatio-service.v2.muutetut.oid) {:lastModifiedSince date-string})
                   (:oids)
                   (remove clojure.string/blank?)
                   (vec))]
      (when (seq res)
        (log/info "Found " (count res) " changes since " date-string " from organisaatiopalvelu"))
      res)))

(defn find-by-oids
  [oids]
  (if (empty? oids)
    []
    (let [url (resolve-url :organisaatio-service.v4.find-by-oids)
          body (str "[\"" (string/join "\", \"" oids) "\"]")]
      (log/debug "POST => " url)
      (let [response (http/post url {:body body :content-type :json :as :json :throw-exceptions false})
            status   (:status response)
            body     (:body response)]
        (cond
          (= 200 status) body
          (= 404 status) (do (log/warn  "Got " status " from POST: " url " with body " body) nil)
          :else          (do (log/error "Got " status " from POST: " url " with body " body) nil))))))

(defn get-by-oid
  [oid]
  (get-json-and-handle-errors (resolve-url :organisaatio-service.v4.oid oid)))

(def get-by-oid-cached
  (memoize/ttl get-by-oid {} :ttl/threshold 86400000))
