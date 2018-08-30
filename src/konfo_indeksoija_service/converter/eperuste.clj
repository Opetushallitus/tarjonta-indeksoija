(ns konfo-indeksoija-service.converter.eperuste)

(defn convert
  [dto]
  (assoc dto :oid (str (:id dto))))