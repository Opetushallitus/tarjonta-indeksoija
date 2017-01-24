(ns tarjonta-indeksoija-service.util.logging
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rolling :refer [rolling-appender]]
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
  (timbre/error e))

(defn get-rolling-appender
  [name file-name]
  {(keyword name) (assoc (rolling-appender {:path (str (logs-path) "/" file-name)
                                                               :pattern :daily})
                                       :timestamp-opts {:pattern "yyyy-MM-dd'T'HH:mm:ss.SSSX"
                                                        :locale (Locale. "fi")
                                                        :timezone (TimeZone/getTimeZone "Europe/Helsinki")})})

(defn logging-config []
  {:level :info
   :appenders (get-rolling-appender "rolling-application-log-appender" "oph-tarjonta-indeksoija.log")})
