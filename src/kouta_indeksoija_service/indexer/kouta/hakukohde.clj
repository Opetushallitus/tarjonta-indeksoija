(ns kouta-indeksoija-service.indexer.kouta.hakukohde
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.tools.general :refer [Tallennettu korkeakoulutus? get-non-korkeakoulu-koodi-uri]]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.koodisto :as koodisto]
            [clojure.string]))

(def index-name "hakukohde-kouta")
(defonce erityisopetus-koulutustyyppi "koulutustyyppi_4")
(defonce tuva-eritysopetus-koulutustyyppi "koulutustyyppi_41")

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

(defn- conj-er-koulutus [toteutus koulutustyyppi]
  (cond
    (true? (get-in toteutus [:metadata :ammatillinenPerustutkintoErityisopetuksena]))
    erityisopetus-koulutustyyppi
    (true? (get-in toteutus [:metadata :tuvaErityisopetuksena]))
    tuva-eritysopetus-koulutustyyppi
    :else koulutustyyppi))

(defn- assoc-koulutustyypit
  [hakukohde toteutus koulutus]
  (let [koodiurit (->> koulutus
                    :koulutuksetKoodiUri
                    (mapcat koodisto/koulutustyypit)
                    (map :koodiUri))
        has-only-one (= 1 (count koodiurit))
        koulutustyyppikoodi (when has-only-one
                              (conj-er-koulutus toteutus (first koodiurit)))]
       (assoc hakukohde :koulutustyyppikoodi koulutustyyppikoodi)))

(defn- assoc-onko-harkinnanvarainen-koulutus
  [hakukohde koulutus]
  (let [non-korkeakoulu-koodi-uri (get-non-korkeakoulu-koodi-uri koulutus)]
    (assoc hakukohde :onkoHarkinnanvarainenKoulutus (and
                                                     (some? non-korkeakoulu-koodi-uri)
                                                     (nil? (koodisto/ei-harkinnanvaraisuutta non-korkeakoulu-koodi-uri))))))

(defn- assoc-jarjestaako-urheilijan-amm-koulutusta [hakukohde toimipiste]
  (assoc hakukohde :jarjestaaUrheilijanAmmKoulutusta (boolean (get-in toimipiste [:metadata :jarjestaaUrheilijanAmmKoulutusta]))))

(defn create-index-entry
  [oid]
  (let [hakukohde      (kouta-backend/get-hakukohde oid)
        haku           (kouta-backend/get-haku (:hakuOid hakukohde))
        toteutus       (kouta-backend/get-toteutus (:toteutusOid hakukohde))
        koulutus       (kouta-backend/get-koulutus (:koulutusOid toteutus))
        sora-kuvaus    (kouta-backend/get-sorakuvaus (:sorakuvausId koulutus))
        valintaperusteId (:valintaperusteId hakukohde)
        valintaperuste (when-not (clojure.string/blank? valintaperusteId)
                         (kouta-backend/get-valintaperuste valintaperusteId))
        jarjestyspaikkaOid (:jarjestyspaikkaOid hakukohde)
        jarjestava-toimipiste (when-not (clojure.string/blank? jarjestyspaikkaOid)
                                (kouta-backend/get-oppilaitoksen-osa jarjestyspaikkaOid))]
    (indexable/->index-entry oid
                             (-> hakukohde
                                 (koodisto/assoc-hakukohde-nimi-from-koodi)
                                 (assoc-yps haku koulutus)
                                 (common/complete-entry)
                                 (assoc-sora-data sora-kuvaus)
                                 (assoc-onko-harkinnanvarainen-koulutus koulutus)
                                 (assoc-koulutustyypit toteutus koulutus)
                                 (assoc-toteutus toteutus)
                                 (assoc-valintaperuste valintaperuste)
                                 (assoc-jarjestaako-urheilijan-amm-koulutusta jarjestava-toimipiste)
                                 (assoc-hakulomake-linkki haku)))))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entry))

(defn get-from-index
  [oid]
  (indexable/get index-name oid))
