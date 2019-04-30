(ns kouta-indeksoija-service.search-data.koulutus
  (:require [kouta-indeksoija-service.rest.tarjonta :as tarjonta-client]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.converter.tyyppi :refer [koulutustyyppi-uri-to-tyyppi]]
            [kouta-indeksoija-service.util.time :refer :all]))

(defn- fix-nimi-keys [json]
  (assoc (dissoc json :nimi) :nimi (clojure.set/rename-keys (:nimi json) {:fi :kieli_fi :en :kieli_en :sv :kieli_sv})))

(defn- loppuPvm-to-opintopolkuPvm [loppuPvm]
  (format (add-months (convert-to-datetime loppuPvm) 6)))

(defn parse-hakuaika-ryhma [hakuaika-ryhma]
  (if (not (nil? hakuaika-ryhma))
    (let [hakuajat (re-seq #"\d{4}\-\d{2}\-\d{2}\s\d{2}\:\d{2}" hakuaika-ryhma)
          alkupvm (if-let [s (nth hakuajat 0 nil)] (convert-to-long (parse-with-time s)))
          loppupvm (if-let [s (nth hakuajat 1 nil)] (convert-to-long (parse-with-time s)))]
      (-> {}
          (cond-> alkupvm (assoc :alkuPvm alkupvm))
          (cond-> loppupvm (assoc :loppuPvm loppupvm))))))

(defn- fix-hakuaika [json]
  (if-let [hakuaika (parse-hakuaika-ryhma (:hakuaikaRyhma json))]
    (assoc (dissoc json :hakuaikaRyhma) :hakuaika hakuaika)
    (dissoc json :hakuaikaRyhma)))

(defn count-opintopolun-nayttaminen-loppuu-from-hakuajat [hakuajat]
  (if-let [loppuPvms (not-empty (remove nil? (map #(:loppuPvm %) hakuajat)))]
    (if-let [maxLoppuPvm (apply max loppuPvms)]
      (loppuPvm-to-opintopolkuPvm maxLoppuPvm))))

(defn find-opintopolun-nayttaminen-loppuu [haut]
  (last (sort (map #(:opintopolunNayttaminenLoppuu %) haut))))

(defn count-opintopolun-nayttaminen-loppuu-haut [haut]
  (if-let [hakuajat (not-empty (apply concat (map #(:hakuaikas %) haut)))]
    (count-opintopolun-nayttaminen-loppuu-from-hakuajat hakuajat)))

(defn count-opintopolun-nayttaminen-loppuu-hakukohteet [hakukohteet]
  (if-let [hakuajat (not-empty (map #(parse-hakuaika-ryhma (:hakuaikaRyhma %)) hakukohteet))]
    (count-opintopolun-nayttaminen-loppuu-from-hakuajat hakuajat)))

(defn count-opintopolun-nayttaminen-loppuu [haut hakukohteet] ;TODO -> haku, jolla ei ole loppuPvm:ää?
  (if-let [opintopolunNayttaminenLoppuu (find-opintopolun-nayttaminen-loppuu haut)]
    opintopolunNayttaminenLoppuu
    (if-let [loppuPvmHakukohteet (count-opintopolun-nayttaminen-loppuu-hakukohteet hakukohteet)]
      loppuPvmHakukohteet
      (count-opintopolun-nayttaminen-loppuu-haut haut))))

(defn find-koulutus-nimi [koulutus hakukohteet tyyppi]
  (if (= "lk" tyyppi)
    (:nimi (:koulutusohjelma koulutus))
    (if (not (empty? (:nimi koulutus)))
      (:nimi koulutus)
      (if-let [koulutuskoodi (:koulutuskoodi koulutus)]
        (:nimi koulutuskoodi)
        (if-let [hakukohde (first hakukohteet)]                   ;TODO -> miltä hakukohteelta haetaan?
          (:nimi hakukohde))))))

(defn- find-koulutus-tyyppi [koulutus]
  (let [tyyppi (koulutustyyppi-uri-to-tyyppi (get-in koulutus [:koulutustyyppi :uri]))]
    (if (and (= "kk" tyyppi) (or (:isAvoimenYliopistonKoulutus koulutus) (not (:johtaaTutkintoon koulutus))))
      "ako"
      tyyppi)))

(defn append-search-data
  [koulutus]
  (let [hakukohteet-raw (tarjonta-client/get-hakukohteet-for-koulutus (:oid koulutus))
        hakukohteet (doall (map #(fix-nimi-keys (fix-hakuaika (select-keys % [:nimi :oid :hakuaikaRyhma :hakuOid]))) hakukohteet-raw))
        haut-raw (tarjonta-client/get-haut-by-oids (distinct (map :hakuOid hakukohteet)))
        haut (doall (map #(dissoc % :maxHakukohdes :hakukohdeOids :canSubmitMultipleApplications :sisaltyvatHaut
                                  :tunnistusKaytossa :modified :autosyncTarjonta :usePriority :hakukohdeOidsYlioppilastutkintoAntaaHakukelpoisuuden
                                  :yhdenPaikanSaanto :modifiedBy :sijoittelu :autosyncTarjontaFrom :autosyncTarjontaTo :ylioppilastutkintoAntaaHakukelpoisuuden) haut-raw))
        organisaatio-raw (organisaatio-client/get-doc (assoc (:organisaatio koulutus) :type "organisaatio") false)
        organisaatio (fix-nimi-keys (select-keys organisaatio-raw [:nimi :oid :status :kotipaikkaUri :alkuPvm :loppuPvm :parentOidPath]))
        tyyppi (find-koulutus-tyyppi koulutus)
        nimi (find-koulutus-nimi koulutus hakukohteet tyyppi)
        opintopolunNayttaminenLoppuu (count-opintopolun-nayttaminen-loppuu haut hakukohteet-raw)
        oppiaineet (map (fn [x] { (keyword (:kieliKoodi x)) (:oppiaine x) }) (:oppiaineet koulutus))]
    (let [searchData (-> {}
                         (cond-> nimi (assoc :nimi nimi))
                         (cond-> tyyppi (assoc :tyyppi tyyppi))
                         (cond-> opintopolunNayttaminenLoppuu (assoc :opintopolunNayttaminenLoppuu opintopolunNayttaminenLoppuu))
                         (cond-> (not-empty oppiaineet) (assoc :oppiaineet oppiaineet))
                         (assoc :haut haut)
                         (assoc :hakukohteet hakukohteet)
                         (assoc :organisaatio organisaatio))]
      (assoc koulutus :searchData searchData))))
