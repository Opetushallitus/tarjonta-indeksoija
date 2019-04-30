(ns kouta-indeksoija-service.util.logging
  (:require [clojure.tools.logging :as log]))

(defn to-date-string [timestamp]
  (def date (.format (java.text.SimpleDateFormat."HH:mm:ss 'on' dd-MM-yyyy") timestamp))
  (pr-str date))

(defn error-handler*
  [^Exception e data request]
  (log/error e))
