(ns kouta-indeksoija-service.indexer.kouta.oppilaitos-search
  (:require [clojure.string :as string]
            [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache list-alakoodi-nimet-with-cache]]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.util.tools :refer [->distinct-vec get-esitysnimi]]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.kouta.common :refer [create-sort-names]]
            [kouta-indeksoija-service.indexer.tools.general :refer [ammatillinen? amm-tutkinnon-osa? julkaistu? not-arkistoitu? luonnos?]]
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
                                               :opintojenLaajuusNumeroMin       (search-tool/opintojen-laajuus-numero-min koulutus)
                                               :opintojenLaajuusNumeroMax       (search-tool/opintojen-laajuus-numero-max koulutus)
                                               :koulutustyypitKoodiUrit         (search-tool/koulutustyyppi-koodi-urit koulutus)
                                               :koulutustyyppi                  (:koulutustyyppi koulutus)}
                                              (amm-tutkinnon-osa? koulutus) (assoc :tutkinnonOsat (search-tool/tutkinnon-osat koulutus)))))

(defn toteutus-search-terms
  [oppilaitos koulutus hakutiedot toteutus]
  (let [hakutieto (search-tool/get-toteutuksen-julkaistut-hakutiedot hakutiedot toteutus)
        toteutus-metadata (:metadata toteutus)
        tarjoajat (tarjoaja-organisaatiot oppilaitos (:tarjoajat toteutus))
        opetus (get-in toteutus [:metadata :opetus])
        jarjestaa-urheilijan-amm-koulutusta (search-tool/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
                                              (:tarjoajat toteutus)
                                              (:haut hakutieto))]
    (search-tool/search-terms :koulutus koulutus
                              :toteutus toteutus
                              :tarjoajat tarjoajat
                              :jarjestaa-urheilijan-amm-koulutusta jarjestaa-urheilijan-amm-koulutusta
                              :oppilaitos oppilaitos
                              :hakutiedot hakutieto
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
                                         :suunniteltuKestoKuukausina (search-tool/kesto-kuukausina opetus)
                                         :koulutustyyppi      (:koulutustyyppi koulutus)})))

(defn- get-kouta-oppilaitos
  [oid execution-id]
  (let [oppilaitos (kouta-backend/get-oppilaitos-with-cache oid execution-id)]
    (when (julkaistu? oppilaitos)
      {:kielivalinta (:kielivalinta oppilaitos)
       :kuvaus       (get-in oppilaitos [:metadata :esittely])
       :logo         (:logo oppilaitos)})))

(defn- create-base-entry
  [oppilaitos koulutukset execution-id]
  (let [kaikki (count koulutukset)
        tutkintoonJohtavat (count (filter :johtaaTutkintoon koulutukset))]
  (-> oppilaitos
      (select-keys [:oid :nimi])
      (merge (get-kouta-oppilaitos (:oid oppilaitos) execution-id))
      (assoc :nimi_sort (create-sort-names (:nimi oppilaitos)))
      (assoc :koulutusohjelmatLkm {
                                   :kaikki kaikki
                                   :tutkintoonJohtavat tutkintoonJohtavat
                                   :eiTutkintoonJohtavat (- kaikki tutkintoonJohtavat)}))))

(defn- assoc-paikkakunnat
  [entry]
  (let [paikkakuntaKoodiUrit (vec (distinct (filter #(string/starts-with? % "kunta") (mapcat :sijainti (:search_terms entry)))))]
    (assoc entry :paikkakunnat (vec (map get-koodi-nimi-with-cache paikkakuntaKoodiUrit)))))

(defn get-tarjoaja-entries
  [hierarkia entries]
  (->> (for [entry entries]
         (when-let [indexable-oids (seq (organisaatio-tool/filter-indexable-oids-for-hierarkia hierarkia (:tarjoajat entry)))]
           (assoc entry :tarjoajat indexable-oids)))
       (remove nil?)
       (vec)))

(defn- create-koulutus-search-terms
  [execution-id oppilaitos hierarkia koulutus]
  (when-let [all-visible-toteutukset (filter not-arkistoitu? (kouta-backend/get-toteutus-list-for-koulutus-with-cache (:oid koulutus) execution-id))]
    (if-let [julkaistut-toteutukset (seq (get-tarjoaja-entries hierarkia (filter julkaistu? all-visible-toteutukset)))]
      (let [hakutiedot (kouta-backend/get-hakutiedot-for-koulutus-with-cache (:oid koulutus) execution-id)]
        (vec (map #(toteutus-search-terms oppilaitos koulutus hakutiedot %) julkaistut-toteutukset)))
      (when (not-empty (seq (get-tarjoaja-entries hierarkia (filter luonnos? all-visible-toteutukset))))
        (vector (koulutus-search-terms oppilaitos koulutus))))))

(defn- create-oppilaitos-entry-with-hits
  [oppilaitos hierarkia koulutukset execution-id]
  (let [koulutus-search-terms (partial create-koulutus-search-terms execution-id oppilaitos hierarkia)]
    (-> oppilaitos
        (create-base-entry koulutukset execution-id)
        (assoc :search_terms (if (seq koulutukset)
                               (vec (mapcat #(koulutus-search-terms %) koulutukset))
                               (vector (oppilaitos-search-terms oppilaitos))))
        (assoc-paikkakunnat))))

(defn create-index-entry
  [oid execution-id]
  (when-let [oppilaitos (cache/find-oppilaitos-by-oid oid)]
    (let [oppilaitos-hierarkia (organisaatio-tool/attach-parent-to-oppilaitos-from-cache (cache/get-hierarkia-cached) oppilaitos)
          oppilaitos-oid (:oid oppilaitos)
          ;; jos toimipiste, haetaan koulutukset parentin oidilla
          oid (if (organisaatio-tool/toimipiste? oppilaitos)
                (:parentOid oppilaitos)
                (:oid oppilaitos))
          koulutukset (delay
                        (kouta-backend/get-koulutukset-by-tarjoaja-with-cache oid execution-id))]
      (if (and (organisaatio-tool/indexable? oppilaitos) (seq @koulutukset))
        (indexable/->index-entry
          oppilaitos-oid (create-oppilaitos-entry-with-hits oppilaitos oppilaitos-hierarkia @koulutukset execution-id))
        (indexable/->delete-entry oppilaitos-oid)))))

(defn do-index
  ([oids execution-id]
   (do-index oids execution-id true))
  ([oids execution-id clear-cache-before]
   (when (= true clear-cache-before)
     (cache/clear-all-cached-data))
    (let [oids-to-index (organisaatio-tool/resolve-organisaatio-oids-to-index (cache/get-hierarkia-cached) oids)]
      (indexable/do-index index-name oids-to-index create-index-entry execution-id))))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
