(ns kouta-indeksoija-service.lokalisointi.service
  (:require [kouta-indeksoija-service.rest.lokalisointi :as lokalisointi-service]
            [kouta-indeksoija-service.lokalisointi.util :as util]
            [clojure.string :refer [split]]))

(defn ->translation-keys
  [json]
  (util/nested-json->key-value-pairs json))

(defn ->json
  [translation-keys]
  (util/key-value-pairs->nested-json translation-keys))

(defn save-translation-keys-to-localisation-service
  [lng key-value-pairs]
  (doseq [[k v] key-value-pairs]
    (lokalisointi-service/post lng (name k) v)))

(defn save-translation-json-to-localisation-service
  [lng json]
  (->> json
       (->translation-keys)
       (save-translation-keys-to-localisation-service lng)))