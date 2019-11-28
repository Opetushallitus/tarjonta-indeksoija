(ns kouta-indeksoija-service.indexer.indexable
  (:require [kouta-indeksoija-service.elastic.tools :as tools]
            [clojure.tools.logging :as log]))

(defn- upsert-index
  [index-name docs]
  (when (not-empty docs)
    (tools/upsert-docs index-name docs)))

(defn- eat-and-log-errors
  [oid f]
  (try (f oid)
     (catch Exception e
       (log/error e "Indeksoinnissa " oid " tapahtui virhe.")
       nil)))

(defn do-index-all
  [oids f]
  (doall (pmap #(eat-and-log-errors % f) oids)))

(defn do-index
  [index-name oids f]
  (when-not (empty? oids)
    (log/info (str "Indeksoidaan " (count oids) " indeksiin " index-name))
    (let [start (. System (currentTimeMillis))
          docs (remove nil? (do-index-all oids f))]
      (upsert-index index-name docs)
      (log/info (str "Indeksointi " index-name " kesti " (- (. System (currentTimeMillis)) start) " ms."))
      docs)))

(defn get
  [index-name oid]
  (tools/get-doc index-name oid))