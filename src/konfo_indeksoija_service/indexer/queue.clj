(ns konfo-indeksoija-service.indexer.queue
  (:require [konfo-indeksoija-service.elastic.queue :refer [reset-queue upsert-to-queue]]
            [konfo-indeksoija-service.rest.tarjonta :as tarjonta-client]
            [konfo-indeksoija-service.rest.kouta :as kouta-client]
            [konfo-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [konfo-indeksoija-service.rest.eperuste :as eperusteet-client]
            [konfo-indeksoija-service.util.time :refer [format-long-to-rfc1123]]
            [konfo-indeksoija-service.indexer.index :refer [index-kouta-koulutus]]
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
        docs (clojure.set/union tarjonta-docs organisaatio-docs eperusteet-docs)]
    (log/info "Saving" (count docs) "items to index-queue" (flatten (for [[k v] (group-by :type docs)] [(count v) k]) ))
    (upsert-to-queue docs)))

(defn queue-kouta
  [since]
  (log/info (str "Indeksoidaan data Kouta-backendistä " since))
  (let [date (format-long-to-rfc1123 since)
        oids (kouta-client/get-last-modified date)]
    ;(log/info "Saving" (count oids) "items to index-queue" (flatten (for [[k v] (group-by :type oids)] [(count v) k]) ))
    ;(upsert-to-queue oids))
    (map index-kouta-koulutus (:koulutukset oids))))


(defn queue
  [index oid]
  (let [docs (find-docs index oid)
        related-koulutus (flatten (map tarjonta-client/get-related-koulutus docs))
        docs-with-related-koulutus (remove nil? (clojure.set/union docs related-koulutus))]
    (println docs-with-related-koulutus)
    (upsert-to-queue docs-with-related-koulutus)))

(defn empty-queue []
  (reset-queue))