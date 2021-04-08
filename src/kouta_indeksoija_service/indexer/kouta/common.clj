(ns kouta-indeksoija-service.indexer.kouta.common
  (:refer-clojure :exclude [replace])
  (:require [kouta-indeksoija-service.rest.kouta :refer [get-koulutus]]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.cache.tarjoaja :as tarjoaja]
            [kouta-indeksoija-service.rest.oppijanumerorekisteri :refer [get-henkilo-nimi-with-cache]]
            [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [clojure.string :refer [replace]]
            [clojure.walk :refer [postwalk]]
            [clojure.string :as string]))

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

(defn- decorate-koodi-value   ;Etsitään koodiUria kaavalla KIRJAIMIANUMEROITA_NONWHITESPACEMERKKEJÄ#NUMERO
  [value]                     ;Numeroita voi olla 1-3 kpl
  (if (and (string? value) (re-find (re-pattern "^\\w+_\\S+(#\\d{1,3})?$") (string/trim value)))
      (get-koodi-nimi-with-cache value)
      value))

(defn decorate-koodi-uris
  [x]
  (postwalk #(-> % strip-koodi-uri-key decorate-koodi-value) x))

(defn- get-tarjoaja
  [oid]
  (assoc (tarjoaja/get-tarjoaja oid) :oid oid))

(defn assoc-organisaatio
  [entry]
  (if-let [oid (:organisaatioOid entry)]
    (assoc (dissoc entry :organisaatioOid) :organisaatio (get-tarjoaja oid))
    entry))

(defn assoc-jarjestyspaikka
  [entry]
  (if-let [oid (:jarjestyspaikkaOid entry)]
    (assoc (dissoc entry :jarjestyspaikkaOid) :jarjestyspaikka (get-tarjoaja oid))
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
    (assoc entry :tarjoajat (map #(get-tarjoaja %1) oids))
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
      (assoc-jarjestyspaikka)
      (assoc-muokkaaja)))

(defn complete-entries
  [entries]
  (map complete-entry entries))

(defn toteutus->list-item
  [toteutus]
  (-> toteutus
      (select-keys [:oid :organisaatio :nimi :tila :tarjoajat :muokkaaja :modified :organisaatiot])
      (assoc-organisaatiot)))

(defn- create-ataru-link
  [haku-oid lang]
  (resolve-url :ataru-hakija.ataru.hakulomake haku-oid lang))

(defn- create-ataru-links
  [haku-oid]
  (if haku-oid {:fi (create-ataru-link haku-oid "fi")
                :sv (create-ataru-link haku-oid "sv")
                :en (create-ataru-link haku-oid "en")}))

(defn create-hakulomake-linkki
  [hakulomaketiedot haku-oid]
  (when-let [linkki (case (:hakulomaketyyppi hakulomaketiedot)
                      "ataru" (create-ataru-links haku-oid)
                      "muu"   (:hakulomakeLinkki hakulomaketiedot)
                      nil)]
    {:hakulomakeLinkki linkki}))
