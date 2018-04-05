(ns konfo-indeksoija-service.util.logging
  (:require [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [clojure.java.io :as io])
  (:import (java.util Locale TimeZone)))

(defn logs-path
  "Assumes that in production logs folder is in user.home/logs
  and in development uses project root (or where the jar is run)."
  []
  (let [app-home (:user-home env)]
    (if (:dev env)
      (if (.isDirectory (io/file "./logs"))
        "./logs"
        (if (.mkdir (io/file "./logs"))
          "./logs"
          (throw (IllegalStateException. "Could not create local logs directory"))))
      (if (.isDirectory (io/file (str app-home "/logs")))
        (str app-home "/logs")
        (throw (IllegalStateException. "Could not determine logs directory"))))))

(defn error-handler*
  [^Exception e data request]
  (log/error e))
