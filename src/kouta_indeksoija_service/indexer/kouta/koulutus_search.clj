(ns kouta-indeksoija-service.indexer.kouta.koulutus-search
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.indexer.tools.general :refer [amm-tutkinnon-osa? amm-osaamisala? julkaistu?]]
            [kouta-indeksoija-service.indexer.tools.hakutieto :refer [get-search-hakutiedot]]
            [kouta-indeksoija-service.indexer.tools.search :as search-tool]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos :as oppilaitos]
            [kouta-indeksoija-service.util.tools :refer [->distinct-vec]]))

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
        :toteutusHakuaika (:hakuaika toteutus-metadata)
        :lukiopainotukset (remove nil? (distinct (map (fn [painotus] (:koodiUri painotus)) (:painotukset toteutus-metadata))))
        :lukiolinjat_er (remove nil? (distinct (map (fn [er_linja] (:koodiUri er_linja)) (:erityisetKoulutustehtavat toteutus-metadata))))
        :osaamisalat (remove nil? (distinct (map (fn [osaamisala] (:koodiUri osaamisala)) (:osaamisalat toteutus-metadata))))
        :hasJotpaRahoitus (:hasJotpaRahoitus toteutus-metadata)

        :metadata {:tutkintonimikkeetKoodiUrit                 (search-tool/tutkintonimike-koodi-urit koulutus)
                   :opetusajatKoodiUrit                        (:opetusaikaKoodiUrit opetus)
                   :maksullisuustyyppi                         (:maksullisuustyyppi opetus)
                   :maksunMaara                                (:maksunMaara opetus)
                   :koulutustyyppi                             (:tyyppi toteutus-metadata)
                   :oppilaitosTila                             (:tila oppilaitos)
                   :jarjestaaUrheilijanAmmKoulutusta           (search-tool/jarjestaako-toteutus-urheilijan-amm-koulutusta
                                                                 (:haut hakutieto))
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
                  (assoc :nimi_sort (common/create-sort-names (:nimi koulutus)))
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
                  (assoc :opintojenLaajuusNumeroMin (get-in koulutus [:metadata :opintojenLaajuusNumeroMin]))
                  (assoc :opintojenLaajuusNumeroMax (get-in koulutus [:metadata :opintojenLaajuusNumeroMax]))
                  (assoc :opintojenLaajuusyksikko (search-tool/opintojen-laajuusyksikko-koodi-uri koulutus))
                  (common/decorate-koodi-uris)
                  (assoc :search_terms (:search_terms koulutus))
                  (common/localize-dates))]
    (cond-> entry
      (amm-tutkinnon-osa? koulutus) (assoc :tutkinnonOsat (-> koulutus (search-tool/tutkinnon-osat) (common/decorate-koodi-uris)))
      (amm-osaamisala? koulutus)    (merge (common/decorate-koodi-uris {:osaamisalaKoodiUri (-> koulutus (search-tool/osaamisala-koodi-uri))})))))

(defn- assoc-toteutusten-tarjoajat
  [koulutus toteutukset]
  (let [tarjoajat (distinct (mapcat (fn [toteutus]
                                      (let [tarjoaja-oids (:tarjoajat toteutus)]
                                        (map (fn [tarjoaja-oid]
                                               (let [hierarkia (cache/get-hierarkia tarjoaja-oid)]
                                                 (organisaatio-tool/find-oppilaitos-from-hierarkia
                                                  hierarkia)))
                                             tarjoaja-oids)))
                                    toteutukset))]
    (assoc koulutus
           :toteutustenTarjoajat
           {:count (count tarjoajat)
            :nimi (when-let [tarjoaja (first tarjoajat)] (get-in tarjoaja [:nimi]))})))

(defn create-index-entry
  [oid execution-id]
  (let [koulutus (kouta-backend/get-koulutus-with-cache oid execution-id)]
    (if (julkaistu? koulutus)
      (let [toteutukset (seq (kouta-backend/get-toteutus-list-for-koulutus-with-cache (:oid koulutus) true execution-id))
            hakutiedot (when toteutukset (kouta-backend/get-hakutiedot-for-koulutus-with-cache (:oid koulutus) execution-id))]
        (indexable/->index-entry oid (-> koulutus
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
