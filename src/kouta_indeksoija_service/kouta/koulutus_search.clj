(ns kouta-indeksoija-service.kouta.koulutus-search
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.kouta.common :as common]
            [kouta-indeksoija-service.kouta.tools.hakuaika :refer [->real-hakuajat]]
            [kouta-indeksoija-service.kouta.indexable :as indexable]
            [kouta-indeksoija-service.util.time :refer [kouta-date-to-long]]))

(def index-name "koulutus-kouta-search")

(defn- transform-asiasanat
         [asiasanat]
         (map (fn [a] { (keyword (:kieli a)) (:arvo a)} ) asiasanat))

(defn- some-true
  [coll]
  (some? (some true? coll)))

(defn- shrink-koulutus
  [k]
  (-> k
      (dissoc :metadata :julkinen :timestamp :kielivalinta :muokkaaja :modified :organisaatio)))

(defn- shrink-toteutus
  [t ]
  (-> t
      (dissoc :koulutusOid)
      (update-in [:metadata] dissoc :kuvaus :yhteystieto)
      (update-in [:metadata :opetus] dissoc :osiot)
      (update-in [:metadata :asiasanat] transform-asiasanat)
      (update-in [:metadata :ammattinimikkeet] transform-asiasanat)))

(defn- create-toteutus-entry
  [t shrinked-koulutus hakutiedot]
  (let [hakutieto (first (filter (fn [x] (= (:toteutusOid x) (:oid t))) hakutiedot))
        hakuajat (->real-hakuajat hakutieto)]
    (-> t shrink-toteutus (assoc :koulutus shrinked-koulutus :haut (:haut hakutieto) :hakuOnKaynnissa hakuajat))))

(defn create-index-entry
  [oid]
  (let [koulutus (common/complete-entry (kouta-backend/get-koulutus oid))]
    (when (= (:tila koulutus) "julkaistu")
      (let [shrinked-koulutus (shrink-koulutus koulutus)
            toteutukset (common/complete-entries (kouta-backend/get-toteutus-list-for-koulutus oid true))
            hakutiedot (common/complete-entries (kouta-backend/get-hakutiedot-for-koulutus oid))]
        (-> koulutus
            (update-in [:metadata] dissoc :kuvaus)
            (assoc :toteutukset (map #(create-toteutus-entry % shrinked-koulutus hakutiedot) toteutukset)))))))


(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entries))
