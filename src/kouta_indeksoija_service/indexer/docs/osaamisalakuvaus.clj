(ns kouta-indeksoija-service.indexer.docs.osaamisalakuvaus)

(defn convert
  [dto]
  (map #(assoc %1 :oid (str (:id %1)) :tyyppi "osaamisalakuvaus") (:docs dto)))
