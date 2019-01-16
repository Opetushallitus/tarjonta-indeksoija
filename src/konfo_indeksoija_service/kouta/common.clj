(ns konfo-indeksoija-service.kouta.common
  (:require [konfo-indeksoija-service.converter.tyyppi :refer [koulutustyyppi-uri-to-tyyppi]]
            [konfo-indeksoija-service.rest.kouta :refer [get-toteutus-list-for-koulutus]]
            [konfo-indeksoija-service.rest.organisaatio :as organisaatio-service]
            [konfo-indeksoija-service.rest.koodisto :refer [get-koodi-with-cache]]
            [konfo-indeksoija-service.kouta.cache.tarjoaja :as tarjoaja]
            [clojure.tools.logging :as log]))

(defn assoc-organisaatio
  [entry]
  (if-let [oid (:organisaatioOid entry)]
    (assoc (dissoc entry :organisaatioOid) :organisaatio (tarjoaja/get-tarjoaja oid))
    entry))

;TODO -> Hae muokkaaja oppijanumerorekisteristä (vaatii CASsin)
(def muokkaaja (memoize (fn [oid] {:nimi (rand-nth ["Aku Ankka" "Minni Hiiri" "Mikki Hiiri"])})))

(defn assoc-muokkaaja
  [entry]
  (if-let [oid (:muokkaaja entry)]
    (assoc entry :muokkaaja {:oid oid :nimi (:nimi (muokkaaja oid))})
    entry))

(defn assoc-tarjoajat
  [entry]
  (if-let [oids (:tarjoajat entry)]
    (assoc entry :tarjoajat (map #(tarjoaja/get-tarjoaja %1) oids))
    entry))

(defn complete-entry
  [entry]
  (-> entry
      (assoc-organisaatio)
      (assoc-tarjoajat)
      (assoc-muokkaaja)))

(defn complete-entries
  [entries]
  (map complete-entry entries))