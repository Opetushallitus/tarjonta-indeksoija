(ns kouta-indeksoija-service.indexer.kouta.koulutus-search
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.indexer.tools.hakuaika :refer [->real-hakuajat]]
            [kouta-indeksoija-service.indexer.tools.general :refer :all]
            [kouta-indeksoija-service.indexer.tools.search :refer :all]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.kouta.common :as common]))

(def index-name "koulutus-kouta-search")

(defn get-tarjoaja-and-oppilaitos
  [oid]
  (let [hierarkia (cache/get-hierarkia oid)]
    {:tarjoaja (organisaatio-tool/find-from-hierarkia hierarkia oid)
     :oppilaitos (organisaatio-tool/find-oppilaitos-from-hierarkia hierarkia)}))

(defn koulutus-hit
  [koulutus]
  (let [organisaatiot (map get-tarjoaja-and-oppilaitos (:tarjoajat koulutus))]
    (hit :koulutustyyppi (:koulutustyyppi koulutus)
         :tarjoajat (vec (map :tarjoaja organisaatiot))
         :oppilaitokset (vec (map :oppilaitos organisaatiot))
         :koulutusalaUrit (koulutusalaKoodiUrit koulutus)
         :nimi (:nimi koulutus))))

(defn toteutus-hit
  [koulutus toteutus]
  (let [organisaatiot (map get-tarjoaja-and-oppilaitos (:tarjoajat toteutus))]
    (hit :koulutustyyppi (:koulutustyyppi koulutus)
         :opetuskieliUrit (get-in toteutus [:metadata :opetus :opetuskieliKoodiUrit])
         :tarjoajat (vec (map :tarjoaja organisaatiot))
         :oppilaitokset (vec (map :oppilaitos organisaatiot))
         :koulutusalaUrit (koulutusalaKoodiUrit koulutus)
         :nimi (:nimi toteutus)
         ;:hakuOnKaynnissa (->real-hakuajat hakutieto) TODO
         ;:haut (:haut hakutieto) TODO
         :asiasanat (asiasana->lng-value-map (get-in toteutus [:metadata :asiasanat]))
         :ammattinimikkeet (asiasana->lng-value-map (get-in toteutus [:metadata :ammattinimikkeet])))))

(defn- create-base-entry
  [koulutus]
  (-> koulutus
      (select-keys [:oid :nimi :kielivalinta])
      (assoc :koulutus (:koulutusKoodiUri koulutus))
      (assoc :tutkintonimikkeet (tutkintonimikeKoodiUrit koulutus))
      (assoc :kuvaus (get-in koulutus [:metadata :kuvaus]))
      (assoc :koulutustyyppi (:koulutustyyppi koulutus))
      (assoc :opintojenlaajuus (opintojenlaajuusKoodiUri koulutus))
      (assoc :opintojenlaajuusyksikko (opintojenlaajuusyksikkoKoodiUri koulutus))
      (common/decorate-koodi-uris)))

;TODO
; (defn get-toteutuksen-hakutieto
;  [hakutiedot t]
;  (first (filter (fn [x] (= (:toteutusOid x) (:oid t))) hakutiedot)))

(defn create-index-entry
  [oid]
  (let [koulutus (kouta-backend/get-koulutus oid)]
    (when (julkaistu? koulutus)
      (let [toteutukset (seq (kouta-backend/get-toteutus-list-for-koulutus oid true))
            ;hakutiedot (when toteutukset (kouta-backend/get-hakutiedot-for-koulutus oid)) TODO
            ]
        (-> koulutus
            (create-base-entry)
            (assoc :hits (if toteutukset
                           (vec (map #(toteutus-hit koulutus %) toteutukset))
                           (vector (koulutus-hit koulutus)))))))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get
  [oid]
  (indexable/get index-name oid))