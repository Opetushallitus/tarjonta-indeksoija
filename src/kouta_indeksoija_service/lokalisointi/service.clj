(ns kouta-indeksoija-service.lokalisointi.service
  (:require [kouta-indeksoija-service.rest.lokalisointi :as lokalisointi-service]
            [kouta-indeksoija-service.lokalisointi.util :as util]))

(defn ->translation-keys
  [json]
  (util/nested-json->key-value-pairs json))

(defn ->json
  [translation-keys]
  (util/key-value-pairs->nested-json translation-keys))

(defn save-translation-keys-to-localisation-service
  [category lng key-value-pairs]
  (doseq [[k v] key-value-pairs]
    ; Lokalisointi-palvelu palauttaa 500, kun yritetään päivittää jo olemassaolevaa lokalisointia ilman force-optiota
    (try (lokalisointi-service/post category lng (name k) v) (catch Exception _))))

(defn save-translation-json-to-localisation-service
  [category lng json]
  (->> json
       (->translation-keys)
       (save-translation-keys-to-localisation-service category lng)))