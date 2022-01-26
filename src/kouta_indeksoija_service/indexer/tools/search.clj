(ns kouta-indeksoija-service.indexer.tools.search
  (:require [clojure.edn :as edn]
            [kouta-indeksoija-service.indexer.tools.general :refer [asiasana->lng-value-map amm-osaamisala? amm-tutkinnon-osa? any-ammatillinen? ammatillinen? korkeakoulutus? lukio? tuva? telma? julkaistu? vapaa-sivistystyo-opistovuosi? vapaa-sivistystyo-muu? get-non-korkeakoulu-koodi-uri set-hakukohde-tila-by-related-haku]]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto]
            [kouta-indeksoija-service.rest.koodisto :refer [extract-versio get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [remove-uri-version koodi-arvo oppilaitostyyppi-uri-to-tyyppi]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.util.tools :refer [->distinct-vec get-esitysnimi]]
            [kouta-indeksoija-service.indexer.cache.eperuste :refer [get-eperuste-by-koulutuskoodi get-eperuste-by-id filter-tutkinnon-osa]]
            [clojure.walk :as walk]))

(defonce amm-perustutkinto-erityisopetuksena-koulutustyyppi "koulutustyyppi_4")

(defn clean-uris
  [uris]
  (vec (map remove-uri-version uris)))

(defn hit
  [& {:keys [koulutustyypit
             opetuskieliUrit
             tarjoajat
             tarjoajaOids
             oppilaitos
             koulutusalaUrit
             nimet
             hakutiedot
             asiasanat
             ammattinimikkeet
             tutkintonimikeUrit
             opetustapaUrit
             oppilaitosOid
             koulutusOid
             toteutusOid
             toteutusNimi
             onkoTuleva
             nimi
             kuva
             lukiopainotukset
             lukiolinjaterityinenkoulutustehtava
             osaamisalat
             metadata]
      :or {koulutustyypit []
           opetuskieliUrit []
           tarjoajat []
           tarjoajaOids []
           oppilaitos []
           koulutusalaUrit []
           nimet []
           hakutiedot []
           asiasanat []
           ammattinimikkeet []
           tutkintonimikeUrit []
           opetustapaUrit []
           oppilaitosOid nil
           koulutusOid nil
           toteutusOid nil
           toteutusNimi {}
           onkoTuleva nil
           nimi {}
           kuva nil
           lukiopainotukset []
           lukiolinjaterityinenkoulutustehtava []
           osaamisalat []
           metadata {}}}]

  (let [tutkintonimikkeet (vec (map #(-> % get-koodi-nimi-with-cache :nimi) tutkintonimikeUrit))
        kunnat (remove nil? (distinct (map :kotipaikkaUri tarjoajat)))
        maakunnat (remove nil? (distinct (map #(:koodiUri (koodisto/maakunta %)) kunnat)))

        ;TODO: poistetaan terms myöhemmin
        terms (fn [lng-keyword] (distinct (remove nil? (concat (map lng-keyword nimet) ;HUOM! Älä tee tästä defniä, koska se ei enää ole thread safe!
                                                               (vector (-> oppilaitos :nimi lng-keyword))
                                                               (map #(-> % :nimi lng-keyword) tarjoajat)
                                                               (map lng-keyword asiasanat)
                                                               (map lng-keyword ammattinimikkeet)
                                                               (map lng-keyword tutkintonimikkeet)))))]

    (cond-> {;:koulutustyypit (clean-uris (concat (vector koulutustyyppi) koulutustyyppiUrit))
             :koulutustyypit (clean-uris koulutustyypit)
             :opetuskielet (clean-uris opetuskieliUrit)
             :sijainti (clean-uris (concat kunnat maakunnat))
             :hakutiedot (map #(-> %
                                   (update :hakutapa remove-uri-version)
                                   (update :valintatavat clean-uris)
                                   (update :pohjakoulutusvaatimukset clean-uris))
                              hakutiedot)
             :koulutusalat (clean-uris koulutusalaUrit)
             :opetustavat (clean-uris opetustapaUrit)
             :terms {:fi (terms :fi)
                     :sv (terms :sv)
                     :en (terms :en)}
             :metadata (common/decorate-koodi-uris (merge metadata {:kunnat kunnat}))
             :lukiopainotukset (clean-uris lukiopainotukset)
             :lukiolinjaterityinenkoulutustehtava (clean-uris lukiolinjaterityinenkoulutustehtava)
             :osaamisalat (clean-uris osaamisalat)}

      (not (nil? koulutusOid))    (assoc :koulutusOid koulutusOid)
      (not (nil? toteutusOid))    (assoc :toteutusOid toteutusOid)
      (not (empty? toteutusNimi)) (assoc :toteutusNimi toteutusNimi)
      (not (nil? oppilaitosOid))  (assoc :oppilaitosOid oppilaitosOid)
      (not (nil? kuva))           (assoc :kuva kuva)
      (not (nil? onkoTuleva))     (assoc :onkoTuleva onkoTuleva)
      (not (empty? tarjoajaOids)) (assoc :tarjoajat tarjoajaOids)
      (not (empty? nimi))         (assoc :nimi nimi))))

(defn- get-koulutusalatasot-by-koulutus-koodi-uri
  [koulutusKoodiUri]
  (vec (concat (map :koodiUri (koodisto/koulutusalat-taso1 koulutusKoodiUri))
               (map :koodiUri (koodisto/koulutusalat-taso2 koulutusKoodiUri)))))

(defn- get-koulutusalatasot-for-amm-tutkinnon-osat
  [koulutus]
  (when-let [koulutusKoodiUrit (->> (get-in koulutus [:metadata :tutkinnonOsat])
                                    (map #(some-> % :koulutusKoodiUri remove-uri-version))
                                    (->distinct-vec))]
    (->distinct-vec (mapcat get-koulutusalatasot-by-koulutus-koodi-uri koulutusKoodiUrit))))

(defn koulutusala-koodi-urit
  [koulutus]
  (if (any-ammatillinen? koulutus)
    (cond
      (amm-tutkinnon-osa? koulutus) (get-koulutusalatasot-for-amm-tutkinnon-osat koulutus)
      :default (get-koulutusalatasot-by-koulutus-koodi-uri (get-non-korkeakoulu-koodi-uri koulutus))))

  (if (any-ammatillinen? koulutus)
    (let [koulutusKoodiUri (get-non-korkeakoulu-koodi-uri koulutus)]
      (vec (concat (map :koodiUri (koodisto/koulutusalat-taso1 koulutusKoodiUri))
                   (map :koodiUri (koodisto/koulutusalat-taso2 koulutusKoodiUri)))))
    (get-in koulutus [:metadata :koulutusalaKoodiUrit])))

;TODO korvaa pelkällä get-eperuste-by-id, kun kaikki tuotantodata käyttää ePeruste id:tä
(defn- get-ammatillinen-eperuste
  [koulutus]
  (let [eperuste-id (:ePerusteId koulutus)]
    (if eperuste-id
      (get-eperuste-by-id eperuste-id)
      (get-eperuste-by-koulutuskoodi (get-non-korkeakoulu-koodi-uri koulutus)))))

(defn tutkintonimike-koodi-urit
  [koulutus]
  (cond (ammatillinen? koulutus) (when-let [eperuste (get-ammatillinen-eperuste koulutus)]
                                   (->distinct-vec (map :tutkintonimikeUri (:tutkintonimikkeet eperuste))))
        (lukio? koulutus) (vector koodisto/koodiuri-ylioppilas-tutkintonimike)
        :else (get-in koulutus [:metadata :tutkintonimikeKoodiUrit] [])))

(defn koulutustyyppi-koodi-urit
  [koulutus]
  (if (ammatillinen? koulutus)
    (vec (map :koodiUri (koodisto/koulutustyypit (get-non-korkeakoulu-koodi-uri koulutus))))
    []))

(defn- get-tutkinnon-osa-laajuudet
  [koulutus eperuste]
  (let [eperuste-tutkinnon-osat (:tutkinnonOsat eperuste)]
    (->> (for [tutkinnon-osa (some-> koulutus :metadata :tutkinnonOsat)
               :let [eperuste-osa (first (filter #(= (:id %) (:tutkinnonosaViite tutkinnon-osa)) eperuste-tutkinnon-osat))]]
           (some-> eperuste-osa :opintojenLaajuus :koodiUri))
         (vec))))

(defn- get-osaamisala
  [eperuste koulutus]
  (when-let [osaamisalaKoodiUri (some-> koulutus :metadata :osaamisalaKoodiUri remove-uri-version)]
    (some->> (:osaamisalat eperuste)
             (filter #(= osaamisalaKoodiUri (some-> % :koodiUri remove-uri-version)))
             (first))))

(defn opintojen-laajuus-koodi-uri
  [koulutus]
  (cond
    (ammatillinen? koulutus)   (-> koulutus (get-ammatillinen-eperuste) (get-in [:opintojenLaajuus :koodiUri]))
    (amm-osaamisala? koulutus) (-> koulutus (get-ammatillinen-eperuste) (get-osaamisala koulutus) (get-in [:opintojenLaajuus :koodiUri]))
    :default                   (get-in koulutus [:metadata :opintojenLaajuusKoodiUri])))

(defn number-or-nil
  [koodiarvo]
  (if (every? #(Character/isDigit %) koodiarvo)
    koodiarvo
    nil))

(defn opintojen-laajuus-numero
  [koulutus]
  (cond
    (ammatillinen? koulutus)   (-> koulutus (get-ammatillinen-eperuste) :opintojenLaajuusNumero)
    (amm-osaamisala? koulutus) (-> koulutus (get-ammatillinen-eperuste) (get-osaamisala koulutus) :opintojenLaajuusNumero)
    :default                   (edn/read-string (-> (get-in koulutus [:metadata :opintojenLaajuusKoodiUri]) koodi-arvo number-or-nil))))

(defn opintojen-laajuusyksikko-koodi-uri
  [koulutus]
  (cond
    (ammatillinen? koulutus)   (-> koulutus (get-ammatillinen-eperuste) (get-in [:opintojenLaajuusyksikko :koodiUri]))
    (amm-osaamisala? koulutus) (-> koulutus (get-ammatillinen-eperuste) (get-in [:opintojenLaajuusyksikko :koodiUri]))
    (korkeakoulutus? koulutus) koodisto/koodiuri-opintopiste-laajuusyksikko
    (lukio? koulutus) koodisto/koodiuri-opintopiste-laajuusyksikko
    (tuva? koulutus) koodisto/koodiuri-viikko-laajuusyksikko
    (telma? koulutus) koodisto/koodiuri-osaamispiste-laajuusyksikko
    :else nil))

(defn tutkinnon-osat
  [koulutus]
  (when (amm-tutkinnon-osa? koulutus)
    (-> (for [tutkinnon-osa (get-in koulutus [:metadata :tutkinnonOsat])
              :let [eperuste-id (:ePerusteId tutkinnon-osa)
                    eperuste (get-eperuste-by-id eperuste-id)
                    eperuste-tutkinnon-osa (filter-tutkinnon-osa eperuste (:tutkinnonosaId tutkinnon-osa))]]
          {:eperuste eperuste-id
           :koulutus (:koulutusKoodiUri tutkinnon-osa)
           :opintojenLaajuusNumero (some-> eperuste-tutkinnon-osa :opintojenLaajuusNumero)
           :opintojenLaajuus (some-> eperuste-tutkinnon-osa :opintojenLaajuus :koodiUri)
           :opintojenLaajuusyksikko (get-in eperuste [:opintojenLaajuusyksikko :koodiUri])
           :tutkinnonOsatKoodiUri (some-> eperuste-tutkinnon-osa :koodiUri)}))))

(defn osaamisala-koodi-uri
  [koulutus]
  (some-> (get-ammatillinen-eperuste koulutus)
          (get-osaamisala koulutus)
          (:koodiUri)))

(defn koulutustyyppi-for-organisaatio
  [organisaatio]
  (when-let [oppilaitostyyppi (:oppilaitostyyppi organisaatio)]
    (oppilaitostyyppi-uri-to-tyyppi oppilaitostyyppi)))

(defn- has-alakoodi
  [koodi alakoodit]
  (some #(= (:koodiUri %) (:koodi (extract-versio koodi)))
        alakoodit))

(defn- find-konfo-alakoodit
  [koodiUri]
  (filter #(->> %
                (:alakoodit)
                (has-alakoodi koodiUri))
          (koodisto/pohjakoulutusvaatimuskonfo)))

(defn- map-to-konfo-koodit
  [koutaKoodiUrit]
  (->> koutaKoodiUrit
       (map find-konfo-alakoodit)
       (flatten)
       (map :koodiUri)
       (->distinct-vec)))

; NOTE: kouta - konfo pohjakoulutuskoodit ovat suhteessa * : * joten jokaista koutakoodia vastaa taulukko konfokoodeja
(defn pohjakoulutusvaatimus-koodi-urit
  [hakukohde]
  (->> hakukohde
       (:pohjakoulutusvaatimusKoodiUrit)
       (map-to-konfo-koodit)))

(defn- tutkintotyyppi->koulutustyyppi
  [tutkintotyyppit]
  (cond
    (= tutkintotyyppit ["tutkintotyyppi_06"]) ["amk-alempi"]
    (= tutkintotyyppit ["tutkintotyyppi_12"]) ["amk-ylempi"]
    (= (set tutkintotyyppit) (set ["tutkintotyyppi_13"])) ["kandi"]
    (= (set tutkintotyyppit) (set ["tutkintotyyppi_14"])) ["maisteri"]
    (= (set tutkintotyyppit) (set ["tutkintotyyppi_16"])) ["tohtori"]
    (clojure.set/subset? (set ["tutkintotyyppi_13" "tutkintotyyppi_14"]) (set tutkintotyyppit)) ["kandi-ja-maisteri"]
    :else []))

(defn- get-korkeakoulutus-koulutustyyppi
  [koulutus]
  (let [tutkintotyyppi-koodi-urit (->> (:koulutuksetKoodiUri koulutus)
                                       (mapcat #(koodisto/tutkintotyypit %))
                                       (map :koodiUri))]
    (tutkintotyyppi->koulutustyyppi (distinct tutkintotyyppi-koodi-urit))))

;Konfo-ui:n koulutushaun koulutustyyppi filtteriä varten täytyy tallentaa erinäisiä hakusanoja
;koulutus-search indeksin jarjestajan metadatan koulutustyyppi kenttään.
;Tämä koostuu näistä paloista:
; 1. Ammatillisille haetaan koulutusKoodiUrilla koodistopalvelusta vastaava koulutustyyppiKoodiUri
;    esim. koulutustyyppi_26 =	Ammatillinen perustutkinto tai koulutustyyppi_12 = Erikoisammattitutkinto.
;    Korkeakoulutuksille ei täältä haeta, koska kyseinen koodisto ei erittele korkeakoulutuksia vaan sisältää vain
;    koodin koulutustyyppi_3 = Korkeakoulutus
;
;2. Kaikille tallennetaan koutan sisäinen koulutustyyppi arvo, joka on kouta-ui:lla koulutukselle valittu.
;   Esim. amm, amk, yo, lk, muu, amm-tutkinnon-osa
;
;3. Korkeakoulutuksille tallennetaan kovakoodattu string, joka päätellään koulutusKoodiUrin avulla
;   tutkintotyyppi koodistosta. Esim tutkintotyyppi_13 = kandi tai tutkintotyyppi_12 = amk-ylempi.
;
;4. Jos ammatilliselle toteutukselle on valittu amm-perustutkinto-erityisopetuksena kenttä, tallennetaan
;   kovakoodattu arvo koulutustyyppi_4 = Ammatillinen perustutkinto erityisopetuksena
;
;Lopputulos on taulukko stringejä, esim. ["amm" "koulutustyyppi_26"] tai ["yo" "maisteri"]
(defn- get-koulutustyypit-from-koulutus-koodi
  [koulutus]
  (let [koulutustyyppikoodit (koulutustyyppi-koodi-urit koulutus)
        koulutustyypit-without-erityisopetus (filter #(not= % amm-perustutkinto-erityisopetuksena-koulutustyyppi) koulutustyyppikoodit)
        internal-koulutustyyppi (vector (:koulutustyyppi koulutus))
        result (concat koulutustyypit-without-erityisopetus internal-koulutustyyppi)]
    (if (korkeakoulutus? koulutus)
      (concat result (get-korkeakoulutus-koulutustyyppi koulutus))
      result)))

(defn deduce-koulutustyypit
  ([koulutus toteutus-metadata]
   (let [
         koulutustyyppi (:koulutustyyppi koulutus)
         amm-erityisopetuksena? (:ammatillinenPerustutkintoErityisopetuksena toteutus-metadata)
         tuva-erityisopetuksena? (:jarjestetaanErityisopetuksena toteutus-metadata)
         ]
   (cond
     amm-erityisopetuksena? [amm-perustutkinto-erityisopetuksena-koulutustyyppi koulutustyyppi]
     (and (tuva? koulutus) (not= toteutus-metadata nil)) [(if tuva-erityisopetuksena? "tuva-erityisopetus" "tuva-normal") koulutustyyppi]
     (vapaa-sivistystyo-opistovuosi? koulutus) [koulutustyyppi "vapaa-sivistystyo"]
     (vapaa-sivistystyo-muu? koulutus) [koulutustyyppi "vapaa-sivistystyo"]
     :else (get-koulutustyypit-from-koulutus-koodi koulutus))))
  ([koulutus]
   (deduce-koulutustyypit koulutus nil)))

(defn- get-toteutuksen-hakutieto
  [hakutiedot t]
  (first (filter (fn [x] (= (:toteutusOid x) (:oid t))) hakutiedot)))

(defn- get-haun-julkaistut-hakukohteet
  [haku]
  (let [hakukohteet (map #(set-hakukohde-tila-by-related-haku % haku) (:hakukohteet haku))]
    filter julkaistu? hakukohteet)
)

(defn- filter-hakutiedon-haut-julkaistu-and-not-empty-hakukohteet
  [hakutiedon-haut]
  (->> hakutiedon-haut
       (map (fn [haku] (assoc haku
                              :hakukohteet
                              (get-haun-julkaistut-hakukohteet haku))))
       (filter #(seq (:hakukohteet %)))))

(defn get-toteutuksen-julkaistut-hakutiedot
  [hakutiedot t]
  (let [toteutuksen-hakutiedot (get-toteutuksen-hakutieto hakutiedot t)]
    (assoc toteutuksen-hakutiedot
           :haut
           (filter-hakutiedon-haut-julkaistu-and-not-empty-hakukohteet (:haut toteutuksen-hakutiedot)))))

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

(defn search-terms
  [& {:keys [koulutus
             toteutus
             oppilaitos
             tarjoajat
             hakutiedot
             toteutus-organisaationimi
             opetuskieliUrit
             koulutustyypit
             kuva
             onkoTuleva
             nimi
             lukiopainotukset
             lukiolinjat_er
             osaamisalat
             metadata]
      :or   {koulutus                  []
             toteutus                  []
             oppilaitos                []
             tarjoajat                 []
             hakutiedot                []
             toteutus-organisaationimi {}
             opetuskieliUrit           []
             koulutustyypit            []
             kuva                      nil
             onkoTuleva                nil
             nimi                      {}
             lukiopainotukset          []
             lukiolinjat_er            []
             osaamisalat               []
             metadata                  {}}}]

  (let [tutkintonimikkeet (vec (map #(-> % get-koodi-nimi-with-cache :nimi) (tutkintonimike-koodi-urit koulutus)))
        ammattinimikkeet (asiasana->lng-value-map (get-in toteutus [:metadata :ammattinimikkeet]))
        asiasanat (flatten (get-in toteutus [:metadata :asiasanat]))
        kunnat (remove nil? (distinct (map :kotipaikkaUri tarjoajat)))
        maakunnat (remove nil? (distinct (map #(:koodiUri (koodisto/maakunta %)) kunnat)))
        toteutusNimi (get-esitysnimi toteutus)]
    (remove-nils-from-search-terms
      {:koulutusOid               (:oid koulutus)
       :koulutusnimi              {:fi (:fi (:nimi koulutus))
                                   :sv (:sv (:nimi koulutus))
                                   :en (:en (:nimi koulutus))}
       :koulutus_organisaationimi {:fi (:fi (:nimi oppilaitos))
                                   :sv (:sv (:nimi oppilaitos))
                                   :en (:en (:nimi oppilaitos))}
       :toteutusOid               (:oid toteutus)
       :toteutusNimi              {:fi (:fi toteutusNimi)
                                   :sv (:sv toteutusNimi)
                                   :en (:en toteutusNimi)}
       :oppilaitosOid             (:oid oppilaitos)
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
       :sijainti                  (clean-uris (concat kunnat maakunnat))
       :koulutusalat              (not-empty (clean-uris (koulutusala-koodi-urit koulutus)))
       :hakutiedot                (not-empty (map #(-> %
                                                       (update :hakutapa remove-uri-version)
                                                       (update :valintatavat clean-uris)
                                                       (update :pohjakoulutusvaatimukset clean-uris))
                                                  hakutiedot))
       :opetustavat               (not-empty (clean-uris (or (some-> toteutus :metadata :opetus :opetustapaKoodiUrit) [])))
       :opetuskielet              (not-empty (clean-uris opetuskieliUrit))
       :koulutustyypit            (clean-uris koulutustyypit)
       :onkoTuleva                onkoTuleva
       :kuva                      kuva
       :nimi                      (not-empty nimi)
       :metadata                  (common/decorate-koodi-uris (merge metadata {:kunnat kunnat}))
       :lukiopainotukset          (clean-uris lukiopainotukset)
       :lukiolinjaterityinenkoulutustehtava (clean-uris lukiolinjat_er)
       :osaamisalat               (clean-uris osaamisalat)})))
