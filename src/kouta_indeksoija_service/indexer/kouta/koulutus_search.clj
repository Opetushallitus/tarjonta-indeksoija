(ns kouta-indeksoija-service.indexer.kouta.koulutus-search
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [remove-uri-version]]
            [kouta-indeksoija-service.indexer.tools.general :refer [asiasana->lng-value-map amm-tutkinnon-osa? amm-osaamisala? julkaistu?]]
            [kouta-indeksoija-service.indexer.tools.hakutieto :refer [get-search-hakutiedot]]
            [kouta-indeksoija-service.indexer.tools.search :as search-tool]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos :as oppilaitos]
            [kouta-indeksoija-service.util.tools :refer [->distinct-vec get-esitysnimi]]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]))

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
             (search-tool/hit :koulutustyypit            (search-tool/deduce-koulutustyypit koulutus toteutus-metadata)
                              :opetuskieliUrit           (:opetuskieliKoodiUrit opetus)
                              :tarjoajat                 (:tarjoajat toteutus)
                              :oppilaitos                oppilaitos
                              :oppilaitosOid             (:oid oppilaitos)
                              :koulutusalaUrit           (search-tool/koulutusala-koodi-urit koulutus)
                              :tutkintonimikeUrit        (search-tool/tutkintonimike-koodi-urit koulutus)
                              :opetustapaUrit            (or (some-> toteutus :metadata :opetus :opetustapaKoodiUrit) [])
                              :nimet                     (vector (:nimi koulutus) (get-esitysnimi toteutus))
                              :hakutiedot                (get-search-hakutiedot hakutieto)
                              :kuva                      (:logo oppilaitos)
                              :asiasanat                 (asiasana->lng-value-map (get-in toteutus [:metadata :asiasanat]))
                              :ammattinimikkeet          (asiasana->lng-value-map (get-in toteutus [:metadata :ammattinimikkeet]))
                              :toteutusOid               (:oid toteutus)
                              :toteutusNimi              (get-esitysnimi toteutus)
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

(defn- jarjestaja-search-terms
  [hierarkia koulutus toteutukset hakutiedot]
  (let [oppilaitos (get-oppilaitos hierarkia)]
    (for [toteutus (->> toteutukset
                        (map (fn [t] (->> (:tarjoajat t)
                                          (organisaatio-tool/filter-indexable-for-hierarkia hierarkia)
                                          (assoc t :tarjoajat))))
                        (filter #(seq (:tarjoajat %))))
          :let [hakutieto (search-tool/get-toteutuksen-julkaistut-hakutiedot hakutiedot toteutus)]
          :let [opetus (get-in toteutus [:metadata :opetus])]
          :let [toteutus-metadata (:metadata toteutus)]]
      (search-tool/search-terms
        :koulutus koulutus
        :toteutus toteutus
        :oppilaitos oppilaitos
        :tarjoajat (:tarjoajat toteutus)
        :hakutiedot (get-search-hakutiedot hakutieto)
        :toteutus-organisaationimi (remove nil? (distinct (map :nimi (flatten (:tarjoajat toteutus)))))
        :opetuskieliUrit (:opetuskieliKoodiUrit opetus)
        :koulutustyypit (search-tool/deduce-koulutustyypit koulutus toteutus-metadata)
        :kuva (:logo oppilaitos)
        :nimi (:nimi oppilaitos)
        :onkoTuleva false
        :metadata {:tutkintonimikkeetKoodiUrit                 (search-tool/tutkintonimike-koodi-urit koulutus)
                   :opetusajatKoodiUrit                        (:opetusaikaKoodiUrit opetus)
                   :maksullisuustyyppi                         (:maksullisuustyyppi opetus)
                   :maksunMaara                                (:maksunMaara opetus)
                   :koulutustyyppi                             (:tyyppi toteutus-metadata)
                   :oppilaitosTila                             (:tila oppilaitos)
                   :ammatillinenPerustutkintoErityisopetuksena (:ammatillinenPerustutkintoErityisopetuksena toteutus-metadata)
                   :jarjestetaanErityisopetuksena              (:jarjestetaanErityisopetuksena toteutus-metadata)}))))

(defn- tuleva-jarjestaja-search-terms
  [hierarkia koulutus]
  (let [tarjoajat (organisaatio-tool/filter-indexable-for-hierarkia hierarkia (:tarjoajat koulutus))
        oppilaitos (get-oppilaitos hierarkia)]
    (search-tool/search-terms
      :koulutus koulutus
      :tarjoajat tarjoajat
      :oppilaitos oppilaitos
      :toteutus-organisaationimi (remove nil? (distinct (map :nimi (flatten tarjoajat))))
      :koulutustyypit (search-tool/deduce-koulutustyypit koulutus)
      :kuva (:logo oppilaitos)
      :nimi (:nimi oppilaitos)
      :onkoTuleva true
      :metadata {:oppilaitosTila (:tila oppilaitos)
                 :koulutustyyppi (search-tool/koulutustyyppi-for-organisaatio oppilaitos)})))

(defn assoc-jarjestaja-hits
  [koulutus toteutukset hakutiedot]
  (if (seq (:tarjoajat koulutus))
    (let [hits (for [hierarkia (map cache/get-hierarkia (find-indexable-oppilaitos-oids (:tarjoajat koulutus)))]
                 (if (tuleva-jarjestaja? hierarkia toteutukset)
                   {:hits [(tuleva-jarjestaja-hit hierarkia koulutus)]}
                   {:hits (jarjestaja-hits hierarkia koulutus toteutukset hakutiedot)}))]
      (->> hits
           (apply merge-with concat)
           (merge koulutus)))
    (assoc koulutus :hits [(tuleva-jarjestaja-hit {} koulutus)])))

(defn assoc-jarjestaja-search-terms
  [koulutus toteutukset hakutiedot]
  (if (seq (:tarjoajat koulutus))
    (let [search-terms (for [hierarkia (map cache/get-hierarkia (find-indexable-oppilaitos-oids (:tarjoajat koulutus)))]
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
                  (assoc :search_terms (:search_terms koulutus))
                  (common/localize-dates))]
    (cond-> entry
      (amm-tutkinnon-osa? koulutus) (assoc :tutkinnonOsat (-> koulutus (search-tool/tutkinnon-osat) (common/decorate-koodi-uris)))
      (amm-osaamisala? koulutus)    (merge (common/decorate-koodi-uris {:osaamisalaKoodiUri (-> koulutus (search-tool/osaamisala-koodi-uri))})))))

(defn- assoc-toteutusten-tarjoajat [koulutus toteutukset]
  (let [tarjoaja-oids (distinct (mapcat (fn [t] (:tarjoajat t)) toteutukset))
        tarjoaja-count (count tarjoaja-oids)]
    (assoc koulutus :toteutustenTarjoajat {:count tarjoaja-count
                                           :nimi (when (= tarjoaja-count 1)
                                                   (let [oid (first tarjoaja-oids)
                                                         hierarkia (cache/get-hierarkia oid)
                                                         org (organisaatio-tool/find-from-hierarkia hierarkia oid)]
                                                     (get-in org [:nimi])))})))

(defn create-index-entry
  [oid]
  (let [koulutus (kouta-backend/get-koulutus oid)]
    (if (julkaistu? koulutus)
      (let [toteutukset (seq (kouta-backend/get-toteutus-list-for-koulutus (:oid koulutus) true))
            hakutiedot (when toteutukset (kouta-backend/get-hakutiedot-for-koulutus (:oid koulutus)))]
        (indexable/->index-entry oid (-> koulutus
                                         (assoc-jarjestaja-hits toteutukset hakutiedot)
                                         (assoc-jarjestaja-search-terms toteutukset hakutiedot)
                                         (assoc-toteutusten-tarjoajat toteutukset)
                                         (create-entry))))
      (indexable/->delete-entry oid))))

(defn do-index
  [oids execution-id]
  (indexable/do-index index-name oids create-index-entry execution-id))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
