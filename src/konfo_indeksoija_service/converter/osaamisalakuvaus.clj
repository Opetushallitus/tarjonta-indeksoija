(ns konfo-indeksoija-service.converter.osaamisalakuvaus)

(defn convert
  [dto]
  (assoc dto :docs (map #(assoc %1 :oid (str (:id %1)) :tyyppi "osaamisalakuvaus") (:docs dto))))