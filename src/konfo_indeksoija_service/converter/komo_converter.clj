(ns konfo-indeksoija-service.converter.komo-converter
  (:require [clojure.tools.logging :as log]))

;Tyhjä placeholder toistaiseksi
(defn convert
  [data]
  (log/info "Käsiteltiin komo oidilla " (:oid data))
  data)