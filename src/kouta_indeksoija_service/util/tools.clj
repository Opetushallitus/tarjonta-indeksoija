(ns kouta-indeksoija-service.util.tools)

(defn uuid
  []
  (.toString (java.util.UUID/randomUUID)))

(defn get-id
  [doc]
  (or (:oid doc) (:id doc)))
