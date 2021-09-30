(ns kouta-indeksoija-service.indexer.kouta.koulutus-search
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [remove-uri-version]]
            [kouta-indeksoija-service.indexer.tools.general :refer [asiasana->lng-value-map amm-tutkinnon-osa? amm-osaamisala? julkaistu?]]
            [kouta-indeksoija-service.indexer.tools.hakutieto :refer [get-search-hakutiedot]]
            [kouta-indeksoija-service.indexer.tools.search :as search-tool]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos :as oppilaitos]
            [kouta-indeksoija-service.util.tools :refer [->distinct-vec]]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [clojure.walk :as walk]))

(def index-name "koulutus-kouta-search")


(defn- get-logo-and-tila
  [oid]
  (when oid
    (:oppilaitos (oppilaitos/get-from-index oid :_source "oppilaitos.logo,oppilaitos.tila"))))

(defn- get-oppilaitos
  [hierarkia]
  (when-let [oppilaitos (organisaatio-tool/find-oppilaitos-from-hierarkia hierarkia)]
    (let [{:keys [tila logo]} (get-logo-and-tila (:oid oppilaitos))]
      (cond-> (assoc oppilaitos :tila tila)
        (julkaistu? {:tila tila}) (assoc :logo logo)))))

(defn tuleva-jarjestaja-hit
  [hierarkia koulutus]
  (let [oppilaitos (get-oppilaitos hierarkia)
        tarjoajat  (organisaatio-tool/filter-indexable-for-hierarkia hierarkia (:tarjoajat koulutus))]
    (search-tool/hit :koulutustyypit     (search-tool/deduce-koulutustyypit koulutus)
                     :tarjoajat          tarjoajat
                     :oppilaitos         oppilaitos
                     :koulutusalaUrit    (search-tool/koulutusala-koodi-urit koulutus)
                     :tutkintonimikeUrit (search-tool/tutkintonimike-koodi-urit koulutus)
                     :nimet              (vector (:nimi koulutus))
                     :kuva               (:logo oppilaitos)
                     :oppilaitosOid      (:oid oppilaitos)
                     :onkoTuleva         true
                     :nimi               (:nimi oppilaitos)
                     :metadata           {:oppilaitosTila (:tila oppilaitos)
                                          :koulutustyyppi (search-tool/koulutustyyppi-for-organisaatio oppilaitos)})))

(defn jarjestaja-hits
  [hierarkia koulutus toteutukset hakutiedot]
    (let [oppilaitos (get-oppilaitos hierarkia)]
      (vec (for [toteutus (->> toteutukset
                               (map (fn [t] (->> (:tarjoajat t)
                                                 (organisaatio-tool/filter-indexable-for-hierarkia hierarkia)
                                                 (assoc t :tarjoajat))))
                               (filter #(seq (:tarjoajat %))))
                 :let [hakutieto (search-tool/get-toteutuksen-julkaistut-hakutiedot hakutiedot toteutus)]
                 :let [toteutus-metadata (:metadata toteutus)]
                 :let [opetus (get-in toteutus [:metadata :opetus])]]
             (search-tool/hit :koulutustyypit            (search-tool/deduce-koulutustyypit koulutus (:ammatillinenPerustutkintoErityisopetuksena toteutus-metadata))
                              :opetuskieliUrit           (:opetuskieliKoodiUrit opetus)
                              :tarjoajat                 (:tarjoajat toteutus)
                              :oppilaitos                oppilaitos
                              :oppilaitosOid             (:oid oppilaitos)
                              :koulutusalaUrit           (search-tool/koulutusala-koodi-urit koulutus)
                              :tutkintonimikeUrit        (search-tool/tutkintonimike-koodi-urit koulutus)
                              :opetustapaUrit            (or (some-> toteutus :metadata :opetus :opetustapaKoodiUrit) [])
                              :nimet                     (vector (:nimi koulutus) (:nimi toteutus))
                              :hakutiedot                (get-search-hakutiedot hakutieto)
                              :kuva                      (:logo oppilaitos)
                              :asiasanat                 (asiasana->lng-value-map (get-in toteutus [:metadata :asiasanat]))
                              :ammattinimikkeet          (asiasana->lng-value-map (get-in toteutus [:metadata :ammattinimikkeet]))
                              :toteutusOid               (:oid toteutus)
                              :toteutusNimi              (:nimi toteutus)
                              :onkoTuleva                false
                              :nimi                      (:nimi oppilaitos)
                              :metadata                  {:tutkintonimikkeetKoodiUrit (search-tool/tutkintonimike-koodi-urit koulutus)
                                                          :opetusajatKoodiUrit        (:opetusaikaKoodiUrit opetus)
                                                          :maksullisuustyyppi         (:maksullisuustyyppi opetus)
                                                          :maksunMaara                (:maksunMaara opetus)
                                                          :koulutustyyppi             (:tyyppi toteutus-metadata)
                                                          :oppilaitosTila             (:tila oppilaitos)
                                                          :ammatillinenPerustutkintoErityisopetuksena (:ammatillinenPerustutkintoErityisopetuksena toteutus-metadata)
                                                          :jarjestetaanErityisopetuksena      (:jarjestetaanErityisopetuksena toteutus-metadata)})))))

(defn tuleva-jarjestaja?
  [hierarkia toteutukset]
  (-> (organisaatio-tool/filter-indexable-for-hierarkia hierarkia (mapcat :tarjoajat toteutukset))
      (->distinct-vec)
      (empty?)))

(defn find-indexable-oppilaitos-oids
  [tarjoajat]
  (->> tarjoajat
       (map cache/get-hierarkia)
       (map organisaatio-tool/find-oppilaitos-from-hierarkia)
       (remove nil?)
       (filter organisaatio-tool/indexable?)
       (map :oid)
       (->distinct-vec)))

(defn- remove-nils-from-search-terms
  [search-terms]
  (walk/postwalk
    (fn [x]
      (if (map? x)
        (not-empty (into {} (remove (comp nil? second)) x))
        x))
    search-terms))

(defn- get-lang-values
  [lang values]
  (distinct (remove nil? (map #(lang %) values))))

(defn- jarjestaja-search-terms
  [hierarkia koulutus toteutukset hakutiedot]
  (for [toteutus (->> toteutukset
                      (map (fn [t] (->> (:tarjoajat t)
                                        (organisaatio-tool/filter-indexable-for-hierarkia hierarkia)
                                        (assoc t :tarjoajat))))
                      (filter #(seq (:tarjoajat %))))]
    (let [tarjoajat (:tarjoajat toteutus)
          opetus (get-in toteutus [:metadata :opetus])
          hakutieto (search-tool/get-toteutuksen-julkaistut-hakutiedot hakutiedot toteutus)
          koulutusnimi (:nimi koulutus)
          toteutusnimi (:nimi toteutus)
          koulutus-organisaationimi (:nimi (get-oppilaitos hierarkia))
          toteutus-organisaationimi (remove nil? (distinct (map :nimi (flatten tarjoajat))))
          asiasanat (flatten (get-in toteutus [:metadata :asiasanat]))
          tutkintonimikkeet (map #(-> % get-koodi-nimi-with-cache :nimi) (search-tool/tutkintonimike-koodi-urit koulutus))
          ammattinimikkeet (asiasana->lng-value-map (get-in toteutus [:metadata :ammattinimikkeet]))
          kunnat (remove nil? (distinct (map :kotipaikkaUri (flatten tarjoajat))))
          maakunnat (remove nil? (distinct (map #(:koodiUri (koodisto/maakunta %)) kunnat)))]

      (remove-nils-from-search-terms
        {:koulutusnimi              {:fi (:fi koulutusnimi)
                                     :sv (:sv koulutusnimi)
                                     :en (:en koulutusnimi)}
         :toteutusnimi              {:fi (:fi toteutusnimi)
                                     :sv (:sv toteutusnimi)
                                     :en (:en toteutusnimi)}
         :koulutus_organisaationimi {:fi (:fi koulutus-organisaationimi)
                                     :sv (:sv koulutus-organisaationimi)
                                     :en (:en koulutus-organisaationimi)}
         :toteutus_organisaationimi {:fi (not-empty (get-lang-values :fi toteutus-organisaationimi))
                                     :sv (not-empty (get-lang-values :sv toteutus-organisaationimi))
                                     :en (not-empty (get-lang-values :en toteutus-organisaationimi))}
         :asiasanat                 {:fi (not-empty (distinct (map #(get % :arvo) (filter #(= (:kieli %) "fi") asiasanat))))
                                     :sv (not-empty (distinct (map #(get % :arvo) (filter #(= (:kieli %) "sv") asiasanat))))
                                     :en (not-empty (distinct (map #(get % :arvo) (filter #(= (:kieli %) "en") asiasanat))))}
         :tutkintonimikkeet         {:fi (not-empty (get-lang-values :fi tutkintonimikkeet))
                                     :sv (not-empty (get-lang-values :sv tutkintonimikkeet))
                                     :en (not-empty (get-lang-values :en tutkintonimikkeet))}
         :ammattinimikkeet          {:fi (not-empty (get-lang-values :fi ammattinimikkeet))
                                     :sv (not-empty (get-lang-values :sv ammattinimikkeet))
                                     :en (not-empty (get-lang-values :en ammattinimikkeet))}
         :sijainti                  (search-tool/clean-uris (concat kunnat maakunnat))
         :koulutusalat              (search-tool/clean-uris (search-tool/koulutusala-koodi-urit koulutus))
         :hakutiedot                (map #(-> %
                                              (update :hakutapa remove-uri-version)
                                              (update :valintatavat search-tool/clean-uris)
                                              (update :pohjakoulutusvaatimukset search-tool/clean-uris))
                                         (get-search-hakutiedot hakutieto))
         :opetustavat               (search-tool/clean-uris (or (some-> toteutus :metadata :opetus :opetustapaKoodiUrit) []))
         :opetuskielet              (search-tool/clean-uris (:opetuskieliKoodiUrit opetus))
         :koulutustyypit            (search-tool/clean-uris (search-tool/deduce-koulutustyypit koulutus (:ammatillinenPerustutkintoErityisopetuksena (:metadata toteutus))))
         }))))

(defn- tuleva-jarjestaja-search-terms
  [hierarkia koulutus]
  (let [tarjoajat  (organisaatio-tool/filter-indexable-for-hierarkia hierarkia (:tarjoajat koulutus))
        koulutusnimi (:nimi koulutus)
        koulutus-organisaationimi (:nimi (get-oppilaitos hierarkia))
        toteutus-organisaationimi (remove nil? (distinct (map :nimi (flatten tarjoajat))))
        tutkintonimikkeet (map #(-> % get-koodi-nimi-with-cache :nimi) (search-tool/tutkintonimike-koodi-urit koulutus))
        kunnat (remove nil? (distinct (map :kotipaikkaUri (flatten tarjoajat))))
        maakunnat (remove nil? (distinct (map #(:koodiUri (koodisto/maakunta %)) kunnat)))]

    (remove-nils-from-search-terms
      {:koulutusnimi              {:fi (:fi koulutusnimi)
                                   :sv (:sv koulutusnimi)
                                   :en (:en koulutusnimi)}
       :koulutus_organisaationimi {:fi (:fi koulutus-organisaationimi)
                                   :sv (:sv koulutus-organisaationimi)
                                   :en (:en koulutus-organisaationimi)}
       :toteutus_organisaationimi {:fi (not-empty (get-lang-values :fi toteutus-organisaationimi))
                                   :sv (not-empty (get-lang-values :sv toteutus-organisaationimi))
                                   :en (not-empty (get-lang-values :en toteutus-organisaationimi))}
       :tutkintonimikkeet         {:fi (not-empty (get-lang-values :fi tutkintonimikkeet))
                                   :sv (not-empty (get-lang-values :sv tutkintonimikkeet))
                                   :en (not-empty (get-lang-values :en tutkintonimikkeet))}
       :sijainti                  (search-tool/clean-uris (concat kunnat maakunnat))
       :koulutusalat              (search-tool/clean-uris (search-tool/koulutusala-koodi-urit koulutus))})))

(defn assoc-jarjestaja-hits
  [koulutus]
  (if (seq (:tarjoajat koulutus))
    (let [toteutukset (seq (kouta-backend/get-toteutus-list-for-koulutus (:oid koulutus) true))
          hakutiedot (when toteutukset (kouta-backend/get-hakutiedot-for-koulutus (:oid koulutus)))
          hits (for [hierarkia (map cache/get-hierarkia (find-indexable-oppilaitos-oids (:tarjoajat koulutus)))]
                 (if (tuleva-jarjestaja? hierarkia toteutukset)
                   {:hits [(tuleva-jarjestaja-hit hierarkia koulutus)]}
                   {:hits (jarjestaja-hits hierarkia koulutus toteutukset hakutiedot)}))]
      (->> hits
           (apply merge-with concat)
           (merge koulutus)))
    (assoc koulutus :hits [(tuleva-jarjestaja-hit {} koulutus)])))

(defn assoc-jarjestaja-search-terms
  [koulutus]
  (if (seq (:tarjoajat koulutus))
    (let [toteutukset (seq (kouta-backend/get-toteutus-list-for-koulutus (:oid koulutus) true))
          hakutiedot (when toteutukset (kouta-backend/get-hakutiedot-for-koulutus (:oid koulutus)))
          search-terms (for [hierarkia (map cache/get-hierarkia (find-indexable-oppilaitos-oids (:tarjoajat koulutus)))]
                         (if (tuleva-jarjestaja? hierarkia toteutukset)
                           {:search_terms [(tuleva-jarjestaja-search-terms hierarkia koulutus)]}
                           {:search_terms (jarjestaja-search-terms hierarkia koulutus toteutukset hakutiedot)}))]
      (->> search-terms
           (apply merge-with concat)
           (merge koulutus)))
    (assoc koulutus :search_terms [(tuleva-jarjestaja-search-terms {} koulutus)])))


(defn- create-entry
  [koulutus]
  (let [entry (-> koulutus
                  (assoc :oid (:oid koulutus))
                  (assoc :nimi (:nimi koulutus))
                  (assoc :kielivalinta (:kielivalinta koulutus))
                  (dissoc :johtaaTutkintoon :esikatselu :modified :muokkaaja :externalId :julkinen :tila :metadata :tarjoajat :sorakuvausId :organisaatioOid :ePerusteId)
                  (assoc :eperuste                (:ePerusteId koulutus))
                  (assoc :koulutukset             (:koulutuksetKoodiUri koulutus))
                  (assoc :tutkintonimikkeet       (search-tool/tutkintonimike-koodi-urit koulutus))
                  (assoc :kuvaus                  (get-in koulutus [:metadata :kuvaus]))
                  (assoc :teemakuva               (:teemakuva koulutus))
                  (assoc :koulutustyyppi          (:koulutustyyppi koulutus))
                  (assoc :opintojenLaajuus        (search-tool/opintojen-laajuus-koodi-uri koulutus))
                  (assoc :opintojenLaajuusNumero  (search-tool/opintojen-laajuus-numero koulutus))
                  (assoc :opintojenLaajuusyksikko (search-tool/opintojen-laajuusyksikko-koodi-uri koulutus))
                  (common/decorate-koodi-uris)
                  (assoc :hits (:hits koulutus))
                  (assoc :search_terms (:search_terms koulutus)))]
    (cond-> entry
      (amm-tutkinnon-osa? koulutus) (assoc :tutkinnonOsat (-> koulutus (search-tool/tutkinnon-osat) (common/decorate-koodi-uris)))
      (amm-osaamisala? koulutus)    (merge (common/decorate-koodi-uris {:osaamisalaKoodiUri (-> koulutus (search-tool/osaamisala-koodi-uri))})))))

(defn create-index-entry
  [oid]
  (let [koulutus (kouta-backend/get-koulutus oid)]
    (if (julkaistu? koulutus)
      (indexable/->index-entry oid (-> koulutus
                                       (assoc-jarjestaja-hits)
                                       (assoc-jarjestaja-search-terms)
                                       (create-entry)))
      (indexable/->delete-entry oid))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
