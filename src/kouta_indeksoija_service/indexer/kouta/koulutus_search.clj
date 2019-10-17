(ns kouta-indeksoija-service.indexer.kouta.koulutus-search
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.tools.hakuaika :refer [->real-hakuajat]]
            [kouta-indeksoija-service.indexer.tools.general :refer :all]
            [kouta-indeksoija-service.indexer.tools.search :refer :all]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "koulutus-kouta-search")

(defn get-tarjoaja-and-oppilaitos
  [oid]
  (let [hierarkia (organisaatio-client/get-hierarkia-v4 oid :aktiiviset true :suunnitellut false :lakkautetut false :skipParents false)]
    {:tarjoaja (organisaatio-tool/find-from-hierarkia hierarkia oid)
     :oppilaitos (organisaatio-tool/find-oppilaitos-from-hierarkia hierarkia)}))

(defn koulutus-hit
  [koulutus]
  (let [organisaatiot (map get-tarjoaja-and-oppilaitos (:tarjoajat koulutus))]
    (hit :koulutustyyppi (:koulutustyyppi koulutus)
         :tarjoajat (vec (map :tarjoaja organisaatiot))
         :oppilaitokset (vec (map :oppilaitos organisaatiot))
         :koulutusalaUrit (get-in koulutus [:metadata :koulutusalaKoodiUrit])
         :nimi (:nimi koulutus))))

(defn toteutus-hit
  [koulutus toteutus]
  (let [organisaatiot (map get-tarjoaja-and-oppilaitos (:tarjoajat toteutus))]
    (hit :koulutustyyppi (:koulutustyyppi koulutus)
         :opetuskieliUrit (get-in toteutus [:metadata :opetus :opetuskieliKoodiUrit])
         :tarjoajat (vec (map :tarjoaja organisaatiot))
         :oppilaitokset (vec (map :oppilaitos organisaatiot))
         :koulutusalaUrit (get-in koulutus [:metadata :koulutusalaKoodiUrit])
         :nimi (:nimi toteutus)
         ;:hakuOnKaynnissa (->real-hakuajat hakutieto) TODO
         ;:haut (:haut hakutieto) TODO
         :asiasanat (asiasana->lng-value-map (get-in toteutus [:metadata :asiasanat]))
         :ammattinimikkeet (asiasana->lng-value-map (get-in toteutus [:metadata :ammattinimikkeet])))))

;TODO kesto
;TODO ammatilliselle koodistosta tutkintonimikkeet
(defn- create-base-entry
  [koulutus]
  (-> koulutus
      (select-keys [:oid :nimi :kielivalinta])
      (assoc :tutkintonimikkeet (get-in koulutus [:metadata :tutkintonimikeKoodiUrit]))
      (assoc :kuvaus (get-in koulutus [:metadata :kuvaus]))))

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

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entries))

(defn get
  [oid]
  (indexable/get index-name oid))