(ns kouta-indeksoija-service.indexer.tools.search
  (:require [kouta-indeksoija-service.indexer.tools.general :refer [amm-osaamisala? amm-tutkinnon-osa? any-ammatillinen? ammatillinen? korkeakoulutus?]]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto]
            [kouta-indeksoija-service.rest.koodisto :refer [extract-versio get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.tools.koodisto :refer [pohjakoulutusvaatimuskonfo]]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [remove-uri-version koodi-arvo oppilaitostyyppi-uri-to-tyyppi]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.util.tools :refer [->distinct-vec]]
            [kouta-indeksoija-service.indexer.cache.eperuste :refer [get-eperuste-by-koulutuskoodi get-eperuste-by-id filter-tutkinnon-osa]]))

(defonce amm-perustutkinto-erityisopetuksena-koulutustyyppi "koulutustyyppi_4")

(defn- clean-uris
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
             hakuajat
             hakutapaUrit
             valintatapaUrit
             pohjakoulutusvaatimusUrit
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
             metadata]
      :or {koulutustyypit []
           opetuskieliUrit []
           tarjoajat []
           tarjoajaOids []
           oppilaitos []
           koulutusalaUrit []
           nimet []
           hakuajat []
           hakutapaUrit []
           valintatapaUrit []
           pohjakoulutusvaatimusUrit []
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
           metadata {}}}]

  (let [tutkintonimikkeet (vec (map #(-> % get-koodi-nimi-with-cache :nimi) tutkintonimikeUrit))
        kunnat (remove nil? (distinct (map :kotipaikkaUri tarjoajat)))
        maakunnat (remove nil? (distinct (map #(:koodiUri (koodisto/maakunta %)) kunnat)))

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
             :hakuajat hakuajat
             :hakutavat (clean-uris hakutapaUrit)
             :valintatavat (clean-uris valintatapaUrit)
             :pohjakoulutusvaatimukset (clean-uris pohjakoulutusvaatimusUrit)
             :koulutusalat (clean-uris koulutusalaUrit)
             :opetustavat (clean-uris opetustapaUrit)
             :terms {:fi (terms :fi)
                     :sv (terms :sv)
                     :en (terms :en)}
             :metadata (common/decorate-koodi-uris (merge metadata {:kunnat kunnat}))}

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

(defn- get-non-korkeakoulu-koodi-uri
  [koulutus]
  (-> koulutus
      (:koulutuksetKoodiUri)
      (first))) ;Ainoastaan korkeakoulutuksilla voi olla useampi kuin yksi koulutusKoodi

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
(defn- get-eperuste
  [koulutus]
  (let [eperuste-id (:ePerusteId koulutus)]
    (if eperuste-id
      (get-eperuste-by-id eperuste-id)
      (get-eperuste-by-koulutuskoodi (get-non-korkeakoulu-koodi-uri koulutus)))))

(defn tutkintonimike-koodi-urit
  [koulutus]
  (if (ammatillinen? koulutus)
    (when-let [eperuste (get-eperuste koulutus)]
      (->distinct-vec (map :tutkintonimikeUri (:tutkintonimikkeet eperuste))))
    (get-in koulutus [:metadata :tutkintonimikeKoodiUrit] [])))

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
    (ammatillinen? koulutus)   (-> koulutus (get-eperuste) (get-in [:opintojenLaajuus :koodiUri]))
    (amm-osaamisala? koulutus) (-> koulutus (get-eperuste) (get-osaamisala koulutus) (get-in [:opintojenLaajuus :koodiUri]))
    :default                   (get-in koulutus [:metadata :opintojenLaajuusKoodiUri])))

(defn opintojen-laajuus-numero
  [koulutus]
  (cond
    (ammatillinen? koulutus)   (-> koulutus (get-eperuste) :opintojenLaajuusNumero)
    (amm-osaamisala? koulutus) (-> koulutus (get-eperuste) (get-osaamisala koulutus) :opintojenLaajuusNumero)
    :default                   (-> (get-in koulutus [:metadata :opintojenLaajuusKoodiUri]) koodi-arvo)))

(defn opintojen-laajuusyksikko-koodi-uri
  [koulutus]
  (cond
    (ammatillinen? koulutus)   (-> koulutus (get-eperuste) (get-in [:opintojenLaajuusyksikko :koodiUri]))
    (amm-osaamisala? koulutus) (-> koulutus (get-eperuste) (get-in [:opintojenLaajuusyksikko :koodiUri]))
    (korkeakoulutus? koulutus) "opintojenlaajuusyksikko_2#1" ;opintopistettä
    :default nil))

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
  (some-> (get-eperuste koulutus)
          (get-osaamisala koulutus)
          (:koodiUri)))

(defn koulutustyyppi-for-organisaatio
  [organisaatio]
  (when-let [oppilaitostyyppi (:oppilaitostyyppi organisaatio)]
    (oppilaitostyyppi-uri-to-tyyppi oppilaitostyyppi)))

(defn hakutapa-koodi-urit
  [hakutieto]
  (->distinct-vec (->> hakutieto
                       :haut
                       (map :hakutapaKoodiUri))))

(defn valintatapa-koodi-urit
  [hakutieto]
  (->distinct-vec
    (flatten
       (->> hakutieto
            :haut
            (map #(->> % :hakukohteet (map :valintatapaKoodiUrit)))))))

(defn- has-alakoodi
  [koodi alakoodit]
  (some #(= (:koodiUri %) (:koodi (extract-versio koodi)))
        alakoodit))

(defn- find-konfo-alakoodit
  [koodiUri]
  (filter #(->> %
                (:alakoodit)
                (has-alakoodi koodiUri))
          (pohjakoulutusvaatimuskonfo)))

(defn- map-to-konfo-koodit
  [koutaKoodiUrit]
  (->> koutaKoodiUrit
       (map find-konfo-alakoodit)
       (flatten)
       (map :koodiUri)
       (->distinct-vec)))

(defn- get-pohjakoulutusvaatimus-koodi-urit-from-hakutieto
  [hakutieto]
  (->> hakutieto
       :haut
       (map #(->> % :hakukohteet (map :pohjakoulutusvaatimusKoodiUrit)))
       (flatten)
       (->distinct-vec)))

; NOTE: kouta - konfo pohjakoulutuskoodit ovat suhteessa * : * joten jokaista koutakoodia vastaa taulukko konfokoodeja
(defn pohjakoulutusvaatimus-koodi-urit
  [hakutieto]
  (->> hakutieto
       (get-pohjakoulutusvaatimus-koodi-urit-from-hakutieto)
       (map-to-konfo-koodit)))

(defn- tutkintotyyppi->koulutustyyppi
  [tutkintotyyppit]
  (case tutkintotyyppit
    ["tutkintotyyppi_12"] ["ylempi-amk"]
    ["tutkintotyyppi_06"] ["alempi-amk"]
    ["tutkintotyyppi_13"] ["kandi"]
    ["tutkintotyyppi_14"] ["maisteri"]
    []))

(defn- get-korkeakoulutus-koulutustyyppi
  [koulutus]
  (let [tutkintotyyppi-koodi-urit (->> (:koulutuksetKoodiUri koulutus)
                                       (map #(koodisto/tutkintotyypit %))
                                       (map :koodiUri))]
    (tutkintotyyppi->koulutustyyppi tutkintotyyppi-koodi-urit)))


(defn- get-koulutustyypit-from-koulutus-koodi
  [koulutus]
  (let [koulutustyyppikoodit (koulutustyyppi-koodi-urit koulutus)
        koulutustyypit-without-erityisopetus (filter #(not= % amm-perustutkinto-erityisopetuksena-koulutustyyppi) koulutustyyppikoodit)
        internal-koulutystyyppi (vector (:koulutustyyppi koulutus))
        result (concat koulutustyypit-without-erityisopetus internal-koulutystyyppi)]
    (if (korkeakoulutus? koulutus)
      (concat result (get-korkeakoulutus-koulutustyyppi koulutus))
      result)))

(defn deduce-koulutustyypit
  ([koulutus ammatillinen-perustutkinto-erityisopetuksena?]
   (if ammatillinen-perustutkinto-erityisopetuksena?
     (concat [amm-perustutkinto-erityisopetuksena-koulutustyyppi] (vector (:koulutustyyppi koulutus)))
     (get-koulutustyypit-from-koulutus-koodi koulutus)))
  ([koulutus]
   (deduce-koulutustyypit koulutus false)))