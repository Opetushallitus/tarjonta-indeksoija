(ns kouta-indeksoija-service.indexer.kouta.koulutus
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.cache.eperuste :refer [get-eperuste-by-koulutuskoodi get-eperuste-by-id filter-tutkinnon-osa]]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.util.time :refer [long->indexed-date-time]]
            [kouta-indeksoija-service.util.tools :refer [->distinct-vec]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.general :refer [ammatillinen? amm-tutkinnon-osa? amm-osaamisala? korkeakoulutus? lukio?]]
            [kouta-indeksoija-service.indexer.tools.koodisto :refer [koulutusalat-taso1]]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [remove-uri-version]]))

(def index-name "koulutus-kouta")

(defn- get-non-korkeakoulu-koodi-uri
  [koulutus]
  (-> koulutus
      (:koulutukset)
      (first) ;Ainoastaan korkeakoulutuksilla voi olla useampi kuin yksi koulutusKoodi
      (:koodiUri)))

;TODO korvaa pelkällä get-eperuste-by-id, kun kaikki tuotantodata käyttää ePeruste id:tä
(defn- enrich-ammatillinen-metadata
  [koulutus]
  (let [koulutusKoodi (get-non-korkeakoulu-koodi-uri koulutus)
        eperusteId (:ePerusteId koulutus)
        eperuste (if eperusteId (get-eperuste-by-id eperusteId) (get-eperuste-by-koulutuskoodi koulutusKoodi))]
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
  (let [tutkinnon-osat (get-in koulutus [:metadata :tutkinnonOsat])]
    (-> koulutus
        (assoc-in [:metadata :tutkinnonOsat] (get-enriched-tutkinnon-osat tutkinnon-osat))
        (assoc-in [:metadata :koulutusala] (->> tutkinnon-osat
                                                (map #(get-in % [:koulutus :koodiUri]))
                                                (mapcat #(koulutusalat-taso1 %))
                                                (distinct))))))

(defn- get-osaamisala
  [eperuste koulutus]
  (when-let [osaamisalaKoodiUri (some-> koulutus :metadata :osaamisala :koodiUri remove-uri-version)]
    (some->> eperuste
             :osaamisalat
             (filter #(= osaamisalaKoodiUri (some-> % :koodiUri remove-uri-version)))
             (first))))

(defn- enrich-osaamisala-metadata
  [koulutus]
  (let [koulutusKoodi (get-non-korkeakoulu-koodi-uri koulutus)
        eperuste (some-> koulutus :ePerusteId (get-eperuste-by-id))
        osaamisala (get-osaamisala eperuste koulutus)]
    (-> koulutus
        (assoc-in [:metadata :opintojenLaajuus] (:opintojenLaajuus osaamisala))
        (assoc-in [:metadata :opintojenLaajuusyksikko] (:opintojenLaajuusyksikko eperuste))
        (assoc-in [:metadata :opintojenLaajuusNumero] (:opintojenLaajuusNumero osaamisala))
        (assoc-in [:metadata :koulutusala] (koulutusalat-taso1 koulutusKoodi)))))

(defonce ^:private opintopiste-laajuusyksikkokoodi "opintojenlaajuusyksikko_2#1")
(defonce ^:private ylioppilas-tutkintonimikekoodi "tutkintonimikkeet_00001#1")

(defn- get-opintopiste-laajuusyksikko
  []
  (get-koodi-nimi-with-cache opintopiste-laajuusyksikkokoodi))

(defn- enrich-korkeakoulutus-metadata
  [koulutus]
  (-> koulutus
      (assoc-in [:metadata :opintojenLaajuusyksikko] (get-opintopiste-laajuusyksikko))))

(defn- enrich-lukio-metadata
  [koulutus]
  (-> koulutus
      (assoc-in [:metadata :opintojenLaajuusyksikko] (get-opintopiste-laajuusyksikko))
      (assoc-in [:metadata :tutkintonimike] (get-koodi-nimi-with-cache ylioppilas-tutkintonimikekoodi))))

(defn- enrich-common-metadata
  [koulutus]
  (let [eperuste (some-> koulutus :ePerusteId (get-eperuste-by-id))]
    (cond-> (assoc-in koulutus [:metadata :tutkintonimike] [])
            (some? eperuste) (#(-> %
                                   (assoc-in [:metadata :eperuste :id]                (:id eperuste))
                                   (assoc-in [:metadata :eperuste :diaarinumero]      (:diaarinumero eperuste))
                                   (assoc-in [:metadata :eperuste :voimassaoloLoppuu] (some-> eperuste
                                                                                              :voimassaoloLoppuu
                                                                                              (long->indexed-date-time))))))))

(defn- enrich-koulutustyyppi-based-metadata
  [koulutus]
  (cond
    (ammatillinen? koulutus)      (enrich-ammatillinen-metadata koulutus)
    (amm-tutkinnon-osa? koulutus) (enrich-tutkinnon-osa-metadata koulutus)
    (amm-osaamisala? koulutus)    (enrich-osaamisala-metadata koulutus)
    (korkeakoulutus? koulutus)    (enrich-korkeakoulutus-metadata koulutus)
    (lukio? koulutus)             (enrich-lukio-metadata koulutus)
    :default koulutus))

(defn- enrich-metadata
  [koulutus]
  (-> koulutus
      (enrich-common-metadata)
      (enrich-koulutustyyppi-based-metadata)))

(defn- assoc-sorakuvaus
  [koulutus]
  (if-let [sorakuvaus-id (:sorakuvausId koulutus)]
    (assoc koulutus :sorakuvaus (common/complete-entry (kouta-backend/get-sorakuvaus sorakuvaus-id)))
    koulutus))

(defn create-index-entry
  [oid]
  (let [koulutus (kouta-backend/get-koulutus oid)
        toteutukset (common/complete-entries (kouta-backend/get-toteutus-list-for-koulutus oid))]
    (indexable/->index-entry oid (-> koulutus
                                     (common/complete-entry)
                                     (common/assoc-organisaatiot)
                                     (enrich-metadata)
                                     (assoc-sorakuvaus)
                                     (assoc :toteutukset (map common/toteutus->list-item toteutukset))))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
