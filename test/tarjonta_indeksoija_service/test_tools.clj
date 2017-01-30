(ns tarjonta-indeksoija-service.test-tools
  (:require [cheshire.core :as cheshire]))

(defn parse-body
  [body]
  (:result (cheshire/parse-string (slurp body) true)))
