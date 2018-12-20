(ns konfo-indeksoija-service.indexer.queue
  (:require [konfo-indeksoija-service.elastic.queue :refer [reset-queue upsert-to-queue]]
            [konfo-indeksoija-service.rest.tarjonta :as tarjonta-client]
            [konfo-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [konfo-indeksoija-service.rest.eperuste :as eperusteet-client]
            [clojure.tools.logging :as log]))


(defn- find-docs
  [index oid]
  (cond
    (= "organisaatio" index) (organisaatio-client/find-docs oid)
    :else [{:type index :oid oid}]))

(defn queue-all
  []
  (log/info "Tyhjennetään indeksointijono ja uudelleenindeksoidaan kaikki data Tarjonnasta ja organisaatiopalvelusta.")
  (reset-queue)
  (let [tarjonta-docs (tarjonta-client/find-all-tarjonta-docs)
        organisaatio-docs (organisaatio-client/find-docs nil)
        eperusteet-docs (eperusteet-client/find-all)
        osaamisalakuvaus-docs (map #(assoc %1 :type "osaamisalakuvaus") eperusteet-docs)
        docs (clojure.set/union tarjonta-docs organisaatio-docs eperusteet-docs osaamisalakuvaus-docs)]
    (log/info "Saving" (count docs) "items to index-queue" (flatten (for [[k v] (group-by :type docs)] [(count v) k]) ))
    (upsert-to-queue docs)))

(defn queue
  [index oid]
  (let [docs (find-docs index oid)
        related-koulutus (flatten (map tarjonta-client/get-related-koulutus docs))
        docs-with-related-koulutus (remove nil? (clojure.set/union docs related-koulutus))]
    (println docs-with-related-koulutus)
    (upsert-to-queue docs-with-related-koulutus)))

(defn empty-queue []
  (reset-queue))