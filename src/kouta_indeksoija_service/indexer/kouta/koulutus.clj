(ns kouta-indeksoija-service.indexer.kouta.koulutus
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.cache.eperuste :refer [get-eperuste-by-koulutuskoodi get-eperuste-by-id filter-tutkinnon-osa]]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache list-alakoodi-nimet-with-cache]]
            [kouta-indeksoija-service.util.time :refer [long->indexed-date-time]]
            [kouta-indeksoija-service.util.tools :refer [->distinct-vec get-oids]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.general :refer [ammatillinen? amm-tutkinnon-osa? amm-osaamisala? korkeakoulutus? lukio? tuva? telma? not-poistettu? aikuisten-perusopetus?]]
            [kouta-indeksoija-service.indexer.tools.koodisto :refer [koulutusalat-taso1 koodiuri-opintopiste-laajuusyksikko koodiuri-ylioppilas-tutkintonimike koodiuri-viikko-laajuusyksikko koodiuri-osaamispiste-laajuusyksikko]]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [remove-uri-version]]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto]))

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



(defn- get-opintopiste-laajuusyksikko
  []
  (get-koodi-nimi-with-cache koodiuri-opintopiste-laajuusyksikko))

(defn- enrich-korkeakoulutus-metadata
  [koulutus]
  (-> koulutus
      (assoc-in [:metadata :opintojenLaajuusyksikko] (get-opintopiste-laajuusyksikko))))

(defn- enrich-lukio-metadata
  [koulutus]
  (-> koulutus
      (assoc-in [:metadata :opintojenLaajuusyksikko] (get-opintopiste-laajuusyksikko))
      (assoc-in [:metadata :tutkintonimike] (vector (get-koodi-nimi-with-cache koodiuri-ylioppilas-tutkintonimike)))))

(defn enrich-tuva-metadata
  [koulutus]
  (-> koulutus
      (assoc-in [:metadata :opintojenLaajuusyksikko] (get-koodi-nimi-with-cache koodiuri-viikko-laajuusyksikko))))

(defn enrich-telma-metadata
  [koulutus]
  (-> koulutus
      (assoc-in [:metadata :opintojenLaajuusyksikko] (get-koodi-nimi-with-cache koodiuri-osaamispiste-laajuusyksikko))))

(defn- does-not-have-tutkintonimike?
  [koulutus]
  (nil? (get-in koulutus [:metadata :tutkintonimike])))

(defn- get-alakoodis-for-multiple-koodiUri
  [koodiUrit alaKoodiKoodistoUri]
  (let [alakoodit (map #(list-alakoodi-nimet-with-cache % alaKoodiKoodistoUri) koodiUrit)]
    (->distinct-vec (flatten alakoodit))))

(defn- assoc-eqf-and-nqf
  [koulutus]
  (let [koulutusKoodiUrit (map #(get % :koodiUri) (get koulutus :koulutukset))
        eqf (get-alakoodis-for-multiple-koodiUri koulutusKoodiUrit "eqf")
        nqf (get-alakoodis-for-multiple-koodiUri koulutusKoodiUrit "nqf")]
    (-> koulutus
      (assoc :eqf eqf)
      (assoc :nqf nqf))))

(defn- enrich-common-metadata
  [koulutus]
  (let [eperuste (some-> koulutus :ePerusteId (get-eperuste-by-id))]
    (cond-> koulutus
            (does-not-have-tutkintonimike? koulutus) (assoc-in [:metadata :tutkintonimike] [])
            (some? eperuste) (#(-> %
                                   (assoc-in [:metadata :eperuste :id]                (:id eperuste))
                                   (assoc-in [:metadata :eperuste :diaarinumero]      (:diaarinumero eperuste))
                                   (assoc-in [:metadata :eperuste :voimassaoloLoppuu] (some-> eperuste
                                                                                              :voimassaoloLoppuu
                                                                                              (long->indexed-date-time))))))))

(defn- enrich-koulutustyyppi-based-metadata
  [koulutus]
  (cond
    (ammatillinen? koulutus)          (enrich-ammatillinen-metadata koulutus)
    (amm-tutkinnon-osa? koulutus)     (enrich-tutkinnon-osa-metadata koulutus)
    (amm-osaamisala? koulutus)        (enrich-osaamisala-metadata koulutus)
    (korkeakoulutus? koulutus)        (enrich-korkeakoulutus-metadata koulutus)
    (lukio? koulutus)                 (enrich-lukio-metadata koulutus)
    (tuva? koulutus)                  (enrich-tuva-metadata koulutus)
    (telma? koulutus)                 (enrich-telma-metadata koulutus)
    :default koulutus))

(defn- enrich-metadata
  [koulutus]
  (-> koulutus
      (enrich-common-metadata)
      (enrich-koulutustyyppi-based-metadata)))

(defn- assoc-sorakuvaus
  [koulutus execution-id]
  (if-let [sorakuvaus-id (:sorakuvausId koulutus)]
    (assoc koulutus :sorakuvaus (common/complete-entry (kouta-backend/get-sorakuvaus-with-cache sorakuvaus-id execution-id)))
    koulutus))

(defn- create-koulutuskoodiuri-with-aste-and-ala
  [koodiuri]
  {:koulutusKoodiUri koodiuri
   :koulutusalaKoodiUrit (koodisto/koulutusalat koodiuri)
   :koulutusasteKoodiUrit (koodisto/koulutusasteet koodiuri)})

(defn- assoc-koulutusala-and-koulutusaste
  [koulutus]
  (let [koulutusAlaJaAstekoodiUrit (->> (:koulutukset koulutus)
                                        (map :koodiUri)
                                        (map create-koulutuskoodiuri-with-aste-and-ala))]
    (assoc koulutus :koulutuskoodienAlatJaAsteet koulutusAlaJaAstekoodiUrit)))

(defn create-index-entry
  [oid execution-id]
  (let [koulutus (common/complete-entry (kouta-backend/get-koulutus-with-cache oid execution-id))]
    (if (not-poistettu? koulutus)
      (let [toteutukset (common/complete-entries (kouta-backend/get-toteutus-list-for-koulutus-with-cache oid execution-id))
            hakutiedot (when toteutukset (kouta-backend/get-hakutiedot-for-koulutus-with-cache oid execution-id))
            haku-oids (get-oids :hakuOid (mapcat :haut hakutiedot))
            koulutus-enriched (-> koulutus
                                  (common/assoc-organisaatiot)
                                  (enrich-metadata)
                                  (assoc-eqf-and-nqf)
                                  (assoc-sorakuvaus execution-id)
                                  (assoc :haut haku-oids)
                                  (assoc :toteutukset (map common/toteutus->list-item toteutukset))
                                  (assoc-koulutusala-and-koulutusaste)
                                  (common/localize-dates))]
        (indexable/->index-entry-with-forwarded-data oid koulutus-enriched koulutus-enriched))
      (indexable/->delete-entry-with-forwarded-data oid koulutus))))

(defn do-index
  [oids execution-id]
  (indexable/do-index index-name oids create-index-entry execution-id))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
