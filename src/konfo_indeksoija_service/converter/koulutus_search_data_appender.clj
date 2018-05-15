(ns konfo-indeksoija-service.converter.koulutus-search-data-appender
  (:require [konfo-indeksoija-service.tarjonta-client :as tarjonta-client]
            [konfo-indeksoija-service.organisaatio-client :as organisaatio-client]
            [clj-time.format :as format]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]))

(defn- fix-nimi-keys [json]
  (assoc (dissoc json :nimi) :nimi (clojure.set/rename-keys (:nimi json) {:fi :kieli_fi :en :kieli_en :sv :kieli_sv})))

(defonce formatter (format/formatter "yyyy-MM-dd"))
(defn- convert-to-datetime [l] (coerce/from-long l))
(defn- add-ten-months [dt] (time/plus dt (time/months 10)))
(defn- format [dt] (format/unparse formatter dt))
(defn- loppuPvm-to-opintopolkuPvm [loppuPvm]
  (format (add-ten-months (convert-to-datetime loppuPvm))))

(defn count-opintopolun-nayttaminen-loppuu [haut]           ;TODO -> haku, jolla ei ole loppuPvm:ää?
  (if-let [opintopolunNayttaminenLoppuu (last (sort (map #(:opintopolunNayttaminenLoppuu %) haut)))]
    opintopolunNayttaminenLoppuu
    (if-let [hakuajat (not-empty (apply concat (map #(:hakuaikas %) haut)))]
      (if-let [loppuPvms (not-empty (remove nil? (map #(:loppuPvm %) hakuajat)))]
        (if-let [maxLoppuPvm (apply max loppuPvms)]
          (loppuPvm-to-opintopolkuPvm maxLoppuPvm))))))

(defn find-koulutus-nimi [koulutus hakukohteet]
  (if-let [koulutuskoodi (:koulutuskoodi koulutus)]
    (:nimi koulutuskoodi)
    (if-let [hakukohde (first hakukohteet)]                   ;TODO -> miltä hakukohteelta haetaan?
      (:nimi hakukohde))))

(defn append-search-data
  [koulutus]
  (let [hakukohteet-raw (tarjonta-client/get-hakukohteet-for-koulutus (:oid koulutus))
        hakukohteet (doall (map #(fix-nimi-keys (clojure.set/rename-keys % {:relatedOid :hakuOid})) hakukohteet-raw))
        haut-raw (tarjonta-client/get-haut-by-oids (distinct (map :hakuOid hakukohteet)))
        haut (doall (map #(dissoc % :maxHakukohdes :hakukohdeOids :canSubmitMultipleApplications :sisaltyvatHaut
                                  :tunnistusKaytossa :modified :autosyncTarjonta :usePriority :hakukohdeOidsYlioppilastutkintoAntaaHakukelpoisuuden
                                  :yhdenPaikanSaanto :modifiedBy :sijoittelu :autosyncTarjontaFrom :autosyncTarjontaTo :ylioppilastutkintoAntaaHakukelpoisuuden) haut-raw))
        organisaatio-raw (organisaatio-client/get-doc (assoc (:organisaatio koulutus) :type "organisaatio") false)
        organisaatio (fix-nimi-keys (select-keys organisaatio-raw [:nimi :oid :status :kotipaikkaUri :alkuPvm :loppuPvm :parentOidPath]))
        nimi (find-koulutus-nimi koulutus hakukohteet)
        opintopolunNayttaminenLoppuu (count-opintopolun-nayttaminen-loppuu haut)]
    (let [searchData (-> {}
                         (cond-> nimi (assoc :nimi nimi))
                         (cond-> opintopolunNayttaminenLoppuu (assoc :opintopolunNayttaminenLoppuu opintopolunNayttaminenLoppuu))
                         (assoc :haut haut)
                         (assoc :hakukohteet hakukohteet)
                         (assoc :organisaatio organisaatio))]
      (assoc koulutus :searchData searchData))))