(ns tarjonta-indeksoija-service.util.tools
  (require [taoensso.timbre :as log]))

(defmacro with-error-logging
  [& body]
  `(try
     (do ~@body)
     (catch Exception ~'e (log/error ~'e))))