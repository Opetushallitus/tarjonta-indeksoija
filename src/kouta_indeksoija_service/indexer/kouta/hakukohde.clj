(ns kouta-indeksoija-service.indexer.kouta.hakukohde
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.tools.general :refer [Tallennettu korkeakoulutus? get-non-korkeakoulu-koodi-uri set-hakukohde-tila-by-related-haku not-poistettu?]]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [remove-uri-version]]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.koulutustyyppi :refer [assoc-koulutustyyppi-path]]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto-tools]
            [kouta-indeksoija-service.indexer.koodisto.koodisto :as koodisto]
            [kouta-indeksoija-service.util.tools :refer [get-esitysnimi jarjestaa-urheilijan-amm-koulutusta?]]
            [clojure.set :as s]
            [clojure.string :as str]
            [clj-time.format :as f]
            [clj-time.core :as t]))

(def index-name "hakukohde-kouta")
(defonce amm-perustutkinto-erityisopetus-koulutustyyppi "koulutustyyppi_4")
(defonce tuva-koulutustyyppi "koulutustyyppi_40")
(defonce telma-koulutustyyppi "koulutustyyppi_5")
(defonce vapaa-sivistava-koulutustyyppi "koulutustyyppi_10")
(defonce tuva-erityisopetus-koulutustyyppi "koulutustyyppi_41")
(defonce lukio-koulutustyyppi "koulutustyyppi_2")
(defonce painotettavat-oppiaineet-lukiossa-kaikki #{"painotettavatoppiaineetlukiossa_a1"
                                                   "painotettavatoppiaineetlukiossa_a2"
                                                   "painotettavatoppiaineetlukiossa_b1"
                                                   "painotettavatoppiaineetlukiossa_b2"
                                                   "painotettavatoppiaineetlukiossa_b3"})

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
    (str/starts-with? alkamiskausi-koodi-uri "kausi_k#")
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
  (not (str/blank? (:kohdejoukonTarkenneKoodiUri haku))))

(defn- jatkotutkintohaku-tarkenne?
  [haku]
  (str/starts-with?
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
  [hakukohde toteutus koulutus]
  (let [non-korkeakoulu-koodi-uri (get-non-korkeakoulu-koodi-uri koulutus)
        hakukohde-nimi-koodi-uri (get-in hakukohde [:hakukohde :koodiUri])
        ; Alla oleva funktio palauttaa koodirelaatioiden kautta esimerkiksi seuraavilla ehdoilla:
        ; - Hakukohde kuuluu koulutukseen "Media-alan perustutkinto" = false
        ; - Hakukohde kuuluu koulutukseen "Hius- ja kauneudenhoitoalan perustutkinto" = true
        ; - Hakukohde on "Perustason ensihoidon osaamisala (Sosiaali- ja terveysalan perustutkinto)" = false
        kysytaanko-harkinnanvaraisuutta-lomakkeella (fn [koodi-uri]
                                                      (nil? (koodisto-tools/harkinnanvaraisuutta-ei-kysyta-lomakkeella koodi-uri)))
        ; Seuraava arvo on tosi esimerkille: "Hakukohde kuuluu koulutukseen "Hius- ja kauneudenhoitoalan perustutkinto"
        harkinnanvaraisuus-question-allowed (and
                                              (some? non-korkeakoulu-koodi-uri)
                                              (kysytaanko-harkinnanvaraisuutta-lomakkeella non-korkeakoulu-koodi-uri)
                                              (not (true? (get-in toteutus [:metadata :ammatillinenPerustutkintoErityisopetuksena])))
                                              (not (.contains ["telma" "tuva" "vapaa-sivistystyo-opistovuosi"] (:koulutustyyppi koulutus)))
                                              (or (nil? hakukohde-nimi-koodi-uri)
                                                  (kysytaanko-harkinnanvaraisuutta-lomakkeella hakukohde-nimi-koodi-uri)))
        ; Seuraava arvo on tosi seuraaville esimerkeille:
        ; - "Hakukohde kuuluu koulutukseen "Hius- ja kauneudenhoitoalan perustutkinto"
        ; - "Hakukohde on "Perustason ensihoidon osaamisala (Sosiaali- ja terveysalan perustutkinto)"
        hakukohde-allows-harkinnanvaraiset-applicants (or harkinnanvaraisuus-question-allowed
                                                          (and
                                                            (some? non-korkeakoulu-koodi-uri)
                                                            (and (some? hakukohde-nimi-koodi-uri)
                                                                 ;Jos hakukohteella on relaatio ei-harkinnanvaraisuutta koodiin "Harkinnanvaraisuutta ei kysytä lomakkeella", se on automaattisesti harkinnanvarainen
                                                                 (not (true? (get-in toteutus [:metadata :ammatillinenPerustutkintoErityisopetuksena])))
                                                                 (or (not (kysytaanko-harkinnanvaraisuutta-lomakkeella non-korkeakoulu-koodi-uri))
                                                                     (not (kysytaanko-harkinnanvaraisuutta-lomakkeella hakukohde-nimi-koodi-uri))))))]
    (assoc hakukohde :salliikoHakukohdeHarkinnanvaraisuudenKysymisen harkinnanvaraisuus-question-allowed
                     :voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita hakukohde-allows-harkinnanvaraiset-applicants)))


(defn- assoc-jarjestaako-urheilijan-amm-koulutusta [hakukohde jarjestyspaikka]
  (if (= (:tila jarjestyspaikka) "julkaistu")
    (assoc hakukohde :jarjestaaUrheilijanAmmKoulutusta (jarjestaa-urheilijan-amm-koulutusta? jarjestyspaikka))
    hakukohde))

(defn- assoc-nimi-as-esitysnimi
  [hakukohde]
  (assoc hakukohde :nimi (get-esitysnimi hakukohde)))

(defn- parse-tarkka-ajankohta [time-str]
  (if-let [date (f/parse time-str)]
    {:kausiUri (if (>= (t/month date) 8)
                 "kausi_s#1"
                 "kausi_k#1")
     :vuosi (t/year date)}))

(defn- parse-alkamiskausi [alkamiskausi oid]
  (let [tyyppi (:alkamiskausityyppi alkamiskausi)
        result (case tyyppi
                 "tarkka alkamisajankohta" (parse-tarkka-ajankohta (:koulutuksenAlkamispaivamaara alkamiskausi))
                 "alkamiskausi ja -vuosi" {:kausiUri (:koulutuksenAlkamiskausiKoodiUri alkamiskausi) :vuosi (:koulutuksenAlkamisvuosi alkamiskausi)}
                 {})]
    (when (and (:kausiUri result) (:vuosi result))
      (merge result
             {:alkamiskausityyppi tyyppi
              :source oid}))))

(defn- assoc-paatelty-alkamiskausi-for-hakukohde [hakukohde haku toteutus]
  (if-let [result (or (parse-alkamiskausi (get-in hakukohde [:metadata :koulutuksenAlkamiskausi]) (:oid hakukohde))
                      (parse-alkamiskausi (get-in haku [:metadata :koulutuksenAlkamiskausi]) (:oid haku))
                      (parse-alkamiskausi (get-in toteutus [:metadata :opetus :koulutuksenAlkamiskausi]) (:oid toteutus)))]
    (assoc hakukohde :paateltyAlkamiskausi result)
    hakukohde))

(defn- get-koodiurit-to-complete
  [koodiurit]
  (flatten
    (filter #(contains? painotettavat-oppiaineet-lukiossa-kaikki (get-in % [:koodiUrit :oppiaine])) (set koodiurit))))

(defn- remove-uri-versions
  [koodiurit]
  (map #(update-in % [:koodiUrit :oppiaine] remove-uri-version) koodiurit))

(defn- get-koodisto-koodiurit
  [koodiurit koodiurit-to-complete]
  (flatten
    (remove-uri-versions (s/difference (set koodiurit) (set koodiurit-to-complete)))))

(defn- replace-with-koodisto-oppiaineet
  [koodiuri]
  (let [koodisto-oppiaineet (filter #(str/starts-with? % (get-in koodiuri [:koodiUrit :oppiaine]))
                                    (koodisto-tools/painotettavatoppiaineetlukiossa-koodiurit))
        painokerroin (get koodiuri :painokerroin)]
    (map #(assoc {} :koodiUrit {:oppiaine %}, :painokerroin painokerroin) koodisto-oppiaineet)))

(defn- complete-koodiurit
  [koodiurit-to-complete]
  (flatten
    (map #(replace-with-koodisto-oppiaineet %) koodiurit-to-complete)))

(defn- get-matching-painokerroin
  [koodiuri koodiurit-koodisto]
  (first
    (filter #(= (get-in koodiuri [:koodiUrit :oppiaine]) (get-in % [:koodiUrit :oppiaine])) koodiurit-koodisto)))

(defn- get-painokerroin-or-use-current
  [koodiuri koodiurit-koodisto]
  (assoc koodiuri :painokerroin (get (or (get-matching-painokerroin koodiuri koodiurit-koodisto) koodiuri) :painokerroin)))

(defn- get-missing-koodiurit
  [koodiurit-koodisto koodiurit-with-painokertoimet]
  (flatten
    (seq (s/difference (set koodiurit-koodisto) (set koodiurit-with-painokertoimet)))))

(defn- complete-painotetut-lukioarvosanat-kaikki
  [koodiurit]
  (let [koodiurit-to-complete (get-koodiurit-to-complete koodiurit)
        koodiurit-koodisto (get-koodisto-koodiurit koodiurit koodiurit-to-complete)
        koodiurit-completed (complete-koodiurit koodiurit-to-complete)
        koodiurit-with-painokertoimet (map #(get-painokerroin-or-use-current % koodiurit-koodisto) koodiurit-completed)
        koodiurit-to-add (get-missing-koodiurit koodiurit-koodisto koodiurit-with-painokertoimet)]
    (vec (flatten (conj koodiurit-with-painokertoimet koodiurit-to-add)))))

(defn- complete-painotetut-lukioarvosanat-if-exists
  [hakukohde]
  (if
    (not (nil? (get-in hakukohde [:metadata :hakukohteenLinja :painotetutArvosanat])))
    (update-in hakukohde [:metadata :hakukohteenLinja :painotetutArvosanat] complete-painotetut-lukioarvosanat-kaikki)
    hakukohde))

(defn create-index-entry
  [oid execution-id]
  (let [hakukohde-from-kouta (kouta-backend/get-hakukohde-with-cache oid execution-id)]
    (if (not-poistettu? hakukohde-from-kouta)
      (let [hakukohde (-> hakukohde-from-kouta
                          (assoc-nimi-as-esitysnimi)
                          (koodisto-tools/assoc-hakukohde-nimi-from-koodi)
                          (complete-painotetut-lukioarvosanat-if-exists)
                          (common/complete-entry))
            haku (kouta-backend/get-haku-with-cache (:hakuOid hakukohde) execution-id)
            toteutus (kouta-backend/get-toteutus-with-cache (:toteutusOid hakukohde) execution-id)
            koulutus (kouta-backend/get-koulutus-with-cache (:koulutusOid toteutus) execution-id)
            sora-kuvaus (kouta-backend/get-sorakuvaus-with-cache (:sorakuvausId koulutus) execution-id)
            valintaperusteId (:valintaperusteId hakukohde)
            valintaperuste (when-not (str/blank? valintaperusteId)
                             (kouta-backend/get-valintaperuste-with-cache valintaperusteId execution-id))
            jarjestyspaikkaOid (get-in hakukohde [:jarjestyspaikka :oid])
            jarjestyspaikka-oppilaitos (when-not (str/blank? jarjestyspaikkaOid)
                                         (first
                                           (:oppilaitokset
                                            (kouta-backend/get-oppilaitokset-with-cache [jarjestyspaikkaOid] execution-id))))]
        (indexable/->index-entry-with-forwarded-data oid
                                                     (-> hakukohde
                                                         (assoc-koulutustyyppi-path koulutus (:metadata toteutus))
                                                         (assoc-yps haku koulutus)
                                                         (assoc :koulutustyyppi (:koulutustyyppi koulutus))
                                                         (set-hakukohde-tila-by-related-haku haku)
                                                         (assoc-sora-data sora-kuvaus)
                                                         (assoc-onko-harkinnanvarainen-koulutus toteutus koulutus)
                                                         (assoc-koulutustyypit toteutus koulutus)
                                                         (assoc-toteutus toteutus)
                                                         (assoc-valintaperuste valintaperuste)
                                                         (assoc-jarjestaako-urheilijan-amm-koulutusta jarjestyspaikka-oppilaitos)
                                                         (assoc-hakulomake-linkki haku)
                                                         (assoc-paatelty-alkamiskausi-for-hakukohde haku toteutus)
                                                         (dissoc :_enrichedData)
                                                         (common/localize-dates)) hakukohde))
      (indexable/->delete-entry-with-forwarded-data oid hakukohde-from-kouta))))

(defn do-index
  [oids execution-id]
  (indexable/do-index index-name oids create-index-entry execution-id))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
