(ns kouta-indeksoija-service.converter.eperuste)

(defn convert
  [dto]
  (assoc dto :oid (str (:id dto)) :tyyppi "eperuste"))      ;TODO Tyyppi on huono parametri, koska se ylikirjoittaa eperusteen oman tyypin
