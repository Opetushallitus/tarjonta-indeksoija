(ns konfo-indeksoija-service.converter.koulutusmoduuli-search-data-appender
  (:require [konfo-indeksoija-service.rest.tarjonta :as tarjonta-client]
            [konfo-indeksoija-service.converter.tyyppi-converter :refer [koulutustyyppi-uri-to-tyyppi]]
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
  ;(log/info "Appending search data for komo oid: " (:oid koulutusmoduuli))
  (let [tyyppi (get-tyyppi koulutusmoduuli)]
    (let [searchData (-> {}
                         (assoc :nimi (get-nimi koulutusmoduuli))
                         (assoc :tyyppi tyyppi))]
      ;(log/info "Searchdata for komo " (:oid koulutusmoduuli) ": " searchData)
    (assoc koulutusmoduuli :searchData searchData))
    )
  )
