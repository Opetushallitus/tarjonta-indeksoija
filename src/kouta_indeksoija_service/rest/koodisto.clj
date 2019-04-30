(ns kouta-indeksoija-service.rest.koodisto
  (:require [kouta-indeksoija-service.util.conf :refer [env]]
            [clj-log.error-log :refer [with-error-logging]]
            [kouta-indeksoija-service.rest.util :as client]
            [clojure.tools.logging :as log]
            [clojure.core.memoize :as memo]))

(defn- get-koodi-with-url
  [url]
  (log/info url)
  (with-error-logging
   (let [res (client/get url {:as :json})]
     (:body res))))

(defn- koodi-url
  ([koodisto koodi]
   (str (:koodisto-service-url env) koodisto "/koodi/" koodi))
  ([koodisto koodi versio]
   (str (koodi-url koodisto koodi) "?koodistoVersio=" versio)))

(defn get-koodi
  [koodisto koodi-uri]
  (when koodi-uri
    (if-let [i (clojure.string/index-of koodi-uri "#")]
      (get-koodi-with-url (koodi-url koodisto (subs koodi-uri 0 i) (subs koodi-uri (+ i 1))))
      (get-koodi-with-url (koodi-url koodisto koodi-uri)))))

(def get-koodi-with-cache
  (memo/ttl get-koodi {} :ttl/threshold 86400000)) ;24 tunnin cache

(defn get-koodi-nimi-with-cache
  ([koodisto koodi-uri]
   (defn extract-nimi
     [value]
     (reduce #(assoc %1 (keyword (clojure.string/lower-case (:kieli %2))) (:nimi %2)) {} (:metadata value)))
   (merge {:koodiUri koodi-uri} {:nimi (extract-nimi (get-koodi-with-cache koodisto koodi-uri))}))
  ([koodi-uri]
   (when koodi-uri
     (get-koodi-nimi-with-cache (subs koodi-uri 0 (clojure.string/index-of koodi-uri "_")) koodi-uri))))
