(ns konfo-indeksoija-service.converter.koulutus-search-data-appender
  (:require [konfo-indeksoija-service.tarjonta-client :as tarjonta-client]
            [konfo-indeksoija-service.organisaatio-client :as organisaatio-client]
            [konfo-indeksoija-service.converter.tyyppi-converter :refer [koulutustyyppi-uri-to-tyyppi]]
            [clojure.tools.logging :as log]
            [clj-time.format :as format]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]))

(defn- fix-nimi-keys [json]
  (assoc (dissoc json :nimi) :nimi (clojure.set/rename-keys (:nimi json) {:fi :kieli_fi :en :kieli_en :sv :kieli_sv})))

(defonce formatter (format/formatter "yyyy-MM-dd"))
(defn- convert-to-datetime [l] (coerce/from-long l))
(defn- add-six-months [dt] (time/plus dt (time/months 6)))
(defn- format [dt] (format/unparse formatter dt))
(defn- loppuPvm-to-opintopolkuPvm [loppuPvm]
  (format (add-six-months (convert-to-datetime loppuPvm))))

(defonce formatter-with-time (format/with-zone (format/formatter "yyyy-MM-dd HH:mm") (time/default-time-zone)))
(defn- convert-to-long [dt] (coerce/to-long dt))
(defn- parse [s] (format/parse formatter-with-time s))

(defn parse-hakuaika-ryhma [hakuaika-ryhma]
  (if (not (nil? hakuaika-ryhma))
    (let [hakuajat (re-seq #"\d{4}\-\d{2}\-\d{2}\s\d{2}\:\d{2}" hakuaika-ryhma)
          alkupvm (if-let [s (nth hakuajat 0 nil)] (convert-to-long (parse s)))
          loppupvm (if-let [s (nth hakuajat 1 nil)] (convert-to-long (parse s)))]
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
          (:nimi hakukohde))
      ))
    ))

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
    ;(if (empty? hakukohteet) (log/warn (str "Koulutukselle " (:oid koulutus) " ei löytynyt hakukohteita!")))
    ;(if (empty? haut) (log/warn (str "Koulutukselle " (:oid koulutus) " ei löytynyt hakuja!")))
    (let [searchData (-> {}
                         (cond-> nimi (assoc :nimi nimi))
                         (cond-> tyyppi (assoc :tyyppi tyyppi))
                         (cond-> opintopolunNayttaminenLoppuu (assoc :opintopolunNayttaminenLoppuu opintopolunNayttaminenLoppuu))
                         (cond-> (not-empty oppiaineet) (assoc :oppiaineet oppiaineet))
                         (assoc :haut haut)
                         (assoc :hakukohteet hakukohteet)
                         (assoc :organisaatio organisaatio))]
      (assoc koulutus :searchData searchData))))