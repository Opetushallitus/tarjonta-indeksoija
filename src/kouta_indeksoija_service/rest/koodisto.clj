(ns kouta-indeksoija-service.rest.koodisto
  (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [clj-log.error-log :refer [with-error-logging]]
            [kouta-indeksoija-service.rest.util :refer [get->json-body]]
            [clojure.tools.logging :as log]
            [clojure.core.memoize :as memo]
            [clojure.string :as str]))

(defn extract-versio
  [koodi-uri]
  (if-let [i (str/index-of koodi-uri "#")]
    {:koodi (subs koodi-uri 0 i)
     :versio (subs koodi-uri (+ i 1))}
    {:koodi koodi-uri}))

(defn extract-koodi-nimi
  [koodi]
  (reduce #(assoc %1 (keyword (str/lower-case (:kieli %2))) (:nimi %2)) {} (:metadata koodi)))

(defn- get-koodi-with-url
  [url]
  (log/info url)
  (with-error-logging
   (get->json-body url)))

(defn get-koodi
  [koodisto koodi-uri]
  (when koodi-uri
    (let [with-versio (extract-versio koodi-uri)]
      (if (contains? with-versio :versio)
        (get-koodi-with-url (resolve-url :koodisto-service.koodisto-koodi-versio koodisto (:koodi with-versio) (:versio with-versio)))
        (get-koodi-with-url (resolve-url :koodisto-service.koodisto-koodi koodisto (:koodi with-versio)))))))

(def get-koodi-with-cache
  (memo/ttl get-koodi {} :ttl/threshold 86400000)) ;24 tunnin cache

(defn get-koodi-nimi-with-cache
  ([koodisto koodi-uri]
   {:koodiUri koodi-uri
    :nimi (extract-koodi-nimi (get-koodi-with-cache koodisto koodi-uri))})
  ([koodi-uri]
   (when koodi-uri
     (get-koodi-nimi-with-cache (subs koodi-uri 0 (str/index-of koodi-uri "_")) koodi-uri))))

(defn get-alakoodit
  [koodi-uri]
  (when koodi-uri
    (let [with-versio (extract-versio koodi-uri)]
      (if (contains? with-versio :versio)
        (get-koodi-with-url (resolve-url :koodisto-service.alakoodit-versio (:koodi with-versio) (:versio with-versio)))
        (get-koodi-with-url (resolve-url :koodisto-service.alakoodit (:koodi with-versio)))))))

(def get-alakoodit-with-cache
  (memo/ttl get-alakoodit {} :ttl/threshold 86400000)) ;24 tunnin cache

(defn list-alakoodit-with-cache
  [koodi-uri alakoodi-uri]
  (when (and koodi-uri alakoodi-uri)
    (when-let [alakoodit (seq (get-alakoodit-with-cache koodi-uri))]
      (find #(= (get-in % [:koodisto :koodistoUri]) alakoodi-uri) alakoodit))))

(defn list-alakoodi-nimet-with-cache
  [koodi-uri alakoodi-uri]

  (defn- alakoodi->nimi-json
    [alakoodi]
    {:koodiUri (:koodiUri alakoodi)
     :nimi     (extract-koodi-nimi alakoodi)})

  (vec (map alakoodi->nimi-json (list-alakoodit-with-cache koodi-uri alakoodi-uri))))

(defn get-alakoodi-nimi-with-cache
  [koodi-uri alakoodi-uri]
  (first (list-alakoodi-nimet-with-cache koodi-uri alakoodi-uri)))