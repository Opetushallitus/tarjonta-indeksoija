(ns kouta-indeksoija-service.indexer.kouta.koulutus
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.cache.eperuste :refer [get-eperuste-by-koulutuskoodi get-eperuste-by-id filter-tutkinnon-osa]]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.util.tools :refer [->distinct-vec]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.general :refer [ammatillinen? amm-tutkinnon-osa?]]
            [kouta-indeksoija-service.indexer.tools.koodisto :refer :all]))

(def index-name "koulutus-kouta")

;TODO korvaa pelkällä get-eperuste-by-id, kun kaikki tuotantodata käyttää ePeruste id:tä
(defn- enrich-ammatillinen-metadata
  [koulutus]
  (let [koulutusKoodi (get-in koulutus [:koulutus :koodiUri])
        eperusteId (:ePerusteId koulutus)
        eperuste (if eperusteId (get-eperuste-by-id eperusteId) (get-eperuste-by-koulutuskoodi koulutusKoodi)) ]
    (-> koulutus
        (assoc-in [:metadata :tutkintonimike]          (->distinct-vec (map (fn [x] {:koodiUri (:tutkintonimikeUri x) :nimi (:nimi x)}) (:tutkintonimikkeet eperuste))))
        (assoc-in [:metadata :opintojenLaajuus]        (:opintojenLaajuus eperuste))
        (assoc-in [:metadata :opintojenLaajuusyksikko] (:opintojenLaajuusyksikko eperuste))
        (assoc-in [:metadata :koulutusala]             (koulutusalat-taso1 koulutusKoodi)))))

(defn- get-enriched-tutkinnon-osat
  [tutkinnon-osat]
  (vec (for [tutkinnon-osa tutkinnon-osat
            :let [eperuste-id (:ePerusteId tutkinnon-osa)
                  eperuste (get-eperuste-by-id eperuste-id)
                  eperuste-tutkinnon-osa (filter-tutkinnon-osa eperuste (:tutkinnonosaId tutkinnon-osa))]]
        (merge tutkinnon-osa
               {:opintojenLaajuusNumero (:opintojenLaajuusNumero eperuste-tutkinnon-osa)
                :opintojenLaajuus (:opintojenLaajuus eperuste-tutkinnon-osa)
                :opintojenLaajuusyksikko (:opintojenLaajuusyksikko eperuste)
                :tutkinnonOsat (select-keys eperuste-tutkinnon-osa [:koodiUri :nimi])}))))

(defn- enrich-tutkinnon-osa-metadata
  [koulutus]
  (assoc-in koulutus [:metadata :tutkinnonOsat] (get-enriched-tutkinnon-osat (get-in koulutus [:metadata :tutkinnonOsat]))))

(defn- enrich-metadata
  [koulutus]
  (cond
    (ammatillinen? koulutus) (enrich-ammatillinen-metadata koulutus)
    (amm-tutkinnon-osa? koulutus) (enrich-tutkinnon-osa-metadata koulutus)
    :default koulutus))

(defn create-index-entry
  [oid]
  (let [koulutus (common/complete-entry (kouta-backend/get-koulutus oid))
        toteutukset (common/complete-entries (kouta-backend/get-toteutus-list-for-koulutus oid))]
    (indexable/->index-entry oid (-> koulutus
                                     (common/assoc-organisaatiot)
                                     (enrich-metadata)
                                     (assoc :toteutukset (map common/toteutus->list-item toteutukset))))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get
  [oid]
  (indexable/get index-name oid))
