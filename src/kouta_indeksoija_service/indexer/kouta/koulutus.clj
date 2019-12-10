(ns kouta-indeksoija-service.indexer.kouta.koulutus
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.toteutus :refer [to-list-item]]
            [kouta-indeksoija-service.indexer.tools.general :refer [ammatillinen?]]
            [kouta-indeksoija-service.indexer.tools.koodisto :refer :all]))

(def index-name "koulutus-kouta")

;TODO: amm tutkintonimikkeet, koulutuksen laajuus, opetuskielet, suunniteltu kesto ja koulutusaste

(defn enrich-ammatillinen-metadata
  [koulutus]
  (if (ammatillinen? koulutus)
    (let [koulutusKoodi (:koulutusKoodiUri koulutus)]
      (-> koulutus
          (assoc-in [:metadata :tutkintonimike]          (tutkintonimikkeet koulutusKoodi))
          (assoc-in [:metadata :opintojenLaajuus]        (opintojenlaajuus koulutusKoodi))
          (assoc-in [:metadata :opintojenLaajuusyksikko] (opintojenlaajuusyksikko koulutusKoodi))
          (assoc-in [:metadata :koulutusala]             (koulutusalat-taso1 koulutusKoodi))))
    koulutus))

(defn create-index-entry
  [oid]
  (let [koulutus (common/complete-entry (kouta-backend/get-koulutus oid))
        toteutukset (common/complete-entries (kouta-backend/get-toteutus-list-for-koulutus oid))]
    (-> koulutus
        (common/assoc-organisaatiot)
        (enrich-ammatillinen-metadata)
        (assoc :toteutukset (map to-list-item toteutukset)))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get
  [oid]
  (indexable/get index-name oid))