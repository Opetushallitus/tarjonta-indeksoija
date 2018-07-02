(ns konfo-indeksoija-service.converter.komo-converter
  (:require [clojure.tools.logging :as log]))

(defn- extract-koodi
  [value]
  {:uri (:uri value)
   :nimi {:kieli_fi (get-in value [:meta :kieli_fi :nimi])
          :kieli_sv (get-in value [:meta :kieli_sv :nimi])
          :kieli_en (get-in value [:meta :kieli_en :nimi])}})

(defn- extract-koodi-list
  [value path-to-koodi-list]
  (map extract-koodi (vals (get-in value path-to-koodi-list))))

(defn- value [value] value)

(defn- koodi [value] (extract-koodi value))

(defn- koodi-list [value] (extract-koodi-list value [:meta]))

(defn- kuvaus
  [value]
  (reduce-kv #(assoc %1 %2 (:tekstis %3))
             {}
             value))

(def map-komo-fields {:tila value
                      :eqf koodi
                      :lukiolinja value
                      :koulutustyyppis koodi-list
                      :koulutusasteTyyppi value
                      :osaamisala value
                      :oppilaitostyyppis value
                      :ohjelmas value
                      :opintojenLaajuusyksikko koodi
                      :koulutusala koodi
                      :koulutusohjelma koodi
                      :koulutusaste koodi
                      :modified value
                      :koulutuskoodi koodi
                      :kuvausKomo kuvaus
                      :opintoala koodi
                      :nimi value
                      :oid value
                      :nqf koodi
                      :koulutusmoduuliTyyppi value
                      :tutkintonimikes koodi-list
                      :tutkinto koodi
                      :komoOid value
                      :tunniste value
                      :version value
                      :organisaatio value
                      :opintojenLaajuusarvo koodi
})

(defn convert
  [dto]
  ;(log/info "Raw keys: " (keys dto) ", count: " (count (keys dto)))
  ;(log/info "Before: " dto)
  (let [raw-res (into {} (for [[k v] dto] [k ((k map-komo-fields) v)]))]
    ;(log/info "After: " raw-res)
    raw-res)
  )
