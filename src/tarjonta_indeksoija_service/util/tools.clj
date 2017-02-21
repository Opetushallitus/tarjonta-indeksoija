(ns tarjonta-indeksoija-service.util.tools
  (:require [taoensso.timbre :as log]
            [environ.core :as env]
            [slingshot.slingshot :refer [try+]]))

(defmacro with-error-logging-value
  [value & body]
  `(try+
    (do ~@body)
    (catch [:status 500] {:keys [~'trace-redirects]}
      (log/error "HTTP 500 from:" ~'trace-redirects))
    (catch Object ~'_
      (if (Boolean/valueOf (:test ~environ.core/env))
        (log/info "Error during test:" (:message ~'&throw-context))
        ;(log/error ~'e)
        ; ^- during test if you want to see stack trace
        (log/error (:throwable ~'&throw-context)))
      ~value)))

(defmacro with-error-logging
  [& body]
  `(with-error-logging-value nil ~@body))
