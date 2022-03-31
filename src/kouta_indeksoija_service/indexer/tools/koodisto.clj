(ns kouta-indeksoija-service.indexer.tools.koodisto
  (:require [kouta-indeksoija-service.rest.koodisto :refer :all]
            [clojure.string]
            [kouta-indeksoija-service.util.time :refer [date-is-before-now?]]))

(defonce koodiuri-yhteishaku-hakutapa "hakutapa_01")

(defonce koodiuri-opintopiste-laajuusyksikko "opintojenlaajuusyksikko_2#1")

(defonce koodiuri-osaamispiste-laajuusyksikko "opintojenlaajuusyksikko_6#1")

(defonce koodiuri-viikko-laajuusyksikko "opintojenlaajuusyksikko_8#1")

(defonce koodiuri-ylioppilas-tutkintonimike "tutkintonimikkeet_00001#1")

(defonce koodiuri-koulutusalataso1 "kansallinenkoulutusluokitus2016koulutusalataso1")

(defonce koodiuri-koulutusalataso2 "kansallinenkoulutusluokitus2016koulutusalataso2")

(defn paikkakunta
  [kuntaKoodiUri]
  (get-koodi-nimi-with-cache "kunta" kuntaKoodiUri))

(defn maakunta
  [kuntaKoodiUri]
  (get-alakoodi-nimi-with-cache kuntaKoodiUri "maakunta"))

(defn tutkintonimikkeet
  [koulutusKoodiUri]
  (list-alakoodi-nimet-with-cache koulutusKoodiUri "tutkintonimikkeet"))

(defn opintojen-laajuus
  [koulutusKoodiUri]
  (get-alakoodi-nimi-with-cache koulutusKoodiUri "opintojenlaajuus"))

(defn opintojen-laajuusyksikko
  [koulutusKoodiUri]
  (get-alakoodi-nimi-with-cache koulutusKoodiUri "opintojenlaajuusyksikko"))

(defn koulutusalat-taso1
  [koulutusKoodiUri]
  (list-alakoodi-nimet-with-cache koulutusKoodiUri "kansallinenkoulutusluokitus2016koulutusalataso1"))

(defn koulutusalat-taso2
  [koulutusKoodiUri]
  (list-alakoodi-nimet-with-cache koulutusKoodiUri "kansallinenkoulutusluokitus2016koulutusalataso2"))

(defn koulutustyypit
  [koulutusKoodiUri]
  (list-alakoodi-nimet-with-cache koulutusKoodiUri "koulutustyyppi"))

(defn tutkintotyypit
  [koulutusKoodiUri]
  (list-alakoodi-nimet-with-cache koulutusKoodiUri "tutkintotyyppi"))

;eiharkinnanvaraisuutta koodin kuvaus: "Koulutukselta ei kysytä harkinnanvaraisuutta (ei siis suodatu hakukohderyhmäpalvelun harkinnanvarainen valinta-listaukseen), jos sillä on pääsykoe."
(defn harkinnanvaraisuutta-ei-kysyta-lomakkeella
  [koulutusKoodiUri]
  (let [asetukset (list-alakoodi-nimet-with-cache koulutusKoodiUri "hakulomakkeenasetukset")]
    (some #(= "hakulomakkeenasetukset_eiharkinnanvaraisuutta" (get % :koodiUri)) asetukset)))

(defn pohjakoulutusvaatimuskonfo
  []
  (map #(assoc % :alakoodit (list-alakoodit-with-cache (:koodiUri %) "pohjakoulutusvaatimuskouta"))
       (get-koodit-with-cache "pohjakoulutusvaatimuskonfo")))

(defn assoc-hakukohde-nimi-from-koodi [hakukohde]
  (let [hakukohde-koodi-uri (:hakukohdeKoodiUri hakukohde)
        hakukohde-koodi-nimi (when-not (clojure.string/blank? hakukohde-koodi-uri)
                               (get-koodi-nimi-with-cache hakukohde-koodi-uri))]
    (if (nil? hakukohde-koodi-nimi) hakukohde (assoc hakukohde :nimi (:nimi hakukohde-koodi-nimi)))))

(defn filter-expired [koodit]
  (filter (fn [koodi]
            (not (if-let [loppu (:voimassaLoppuPvm koodi)]
                   (date-is-before-now? loppu)))) koodit))
