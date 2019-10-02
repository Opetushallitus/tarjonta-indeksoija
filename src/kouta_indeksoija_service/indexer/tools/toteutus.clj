(ns kouta-indeksoija-service.indexer.tools.toteutus
  (:require [kouta-indeksoija-service.indexer.kouta.common :as common]))

(defn to-list-item
  [toteutus]
  (-> toteutus
      (select-keys [:oid :organisaatio :nimi :tila :tarjoajat :muokkaaja :modified :organisaatiot])
      (common/assoc-organisaatiot)))