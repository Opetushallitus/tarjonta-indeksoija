(ns konfo-indeksoija-service.kouta.koulutus-search
  (:require [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [konfo-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [konfo-indeksoija-service.kouta.common :as common]
            [konfo-indeksoija-service.kouta.indexable :as indexable]
            [konfo-indeksoija-service.util.time :refer [kouta-date-to-long]]))

(def index-name "koulutus-kouta-search")

(defn- transform-asiasanat
  [asiasanat]
  (map (fn [a] { (keyword (:kieli a)) (:arvo a)} ) asiasanat))

(defn- shrink-toteutus
  [toteutus]
  (-> toteutus
      (dissoc :koulutusOid :kielivalinta)
      (update-in [:metadata] dissoc :kuvaus :yhteystieto)
      (update-in [:metadata :opetus] dissoc :osiot)
      (update-in [:metadata :asiasanat] transform-asiasanat)
      (update-in [:metadata :ammattinimikkeet] transform-asiasanat)))

(defn- create-haut-entry
  [haut]

  (defn- now?
    [hakuaika]
    (if-let [alkaa (:alkaa hakuaika)]
      (if-let [paattyy (:paattyy hakuaika)]
        (< (kouta-date-to-long alkaa) (. System (currentTimeMillis)) (kouta-date-to-long paattyy))
        (< (kouta-date-to-long alkaa) (. System (currentTimeMillis))))
      false))

  (defn- some-true
    [coll]
    (some? (some true? coll)))

  (defn- hakuIsOn?
    [haku]
    (if (some-true (map :kaytetaanHaunAikataulua (:hakukohteet haku)))
      (some-true (map now? (:hakuajat haku)))
      (some-true (map now? (mapcat :hakuajat (:hakukohteet haku))))))

  (if (not-empty haut)
    (map #(assoc % :hakuKäynnissä (hakuIsOn? %) ) haut)
    []))

(defn- create-toteutus-entry
  [t hakutiedot]
  (let [hakutieto (first (filter (fn [x] (= (:toteutusOid x) (:oid t))) hakutiedot))
        haut (create-haut-entry (:haut hakutieto))]
    (-> t shrink-toteutus (assoc :haut haut :hakuKäynnissä (some-true (map :hakuKäynnissä haut))))))

(defn create-index-entry
  [oid]
  (let [koulutus (common/complete-entry (kouta-backend/get-koulutus oid))]
    (when (= (:tila koulutus) "julkaistu")
      (let [toteutukset (common/complete-entries (kouta-backend/get-toteutus-list-for-koulutus oid true))
            hakutiedot (common/complete-entries (kouta-backend/get-hakutiedot-for-koulutus oid))]
        (-> koulutus
            (update-in [:metadata] dissoc :kuvaus)
            (assoc :toteutukset (map #(create-toteutus-entry % hakutiedot) toteutukset)))))))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entries))