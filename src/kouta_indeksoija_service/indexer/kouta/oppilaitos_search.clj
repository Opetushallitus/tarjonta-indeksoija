(ns kouta-indeksoija-service.indexer.kouta.oppilaitos-search
  (:require [clojure.string :as string]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache list-alakoodi-nimet-with-cache]]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]
            [kouta-indeksoija-service.indexer.tools.hakutieto :refer [get-search-hakutiedot]]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.util.tools :refer [->distinct-vec get-esitysnimi]]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.general :refer [ammatillinen? amm-tutkinnon-osa? julkaistu? not-arkistoitu? luonnos? asiasana->lng-value-map]]
            [kouta-indeksoija-service.indexer.tools.search :as search-tool]))

(def index-name "oppilaitos-kouta-search")

(defn- tarjoaja-organisaatiot
  [oppilaitos tarjoajat]
  (vec (map #(organisaatio-tool/find-from-organisaatio-and-children oppilaitos %) tarjoajat)))

(defn- tutkintonimikkeet-for-osaamisala
  [osaamisalaKoodiUri]
  (list-alakoodi-nimet-with-cache osaamisalaKoodiUri "tutkintonimikkeet"))

(defn- tutkintonimikkeet-for-toteutus
  [toteutus]
  ;TODO -> eperusteet
  (if (ammatillinen? toteutus)
    (->> (get-in toteutus [:metadata :osaamisalat :koodiUri])
         (mapcat tutkintonimikkeet-for-osaamisala)
         (->distinct-vec))
    []))

(defn oppilaitos-hit
  [oppilaitos]
  (search-tool/hit :koulutustyypit  (vector (search-tool/koulutustyyppi-for-organisaatio oppilaitos))
                   :opetuskieliUrit (:kieletUris oppilaitos)
                   :tarjoajat       (vector oppilaitos)
                   :oppilaitos      oppilaitos
                   :logo            (:logo oppilaitos)))

(defn koulutus-hit
  [oppilaitos koulutus]
  (search-tool/hit :koulutustyypit     (search-tool/deduce-koulutustyypit koulutus)
                   :opetuskieliUrit    (:kieletUris oppilaitos)
                   :tarjoajat          (tarjoaja-organisaatiot oppilaitos (:tarjoajat koulutus))
                   :tarjoajaOids       (:tarjoajat koulutus)
                   :oppilaitos         oppilaitos
                   :koulutusalaUrit    (search-tool/koulutusala-koodi-urit koulutus)
                   :tutkintonimikeUrit (search-tool/tutkintonimike-koodi-urit koulutus)
                   :nimet              (vector (:nimi koulutus))
                   :koulutusOid        (:oid koulutus)
                   :kuva               (:teemakuva koulutus)
                   :onkoTuleva         true
                   :nimi               (:nimi koulutus)
                   :metadata           (cond-> {:tutkintonimikkeetKoodiUrit      (search-tool/tutkintonimike-koodi-urit koulutus)
                                                :opintojenLaajuusKoodiUri        (search-tool/opintojen-laajuus-koodi-uri koulutus)
                                                :opintojenLaajuusyksikkoKoodiUri (search-tool/opintojen-laajuusyksikko-koodi-uri koulutus)
                                                :opintojenLaajuusNumero          (search-tool/opintojen-laajuus-numero koulutus)
                                                :koulutustyypitKoodiUrit         (search-tool/koulutustyyppi-koodi-urit koulutus)
                                                :koulutustyyppi                  (:koulutustyyppi koulutus)}
                                         (amm-tutkinnon-osa? koulutus) (assoc :tutkinnonOsat (search-tool/tutkinnon-osat koulutus)))))

(defn toteutus-hit
  [oppilaitos koulutus hakutiedot toteutus]
  (let [hakutieto (search-tool/get-toteutuksen-julkaistut-hakutiedot hakutiedot toteutus)
        toteutus-metadata (:metadata toteutus)
        opetus (get-in toteutus [:metadata :opetus])]
    (search-tool/hit :koulutustyypit            (search-tool/deduce-koulutustyypit koulutus toteutus-metadata)
                     :opetuskieliUrit           (get-in toteutus [:metadata :opetus :opetuskieliKoodiUrit])
                     :tarjoajat                 (tarjoaja-organisaatiot oppilaitos (:tarjoajat toteutus))
                     :tarjoajaOids              (:tarjoajat toteutus)
                     :oppilaitos                oppilaitos
                     :koulutusalaUrit           (search-tool/koulutusala-koodi-urit koulutus)
                     :tutkintonimikeUrit        (search-tool/tutkintonimike-koodi-urit koulutus)
                     :opetustapaUrit            (or (some-> toteutus :metadata :opetus :opetustapaKoodiUrit) [])
                     :nimet                     (vector (:nimi koulutus) (get-esitysnimi toteutus))
                     :asiasanat                 (asiasana->lng-value-map (get-in toteutus [:metadata :asiasanat]))
                     :ammattinimikkeet          (asiasana->lng-value-map (get-in toteutus [:metadata :ammattinimikkeet]))
                     :koulutusOid               (:oid koulutus)
                     :toteutusOid               (:oid toteutus)
                     :nimi                      (get-esitysnimi toteutus)
                     :hakutiedot                (get-search-hakutiedot hakutieto)
                     :pohjakoulutusvaatimusUrit (search-tool/pohjakoulutusvaatimus-koodi-urit hakutieto)
                     :kuva                      (:teemakuva toteutus)
                     :onkoTuleva                false
                     :metadata                  {:tutkintonimikkeet  (tutkintonimikkeet-for-toteutus toteutus)
                                                 :opetusajatKoodiUrit (:opetusaikaKoodiUrit opetus)
                                                 :maksullisuustyyppi  (:maksullisuustyyppi opetus)
                                                 :maksunMaara         (:maksunMaara opetus)
                                                 :koulutustyyppi      (:koulutustyyppi koulutus)})))

(defn oppilaitos-search-terms
  [oppilaitos]
  (search-tool/search-terms :tarjoajat (vector oppilaitos)
                            :oppilaitos oppilaitos
                            :opetuskieliUrit (:kieletUris oppilaitos)
                            :koulutustyypit (vector (search-tool/koulutustyyppi-for-organisaatio oppilaitos))
                            :kuva (:logo oppilaitos)
                            :nimi (:nimi oppilaitos)))

(defn koulutus-search-terms
  [oppilaitos koulutus]
  (search-tool/search-terms :koulutus koulutus
                            :tarjoajat (tarjoaja-organisaatiot oppilaitos (:tarjoajat koulutus))
                            :oppilaitos oppilaitos
                            :opetuskieliUrit (:kieletUris oppilaitos)
                            :koulutustyypit (search-tool/deduce-koulutustyypit koulutus)
                            :kuva (:teemakuva koulutus)
                            :nimi (:nimi koulutus)
                            :onkoTuleva true
                            :metadata (cond-> {:tutkintonimikkeetKoodiUrit      (search-tool/tutkintonimike-koodi-urit koulutus)
                                               :opintojenLaajuusKoodiUri        (search-tool/opintojen-laajuus-koodi-uri koulutus)
                                               :opintojenLaajuusyksikkoKoodiUri (search-tool/opintojen-laajuusyksikko-koodi-uri koulutus)
                                               :opintojenLaajuusNumero          (search-tool/opintojen-laajuus-numero koulutus)
                                               :koulutustyypitKoodiUrit         (search-tool/koulutustyyppi-koodi-urit koulutus)
                                               :koulutustyyppi                  (:koulutustyyppi koulutus)}
                                              (amm-tutkinnon-osa? koulutus) (assoc :tutkinnonOsat (search-tool/tutkinnon-osat koulutus)))))

(defn toteutus-search-terms
  [oppilaitos koulutus hakutiedot toteutus]
  (let [hakutieto (search-tool/get-toteutuksen-julkaistut-hakutiedot hakutiedot toteutus)
        toteutus-metadata (:metadata toteutus)
        tarjoajat (tarjoaja-organisaatiot oppilaitos (:tarjoajat toteutus))
        opetus (get-in toteutus [:metadata :opetus])]
    (search-tool/search-terms :koulutus koulutus
                              :toteutus toteutus
                              :tarjoajat tarjoajat
                              :oppilaitos oppilaitos
                              :hakutiedot (get-search-hakutiedot hakutieto)
                              :toteutus-organisaationimi (remove nil? (distinct (map :nimi tarjoajat)))
                              :opetuskieliUrit (get-in toteutus [:metadata :opetus :opetuskieliKoodiUrit])
                              :koulutustyypit (search-tool/deduce-koulutustyypit koulutus toteutus-metadata)
                              :kuva (:teemakuva toteutus)
                              :nimi (get-esitysnimi toteutus)
                              :onkoTuleva false
                              :metadata {:tutkintonimikkeet   (tutkintonimikkeet-for-toteutus toteutus)
                                         :opetusajatKoodiUrit (:opetusaikaKoodiUrit opetus)
                                         :maksullisuustyyppi  (:maksullisuustyyppi opetus)
                                         :maksunMaara         (:maksunMaara opetus)
                                         :koulutustyyppi      (:koulutustyyppi koulutus)})))

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
  (let [paikkakuntaKoodiUrit (vec (distinct (filter #(string/starts-with? % "kunta") (mapcat :sijainti (:hits entry)))))]
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
  (when-let [all-visible-toteutukset (filter not-arkistoitu? (kouta-backend/get-toteutus-list-for-koulutus (:oid koulutus)))]
    (if-let [julkaistut-toteutukset (seq (get-tarjoaja-entries hierarkia (filter julkaistu? all-visible-toteutukset)))]
      (let [hakutiedot (kouta-backend/get-hakutiedot-for-koulutus (:oid koulutus))]
        (vec (map #(toteutus-hit oppilaitos koulutus hakutiedot %) julkaistut-toteutukset)))
      (when (not-empty (seq (get-tarjoaja-entries hierarkia (filter luonnos? all-visible-toteutukset))))
        (vector (koulutus-hit oppilaitos koulutus))))))

(defn- create-koulutus-search-terms
  [oppilaitos hierarkia koulutus]
  (when-let [all-visible-toteutukset (filter not-arkistoitu? (kouta-backend/get-toteutus-list-for-koulutus (:oid koulutus)))]
    (if-let [julkaistut-toteutukset (seq (get-tarjoaja-entries hierarkia (filter julkaistu? all-visible-toteutukset)))]
      (let [hakutiedot (kouta-backend/get-hakutiedot-for-koulutus (:oid koulutus))]
        (vec (map #(toteutus-search-terms oppilaitos koulutus hakutiedot %) julkaistut-toteutukset)))
      (when (not-empty (seq (get-tarjoaja-entries hierarkia (filter luonnos? all-visible-toteutukset))))
        (vector (koulutus-search-terms oppilaitos koulutus))))))

(defn- create-oppilaitos-entry-with-hits
  [oppilaitos hierarkia koulutukset]
  (let [koulutus-hits (partial create-koulutus-hits oppilaitos hierarkia)
        koulutus-search-terms (partial create-koulutus-search-terms oppilaitos hierarkia)]
    (-> oppilaitos
        (create-base-entry koulutukset)
        (assoc :hits (if (seq koulutukset)
                       (vec (mapcat #(koulutus-hits %) koulutukset))
                       (vector (oppilaitos-hit oppilaitos))))
        (assoc :search_terms (if (seq koulutukset)
                               (vec (mapcat #(koulutus-search-terms %) koulutukset))
                               (vector (oppilaitos-search-terms oppilaitos))))
        (assoc-paikkakunnat))))

(defn create-index-entry
  [oid]
  (let [hierarkia (cache/get-hierarkia oid)]
    (when-let [oppilaitos-oid (:oid (organisaatio-tool/find-oppilaitos-from-hierarkia hierarkia))]
      (let [oppilaitos (organisaatio-client/get-hierarkia-for-oid-without-parents oppilaitos-oid)
            koulutukset (delay
                          (get-tarjoaja-entries hierarkia
                                                (kouta-backend/get-koulutukset-by-tarjoaja oppilaitos-oid)))]
        (if (and (organisaatio-tool/indexable? oppilaitos) (seq @koulutukset))
          (indexable/->index-entry
            (:oid oppilaitos) (create-oppilaitos-entry-with-hits oppilaitos hierarkia @koulutukset))
          (indexable/->delete-entry (:oid oppilaitos)))))))

(defn do-index
  [oids execution-id]
  (indexable/do-index index-name oids create-index-entry execution-id))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
