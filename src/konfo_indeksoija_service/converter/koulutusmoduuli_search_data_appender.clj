(ns konfo-indeksoija-service.converter.koulutusmoduuli-search-data-appender
  (:require [konfo-indeksoija-service.tarjonta-client :as tarjonta-client]
            [konfo-indeksoija-service.converter.tyyppi-converter :refer [koulutustyyppi-uri-to-tyyppi]]
            [clojure.tools.logging :as log]))

(defn get-komo-nimi
  [koulutusmoduuli]
  ;fixme ehkä: halutaanko searchdataan nimi jostain muualta?
  (get-in koulutusmoduuli [:koulutuskoodi :nimi]))

(defn- get-komo-tyyppi [koulutusmoduuli]
  ;fixme ehkä: voiko komolla oikeasti olla enemmän kuin yksi koulutustyyppi? Tulee tarjonnasta settinä, mutta käytännössä niitä vaikuttaisi olevan vain yksi.
  (if (> (count (:koulutustyyppis koulutusmoduuli)) 1)
   (log/warn "Koulutusmoduulilla useampia kuin yksi koulutustyyppi!"))
  (koulutustyyppi-uri-to-tyyppi (:uri (first (:koulutustyyppis koulutusmoduuli))))
  )

(defn append-search-data
  [koulutusmoduuli]
  (log/info "Appending search data for komo oid: " (:oid koulutusmoduuli))
  (let [tyyppi (get-komo-tyyppi koulutusmoduuli)]
    (let [searchData (-> {}
                         (assoc :nimi (get-komo-nimi koulutusmoduuli))
                         (assoc :tyyppi tyyppi))]
    (assoc koulutusmoduuli :searchData searchData))
    )
  )
