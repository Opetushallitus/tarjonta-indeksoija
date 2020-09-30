(ns kouta-indeksoija-service.indexer.kouta.oppilaitos-search
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache list-alakoodi-nimet-with-cache]]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.indexer.tools.hakuaika :refer [->real-hakuajat]]
            [kouta-indeksoija-service.util.tools :refer [->distinct-vec]]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.general :refer :all]
            [kouta-indeksoija-service.indexer.tools.search :refer :all]))

(def index-name "oppilaitos-kouta-search")

(defn- tarjoaja-organisaatiot
  [oppilaitos tarjoajat]
  (vec (map #(organisaatio-tool/find-from-organisaatio-and-children oppilaitos %) tarjoajat)))

(defn- tutkintonimikkeet-for-osaamisala
  [osaamisalaKoodiUri]
  (list-alakoodi-nimet-with-cache osaamisalaKoodiUri "tutkintonimikkeet"))

(defn- tutkintonimikket-for-toteutus
  [toteutus]
  ;TODO -> eperusteet
  (when (ammatillinen? toteutus)
    (->> (get-in toteutus [:metadata :osaamisalat :koodiUri])
         (mapcat tutkintonimikkeet-for-osaamisala)
         (->distinct-vec))))

(defn oppilaitos-hit
  [oppilaitos]
  (hit :koulutustyyppi  (koulutustyyppi-for-organisaatio oppilaitos)
       :opetuskieliUrit (:kieletUris oppilaitos)
       :tarjoajat       (vector oppilaitos)
       :oppilaitos      oppilaitos
       :logo            (:logo oppilaitos)))

(defn koulutus-hit
  [oppilaitos koulutus]
  (hit :koulutustyyppi     (:koulutustyyppi koulutus)
       :koulutustyyppiUrit (koulutustyyppiKoodiUrit koulutus)
       ;:opetuskieliUrit   (:kieletUris oppilaitos)
       :tarjoajat          (tarjoaja-organisaatiot oppilaitos (:tarjoajat koulutus))
       :tarjoajaOids       (:tarjoajat koulutus)
       :oppilaitos         oppilaitos
       :koulutusalaUrit    (koulutusalaKoodiUrit koulutus)
       :tutkintonimikeUrit (tutkintonimikeKoodiUrit koulutus)
       :nimet              (vector (:nimi koulutus))
       :koulutusOid        (:oid koulutus)
       :kuva               (:teemakuva koulutus)
       :onkoTuleva         true
       :nimi               (:nimi koulutus)
       :metadata           (cond-> {:tutkintonimikkeetKoodiUrit      (tutkintonimikeKoodiUrit koulutus)
                                    :opintojenLaajuusKoodiUri        (opintojenlaajuusKoodiUri koulutus)
                                    :opintojenLaajuusyksikkoKoodiUri (opintojenlaajuusyksikkoKoodiUri koulutus)
                                    :koulutustyypitKoodiUrit         (koulutustyyppiKoodiUrit koulutus)
                                    :koulutustyyppi                  (:koulutustyyppi koulutus)}
                                   (amm-tutkinnon-osa? koulutus) (assoc :tutkinnonOsat (tutkinnonOsaKoodiUrit koulutus)))))

(defn toteutus-hit
  [oppilaitos koulutus toteutus]
  (let [opetus (get-in toteutus [:metadata :opetus])]

    (hit :koulutustyyppi     (:koulutustyyppi koulutus)
         :koulutustyyppiUrit (koulutustyyppiKoodiUrit koulutus)
         :opetuskieliUrit    (get-in toteutus [:metadata :opetus :opetuskieliKoodiUrit])
         :tarjoajat          (tarjoaja-organisaatiot oppilaitos (:tarjoajat toteutus))
         :tarjoajaOids       (:tarjoajat toteutus)
         :oppilaitos         oppilaitos
         :koulutusalaUrit    (koulutusalaKoodiUrit koulutus)
         :tutkintonimikeUrit (tutkintonimikeKoodiUrit koulutus)
         :nimet              (vector (:nimi koulutus) (:nimi toteutus))
         :asiasanat          (asiasana->lng-value-map (get-in toteutus [:metadata :asiasanat]))
         :ammattinimikkeet   (asiasana->lng-value-map (get-in toteutus [:metadata :ammattinimikkeet]))
         :koulutusOid        (:oid koulutus)
         :toteutusOid        (:oid toteutus)
         :nimi               (:nimi toteutus)
         :kuva               (:teemakuva toteutus)
         :onkoTuleva         false
         :metadata           (cond-> {:tutkintonimikkeet  (tutkintonimikket-for-toteutus toteutus)
                                      :opetusajatKoodiUrit (:opetusaikaKoodiUrit opetus)
                                      :onkoMaksullinen     (:onkoMaksullinen opetus)
                                      :maksunMaara         (:maksunMaara opetus)
                                      :koulutustyyppi      (:koulutustyyppi koulutus)}
                                     (amm-tutkinnon-osa? koulutus) (assoc :tutkinnonOsat (tutkinnonOsaKoodiUrit koulutus))))))

(defn- get-kouta-oppilaitos
  [oid]
  (let [oppilaitos (kouta-backend/get-oppilaitos oid)]
    (when (julkaistu? oppilaitos)
      {:kielivalinta (:kielivalinta oppilaitos)
       :kuvaus       (get-in oppilaitos [:metadata :esittely])
       :logo         (:logo oppilaitos)})))

(defn- create-base-entry
  [oppilaitos koulutukset]
  (-> oppilaitos
      (select-keys [:oid :nimi])
      (merge (get-kouta-oppilaitos (:oid oppilaitos)))
      (assoc :koulutusohjelmia (count (filter :johtaaTutkintoon koulutukset)))))

(defn- assoc-paikkakunnat
  [entry]
  (let [paikkakuntaKoodiUrit (vec (distinct (filter #(clojure.string/starts-with? % "kunta") (mapcat :sijainti (:hits entry)))))]
    (assoc entry :paikkakunnat (vec (map get-koodi-nimi-with-cache paikkakuntaKoodiUrit)))))

(defn get-tarjoaja-entries
  [hierarkia entries]
  (->> (for [entry entries]
         (when-let [indexable-oids (seq (organisaatio-tool/filter-indexable-oids-for-hierarkia hierarkia (:tarjoajat entry)))]
           (assoc entry :tarjoajat indexable-oids)))
       (remove nil?)
       (vec)))

(defn- create-koulutus-hits
  [oppilaitos hierarkia koulutus]
  (if-let [toteutukset (seq (get-tarjoaja-entries hierarkia (kouta-backend/get-toteutus-list-for-koulutus (:oid koulutus) true)))]
    (vec (map #(toteutus-hit oppilaitos koulutus %) toteutukset))
    (vector (koulutus-hit oppilaitos koulutus))))

(defn- create-oppilaitos-entry-with-hits
  [oppilaitos hierarkia]
  (let [koulutus-hits (partial create-koulutus-hits oppilaitos hierarkia)
        koulutukset (get-tarjoaja-entries hierarkia (kouta-backend/get-koulutukset-by-tarjoaja (:oid oppilaitos)))]
    (-> oppilaitos
        (create-base-entry koulutukset)
        (assoc :hits (if (seq koulutukset)
                       (vec (mapcat #(koulutus-hits %) koulutukset))
                       (vector (oppilaitos-hit oppilaitos))))
        (assoc-paikkakunnat))))

(defn create-index-entry
  [oid]
  (let [hierarkia (cache/get-hierarkia oid)]
    (when-let [oppilaitos (organisaatio-tool/find-oppilaitos-from-hierarkia hierarkia)]
      (if (organisaatio-tool/indexable? oppilaitos)
        (indexable/->index-entry (:oid oppilaitos) (create-oppilaitos-entry-with-hits oppilaitos hierarkia))
        (indexable/->delete-entry (:oid oppilaitos))))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get
  [oid]
  (indexable/get index-name oid))
