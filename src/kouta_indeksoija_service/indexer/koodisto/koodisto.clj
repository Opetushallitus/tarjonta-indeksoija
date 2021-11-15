(ns kouta-indeksoija-service.indexer.koodisto.koodisto
  (:require [kouta-indeksoija-service.rest.koodisto :as koodisto-service]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.util.time :refer [date-is-before-now?]]))

(def index-name "koodisto")

(defn ->koodi-entry
  [koodi]
  (-> (select-keys koodi [:koodiUri :versio])
      (assoc :nimi (koodisto-service/extract-koodi-nimi koodi))))

(defn- assoc-alakoodi-entries
  [entry alakoodiUri]
  (->> (-> (:koodiUri entry)
           (koodisto-service/list-alakoodit-with-cache alakoodiUri))
       (map ->koodi-entry)
       (vec)
       (assoc entry :alakoodit)))

(defn create-koodi-entry
  [koodisto koodi]
  (cond-> (->koodi-entry koodi)
    (= koodisto/koodiuri-koulutusalataso1 koodisto) (assoc-alakoodi-entries koodisto/koodiuri-koulutusalataso2)))

(defn filter-expired [koodit]
  (filter (fn [koodi]
            (not (if-let [loppu (:voimassaLoppuPvm koodi)]
                   (date-is-before-now? loppu)))) koodit))

(defn create-index-entry
  [koodisto]
  (let [koodit (filter-expired (koodisto-service/get-koodit koodisto))]
    (indexable/->index-entry koodisto {:id koodisto
                                       :koodisto koodisto
                                       :koodit (vec (map (partial create-koodi-entry koodisto) koodit))})))

(defn do-index
  [koodistot execution-id]
  (indexable/do-index index-name koodistot create-index-entry execution-id))

(defn get-from-index
  [koodisto]
  (indexable/get index-name koodisto))