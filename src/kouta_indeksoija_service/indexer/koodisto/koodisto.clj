(ns kouta-indeksoija-service.indexer.koodisto.koodisto
  (:require [kouta-indeksoija-service.rest.koodisto :as koodisto-service]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "koodisto")

(defonce koodiUriKoulutusalataso1 "kansallinenkoulutusluokitus2016koulutusalataso1")
(defonce koodiUriKoulutusalataso2 "kansallinenkoulutusluokitus2016koulutusalataso2")

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
    (= koodiUriKoulutusalataso1 koodisto) (assoc-alakoodi-entries koodiUriKoulutusalataso2)))

(defn create-index-entry
  [koodisto]
  (let [koodit (koodisto-service/get-koodit koodisto)]
    (indexable/->index-entry koodisto {:id koodisto
                                       :koodisto koodisto
                                       :koodit (vec (map (partial create-koodi-entry koodisto) koodit))})))

(defn do-index
  [koodistot]
  (indexable/do-index index-name koodistot create-index-entry))

(defn get
  [koodisto]
  (indexable/get index-name koodisto))