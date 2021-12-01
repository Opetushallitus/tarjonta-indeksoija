(ns kouta-indeksoija-service.indexer.kouta.hakukohde
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.tools.general :refer [Tallennettu korkeakoulutus? get-non-korkeakoulu-koodi-uri julkaistu? set-hakukohde-tila-by-related-haku not-poistettu?]]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto-tools]
            [kouta-indeksoija-service.indexer.koodisto.koodisto :as koodisto]
            [clojure.string]))

(def index-name "hakukohde-kouta")
(defonce amm-perustutkinto-erityisopetus-koulutustyyppi "koulutustyyppi_4")
(defonce tuva-koulutustyyppi "koulutustyyppi_40")
(defonce telma-koulutustyyppi "koulutustyyppi_5")
(defonce vapaa-sivistava-koulutustyyppi "koulutustyyppi_10")
(defonce tuva-erityisopetus-koulutustyyppi "koulutustyyppi_41")
(defonce lukio-koulutustyyppi "koulutustyyppi_2")

(defn- assoc-valintaperuste
  [hakukohde valintaperuste]
  (cond-> (dissoc hakukohde :valintaperusteId)
    (some? (:valintaperusteId hakukohde)) (assoc :valintaperuste (-> valintaperuste
                                                                     (dissoc :metadata)
                                                                     (common/complete-entry)))))

(defn- assoc-toteutus
  [hakukohde toteutus]
  (assoc hakukohde :toteutus (-> toteutus
                                 (common/complete-entry)
                                 (common/toteutus->list-item))))

(defn- assoc-sora-data
  [hakukohde sora-tiedot]
  (assoc
   hakukohde
   :sora
   (when sora-tiedot
     (select-keys sora-tiedot [:tila]))))

(defn- luonnos?
  [haku-tai-hakukohde]
  (= Tallennettu (:tila haku-tai-hakukohde)))

(defn- johtaa-tutkintoon?
  [koulutus]
  (:johtaaTutkintoon koulutus))

(defn- alkamiskausi-kevat?
  [haku hakukohde]
  (if-some [alkamiskausi-koodi-uri (if (:kaytetaanHaunAlkamiskautta hakukohde)
                                     (get-in haku [:metadata :koulutuksenAlkamiskausi :koulutuksenAlkamiskausiKoodiUri])
                                     (:alkamiskausiKoodiUri hakukohde))]
    (clojure.string/starts-with? alkamiskausi-koodi-uri "kausi_k#")
    false))

(defn- alkamisvuosi
  [haku hakukohde]
  (when-some [alkamisvuosi (if (:kaytetaanHaunAlkamiskautta hakukohde)
                             (get-in haku [:metadata :koulutuksenAlkamiskausi :koulutuksenAlkamisvuosi])
                             (:alkamisvuosi hakukohde))]
    (Integer/valueOf alkamisvuosi)))

(defn- alkamiskausi-ennen-syksya-2016?
  [haku hakukohde]
  (if-some [alkamisvuosi (alkamisvuosi haku hakukohde)]
    (or (< alkamisvuosi 2016)
        (and (= alkamisvuosi 2016)
             (alkamiskausi-kevat? haku hakukohde)))
    false))

(defn- some-kohdejoukon-tarkenne?
  [haku]
  (not (clojure.string/blank? (:kohdejoukonTarkenneKoodiUri haku))))

(defn- jatkotutkintohaku-tarkenne?
  [haku]
  (clojure.string/starts-with?
   (:kohdejoukonTarkenneKoodiUri haku)
   "haunkohdejoukontarkenne_3#"))

(defn- ->ei-yps
  [syy]
  {:voimassa false :syy syy})

(def ^:private yps
  {:voimassa true
   :syy      "Hakukohde on yhden paikan säännön piirissä"})

(defn- assoc-yps
  [hakukohde haku koulutus]
  (assoc
   hakukohde
   :yhdenPaikanSaanto
   (cond (luonnos? haku)
         (->ei-yps "Haku on luonnos tilassa")

         (luonnos? hakukohde)
         (->ei-yps "Hakukohde on luonnos tilassa")

         (not (korkeakoulutus? koulutus))
         (->ei-yps "Ei korkeakoulutus koulutusta")

         (not (johtaa-tutkintoon? koulutus))
         (->ei-yps "Ei tutkintoon johtavaa koulutusta")

         (alkamiskausi-ennen-syksya-2016? haku hakukohde)
         (->ei-yps "Koulutuksen alkamiskausi on ennen syksyä 2016")

         (and (some-kohdejoukon-tarkenne? haku)
              (not (jatkotutkintohaku-tarkenne? haku)))
         (->ei-yps (str "Haun kohdejoukon tarkenne on "
                        (:kohdejoukonTarkenneKoodiUri haku)))

         :else
         yps)))

(defn- assoc-hakulomake-linkki
  [hakukohde haku]
  (let [link-holder (if (true? (:kaytetaanHaunHakulomaketta hakukohde)) haku hakukohde)]
    (conj hakukohde (common/create-hakulomake-linkki-for-hakukohde link-holder (:oid hakukohde)))))

(defn- use-special-koulutus [toteutus koulutus]
  (let [koulutuksentyyppi (get-in koulutus [:metadata :tyyppi])]
    (cond
      (true? (get-in toteutus [:metadata :ammatillinenPerustutkintoErityisopetuksena]))
      amm-perustutkinto-erityisopetus-koulutustyyppi
      (true? (get-in toteutus [:metadata :jarjestetaanErityisopetuksena]))
      tuva-erityisopetus-koulutustyyppi
      (= koulutuksentyyppi "tuva")
      tuva-koulutustyyppi
      (= koulutuksentyyppi "telma")
      telma-koulutustyyppi
      (= koulutuksentyyppi "vapaa-sivistystyo-muu")
      vapaa-sivistava-koulutustyyppi
      (= koulutuksentyyppi "vapaa-sivistystyo-opistovuosi")
      vapaa-sivistava-koulutustyyppi)))

(defn- filter-expired-koodis
  [koodit]
  (let [aktiivisetkoulutustyypit (->> (koodisto/get-from-index "koulutustyyppi")
                                     :koodit
                                     (map #(:koodiUri %)))]
    (filter #(some (partial = %) aktiivisetkoulutustyypit) koodit)))

(defn- get-koulutustyyppikoodi-from-koodisto
  [koulutus]
  (let [code-state-not-passive #(not (= "PASSIIVINEN" (:tila %)))
        koodiurit (->> koulutus
                      :koulutuksetKoodiUri
                      (mapcat koodisto-tools/koulutustyypit)
                      (filter code-state-not-passive)
                      (map :koodiUri)
                      (filter-expired-koodis)
                      (distinct)
                      (filter #(not (.contains [amm-perustutkinto-erityisopetus-koulutustyyppi tuva-erityisopetus-koulutustyyppi] %))))]
    (cond
      (= 1 (count koodiurit)) ; ei tehdä päättelyä useamman koulutustyypin välillä, vaan jätetään arvoksi nil paitsi jos lukiokoulutus löytyy
      (first koodiurit)

      (seq (filter #(= lukio-koulutustyyppi %) koodiurit))
      lukio-koulutustyyppi)))

(defn- assoc-koulutustyypit
  [hakukohde toteutus koulutus]
  (let [specialkoodi (use-special-koulutus toteutus koulutus)
        koulutustyyppikoodi (if (not (nil? specialkoodi))
                                specialkoodi
                              (get-koulutustyyppikoodi-from-koodisto koulutus))]
       (assoc hakukohde :koulutustyyppikoodi koulutustyyppikoodi)))

(defn- assoc-onko-harkinnanvarainen-koulutus
  [hakukohde koulutus]
  (let [non-korkeakoulu-koodi-uri (get-non-korkeakoulu-koodi-uri koulutus)
        hakokohde-nimi-koodi-uri (get-in hakukohde [:hakukohde :koodiUri])]
    (assoc hakukohde :onkoHarkinnanvarainenKoulutus (and
                                                     (some? non-korkeakoulu-koodi-uri)
                                                     (nil? (koodisto-tools/ei-harkinnanvaraisuutta non-korkeakoulu-koodi-uri))
                                                     (or (nil? hakokohde-nimi-koodi-uri)
                                                         (nil? (koodisto-tools/ei-harkinnanvaraisuutta hakokohde-nimi-koodi-uri)))))))

(defn- assoc-jarjestaako-urheilijan-amm-koulutusta [hakukohde toimipiste]
  (assoc hakukohde :jarjestaaUrheilijanAmmKoulutusta (boolean (get-in toimipiste [:metadata :jarjestaaUrheilijanAmmKoulutusta]))))

(defn- assoc-nimi-as-esitysnimi
  [hakukohde]
  (assoc hakukohde :nimi (:esitysnimi (:_enrichedData hakukohde))))

(defn create-index-entry
  [oid]
    (let [hakukohde (-> (kouta-backend/get-hakukohde oid)
                        (assoc-nimi-as-esitysnimi)
                        (koodisto-tools/assoc-hakukohde-nimi-from-koodi)
                        (common/complete-entry))]
    (if (not-poistettu? hakukohde)
      (let [haku              (kouta-backend/get-haku (:hakuOid hakukohde))
            toteutus          (kouta-backend/get-toteutus (:toteutusOid hakukohde))
            koulutus          (kouta-backend/get-koulutus (:koulutusOid toteutus))
            sora-kuvaus       (kouta-backend/get-sorakuvaus (:sorakuvausId koulutus))
            valintaperusteId  (:valintaperusteId hakukohde)
            valintaperuste    (when-not (clojure.string/blank? valintaperusteId)
                                (kouta-backend/get-valintaperuste valintaperusteId))
            jarjestyspaikkaOid (:jarjestyspaikkaOid hakukohde)
            jarjestava-toimipiste (when-not (clojure.string/blank? jarjestyspaikkaOid)
                                    (kouta-backend/get-oppilaitoksen-osa jarjestyspaikkaOid))]
        (indexable/->index-entry oid
                                 (-> hakukohde
                                     (assoc-yps haku koulutus)
                                     (set-hakukohde-tila-by-related-haku haku)
                                     (assoc-sora-data sora-kuvaus)
                                     (assoc-onko-harkinnanvarainen-koulutus koulutus)
                                     (assoc-koulutustyypit toteutus koulutus)
                                     (assoc-toteutus toteutus)
                                     (assoc-valintaperuste valintaperuste)
                                     (assoc-jarjestaako-urheilijan-amm-koulutusta jarjestava-toimipiste)
                                     (assoc-hakulomake-linkki haku)
                                     (dissoc :_enrichedData)) hakukohde))
      (indexable/->delete-entry oid hakukohde))))

(defn do-index
  [oids execution-id]
  (indexable/do-index index-name oids create-index-entry execution-id))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
