(ns kouta-indeksoija-service.search-data.koulutusmoduuli
  (:require [kouta-indeksoija-service.converter.tyyppi :refer [koulutustyyppi-uri-to-tyyppi]]
            [clojure.tools.logging :as log]))

(defn get-nimi
  [koulutusmoduuli]
  ;fixme ehkä: halutaanko searchdataan nimi jostain muualta?
  (get-in koulutusmoduuli [:koulutuskoodi :nimi]))

(defn- get-tyyppi [koulutusmoduuli]
  ;Tulee tarjonnasta settinä, mutta käytännössä niitä vaikuttaisi olevan vain yksi. Onko data järjellistä jos niitä on monta?
  (if (> (count (:koulutustyyppis koulutusmoduuli)) 1)
   (log/warn "Koulutusmoduulilla useampia kuin yksi koulutustyyppi!"))
  (koulutustyyppi-uri-to-tyyppi (:uri (first (:koulutustyyppis koulutusmoduuli))))
  )

(defn append-search-data
  [koulutusmoduuli]
  (let [tyyppi (get-tyyppi koulutusmoduuli)]
    (let [searchData (-> {}
                         (assoc :nimi (get-nimi koulutusmoduuli))
                         (assoc :tyyppi tyyppi))]
    (assoc koulutusmoduuli :searchData searchData))))
