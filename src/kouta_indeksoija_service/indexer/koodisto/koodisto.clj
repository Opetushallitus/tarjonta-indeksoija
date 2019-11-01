(ns kouta-indeksoija-service.indexer.koodisto.koodisto
  (:require [kouta-indeksoija-service.rest.koodisto :as koodisto-service]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "koodisto")

(defn create-koodi-entry
  [koodi]
  (-> (select-keys koodi [:koodiUri :versio])
      (assoc :nimi (koodisto-service/extract-koodi-nimi koodi))))

(defn create-index-entry
  [koodisto]
  (let [koodit (koodisto-service/get-koodit koodisto)]
    {:id koodisto
     :koodisto koodisto
     :koodit (vec (map create-koodi-entry koodit))}))

(defn create-index-entries
  [koodistot]
  (doall (pmap create-index-entry koodistot)))

(defn do-index
  [koodistot]
  (indexable/do-index index-name koodistot create-index-entries))

(defn get
  [koodisto]
  (indexable/get index-name koodisto))