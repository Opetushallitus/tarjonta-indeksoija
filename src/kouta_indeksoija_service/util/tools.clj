(ns kouta-indeksoija-service.util.tools)

(defn uuid
  []
  (.toString (java.util.UUID/randomUUID)))