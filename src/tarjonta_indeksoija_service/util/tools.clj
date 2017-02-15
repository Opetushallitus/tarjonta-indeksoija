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

(def lastindex-lock? (atom false :error-handler #(log/error %)))
(reset! lastindex-lock? false)

(defmacro wait-elastic-lock
  [& body]
  `(while (not (compare-and-set! lastindex-lock? false true))
     (Thread/sleep 100)
     (try
       (do ~@body)
       (finally (reset! lastindex-lock? false)))))