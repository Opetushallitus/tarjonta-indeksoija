(ns kouta-indeksoija-service.kouta.common
  (:require [kouta-indeksoija-service.rest.kouta :refer [get-toteutus-list-for-koulutus]]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.kouta.cache.tarjoaja :as tarjoaja]))

(defn- strip-koodi-uri-key
  [key]
  (if (keyword? key)
    (-> key
        (name)
        (clojure.string/replace "KoodiUrit" "")
        (clojure.string/replace "KoodiUri" "")
        (clojure.string/replace "Uri" "")
        (keyword))
    key))

(defn- decorate-koodi-value
  [value]
  (if (and (string? value) (re-find (re-pattern "\\w+_\\w+[#\\d{1,2}]?") value))
    (get-koodi-nimi-with-cache value)
    value))

(defn decorate-koodi-uris
  [x]
  (clojure.walk/postwalk #(-> % strip-koodi-uri-key decorate-koodi-value) x))

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
      (decorate-koodi-uris)
      (assoc-organisaatio)
      (assoc-tarjoajat)
      (assoc-muokkaaja)))

(defn complete-entries
  [entries]
  (map complete-entry entries))
