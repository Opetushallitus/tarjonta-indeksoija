(ns kouta-indeksoija-service.converter.common)

(defn extract-koodi
  [value]
  {:uri (:uri value)
   :nimi {:kieli_fi (get-in value [:meta :kieli_fi :nimi])
          :kieli_sv (get-in value [:meta :kieli_sv :nimi])
          :kieli_en (get-in value [:meta :kieli_en :nimi])}})

(defn extract-koodi-list
  [value path-to-koodi-list]
  (map extract-koodi (vals (get-in value path-to-koodi-list))))

(defn value [value] value)

(defn koodi [value] (extract-koodi value))

(defn koodi-list [value] (extract-koodi-list value [:meta]))

(defn kuvaus
  [value]
  (reduce-kv #(assoc %1 %2 (:tekstis %3))
             {}
             value))
