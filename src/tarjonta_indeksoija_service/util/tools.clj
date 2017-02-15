(ns tarjonta-indeksoija-service.util.tools
  (require [taoensso.timbre :as log]
           [environ.core :as env]))

(defmacro with-error-logging-value
  [value & body]
  `(try
     (do ~@body)
     (catch Exception ~'e
       (if (Boolean/valueOf (:test ~environ.core/env))
         (log/info "Error during test:" (.getMessage ~'e))
         ;; (log/error ~'e) during test if you want to see stack trace
         (log/error ~'e))
       ~value)))

(defmacro with-error-logging
  [& body]
  `(with-error-logging-value nil ~@body))
