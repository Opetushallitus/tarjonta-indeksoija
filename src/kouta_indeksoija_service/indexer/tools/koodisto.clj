(ns kouta-indeksoija-service.indexer.tools.koodisto
  (:require [kouta-indeksoija-service.rest.koodisto :refer :all]))

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

(defn pohjakoulutusvaatimuskonfo
  []
  (map #(assoc % :alakoodit (list-alakoodit-with-cache (:koodiUri %) "pohjakoulutusvaatimuskouta"))
       (get-koodit-with-cache "pohjakoulutusvaatimuskonfo")))
