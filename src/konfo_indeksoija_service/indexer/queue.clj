(ns konfo-indeksoija-service.indexer.queue
  (:require [konfo-indeksoija-service.elastic.elastic-client :as elastic-client]
            [konfo-indeksoija-service.rest.tarjonta :as tarjonta-client]
            [konfo-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [clojure.tools.logging :as log]))


(defn- find-docs
  [index oid]
  (cond
    (= "organisaatio" index) (organisaatio-client/find-docs oid)
    :else [{:type index :oid oid}]))

(defn queue-all
  []
  (log/info "Tyhjennetään indeksointijono ja uudelleenindeksoidaan kaikki data Tarjonnasta ja organisaatiopalvelusta.")
  (elastic-client/delete-index "indexdata")
  (elastic-client/initialize-indices)
  (let [tarjonta-docs (tarjonta-client/find-all-tarjonta-docs)
        organisaatio-docs (organisaatio-client/find-docs nil)
        docs (clojure.set/union tarjonta-docs organisaatio-docs)]
    (log/info "Saving" (count docs) "items to index-queue" (flatten (for [[k v] (group-by :type docs)] [(count v) k]) ))
    (elastic-client/upsert-indexdata docs)))

(defn queue
  [index oid]
  (let [docs (find-docs index oid)
        related-koulutus (flatten (map tarjonta-client/get-related-koulutus docs))
        docs-with-related-koulutus (clojure.set/union docs related-koulutus)]
    (elastic-client/upsert-indexdata docs-with-related-koulutus)))

(defn empty-queue []
  (let [delete-res (elastic-client/delete-index "indexdata")
        init-res (elastic-client/initialize-indices)]
    { :delete-queue delete-res
     :init-indices init-res }))