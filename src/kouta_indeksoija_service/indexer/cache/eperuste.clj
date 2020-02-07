(ns kouta-indeksoija-service.indexer.cache.eperuste
  (:require [kouta-indeksoija-service.rest.eperuste :refer [get-by-koulutuskoodi]]
            [kouta-indeksoija-service.indexer.tools.organisaatio :refer :all]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [clojure.core.cache :as cache]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [remove-uri-version eperuste-laajuusyksikko->opintojenlaajuusyksikko]]))

(defonce eperuste_cache_time_millis (* 1000 60 20))

(defonce EPERUSTE_CACHE (atom (cache/ttl-cache-factory {} :ttl eperuste_cache_time_millis)))

(defn- strip
  [eperuste]
  (if-let [suoritustapa (some-> eperuste :suoritustavat (first))]
    (let [opintojenlaajuusNumero          (get-in suoritustapa [:rakenne :muodostumisSaanto :laajuus :minimi])
          opintojenlaajuusKoodiUri        (when opintojenlaajuusNumero (str "opintojenlaajuus_" opintojenlaajuusNumero))
          opintojenlaajuus                (when opintojenlaajuusKoodiUri (get-koodi-nimi-with-cache opintojenlaajuusKoodiUri))
          opintojenlaajuusyksikkoKoodiUri (eperuste-laajuusyksikko->opintojenlaajuusyksikko (:laajuusYksikko suoritustapa))
          opintojenlaajuusyksikko         (when opintojenlaajuusyksikkoKoodiUri (get-koodi-nimi-with-cache opintojenlaajuusyksikkoKoodiUri))]
      (cond-> (select-keys eperuste [:id :tutkintonimikkeet :koulutukset])
              (not (nil? opintojenlaajuus))                (assoc :opintojenlaajuus opintojenlaajuus)
              (not (nil? opintojenlaajuusyksikko))         (assoc :opintojenlaajuusyksikko opintojenlaajuusyksikko)))
    (select-keys eperuste [:id :tutkintonimikkeet :koulutukset])))

(defn cache-eperuste
  [koodi]
  (when-let [eperuste (get-by-koulutuskoodi koodi)]
    (let [stripped (strip eperuste)]
      (swap! EPERUSTE_CACHE cache/through-cache koodi (constantly stripped)))))

(defn get-eperuste
  [koulutuskoodiUri]
  (let [koodi (remove-uri-version koulutuskoodiUri)]
    (if-let [eperuste (cache/lookup @EPERUSTE_CACHE koodi)]
      eperuste
      (do (cache-eperuste koodi)
          (cache/lookup @EPERUSTE_CACHE koodi)))))