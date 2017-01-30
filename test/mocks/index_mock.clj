(ns mocks.index-mock
  (:require [tarjonta-indeksoija-service.elastic-client :as elastic-client]))

(defn reindex-mock
  [index params]
  (let [docs {:type "hakukohde" :oid "1.2.246.562.20.28810946823"}]
    (elastic-client/bulk-upsert "indexdata" "indexdata" [docs])))
