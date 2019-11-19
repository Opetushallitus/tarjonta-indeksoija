(ns kouta-indeksoija-service.indexer.kouta.common
  (:require [kouta-indeksoija-service.rest.kouta :refer [get-koulutus]]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.cache.tarjoaja :as tarjoaja]
            [kouta-indeksoija-service.rest.oppijanumerorekisteri :refer [get-henkilo-nimi-with-cache]]
            [clojure.string :refer [replace]]
            [clojure.walk :refer [postwalk]]))

(defn- strip-koodi-uri-key
  [key]
  (if (keyword? key)
    (-> key
        (name)
        (replace "KoodiUrit" "")
        (replace "KoodiUri" "")
        (replace "Uri" "")
        (keyword))
    key))

(defn- decorate-koodi-value
  [value]
  (if (and (string? value) (re-find (re-pattern "\\w+_\\w+[#\\d{1,2}]?") value))
    (get-koodi-nimi-with-cache value)
    value))

(defn decorate-koodi-uris
  [x]
  (postwalk #(-> % strip-koodi-uri-key decorate-koodi-value) x))

(defn assoc-organisaatio
  [entry]
  (if-let [oid (:organisaatioOid entry)]
    (assoc (dissoc entry :organisaatioOid) :organisaatio (tarjoaja/get-tarjoaja oid))
    entry))


(defn assoc-muokkaaja
  [entry]
  (if-let [oid (:muokkaaja entry)]
    (if-let [nimi (get-henkilo-nimi-with-cache oid)]
      (assoc entry :muokkaaja {:oid oid :nimi nimi})
      (assoc entry :muokkaaja {:oid oid}))
    entry))

(defn assoc-tarjoajat
  [entry]
  (if-let [oids (:tarjoajat entry)]
    (assoc entry :tarjoajat (map #(tarjoaja/get-tarjoaja %1) oids))
    entry))

(defn assoc-organisaatiot
  [entry]
  (let [organisaatio (get-in entry [:organisaatio :oid])
        tarjoajat (map :oid (:tarjoajat entry))]
    (assoc entry :organisaatiot (vec (distinct (remove nil? (conj tarjoajat organisaatio)))))))

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
