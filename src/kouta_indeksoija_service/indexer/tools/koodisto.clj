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

(defn opintojenlaajuus
  [koulutusKoodiUri]
  (get-alakoodi-nimi-with-cache koulutusKoodiUri "opintojenlaajuus"))

(defn opintojenlaajuusyksikko
  [koulutusKoodiUri]
  (get-alakoodi-nimi-with-cache koulutusKoodiUri "opintojenlaajuusyksikko"))

(defn koulutusalat
  [koulutusKoodiUri]
  (list-alakoodi-nimet-with-cache koulutusKoodiUri "kansallinenkoulutusluokitus2016koulutusalataso1"))