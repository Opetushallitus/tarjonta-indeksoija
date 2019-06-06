(ns kouta-indeksoija-service.rest.koodisto
  (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [clj-log.error-log :refer [with-error-logging]]
            [kouta-indeksoija-service.rest.util :refer [get->json-body]]
            [clojure.tools.logging :as log]
            [clojure.core.memoize :as memo]
            [clojure.string :as str]))

(defn- get-koodi-with-url
  [url]
  (log/info url)
  (with-error-logging
   (get->json-body url)))

(defn get-koodi
  [koodisto koodi-uri]
  (when koodi-uri
    (if-let [i (str/index-of koodi-uri "#")]
      (get-koodi-with-url (resolve-url :koodisto-service.koodisto-koodi-versio koodisto (subs koodi-uri 0 i) (subs koodi-uri (+ i 1))))
      (get-koodi-with-url (resolve-url :koodisto-service.koodisto-koodi koodisto koodi-uri)))))

(def get-koodi-with-cache
  (memo/ttl get-koodi {} :ttl/threshold 86400000)) ;24 tunnin cache

(defn get-koodi-nimi-with-cache
  ([koodisto koodi-uri]
    (let [extract-nimi (fn [value] (reduce #(assoc %1 (keyword (str/lower-case (:kieli %2))) (:nimi %2)) {} (:metadata value)))]
      (merge {:koodiUri koodi-uri} {:nimi (extract-nimi (get-koodi-with-cache koodisto koodi-uri))})))
  ([koodi-uri]
   (when koodi-uri
     (get-koodi-nimi-with-cache (subs koodi-uri 0 (str/index-of koodi-uri "_")) koodi-uri))))