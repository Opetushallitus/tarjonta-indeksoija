(ns kouta-indeksoija-service.util.time
  (:require [clj-time.format :as format]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]))

(defonce formatter-with-time (format/with-zone (format/formatter "yyyy-MM-dd HH:mm") (time/default-time-zone)))

(defonce formatter-rfc1123 (format/formatter "EEE, dd MMM yyyy HH:mm:ss"))

(defn long->date-time
  [long]
  (coerce/from-long long))

(defn date-time->date-time-string
  [datetime]
  (format/unparse formatter-with-time datetime))

(defn long->date-time-string
  [long]
  (date-time->date-time-string (long->date-time long)))

;purkkaratkaisu, clj-time formatoi time zonen väärin (UTC eikä GMT, https://stackoverflow.com/questions/25658897/is-utc-a-valid-timezone-name-for-rfc-1123-specification)
(defn long->rfc1123
  [long]
  (str (format/unparse formatter-rfc1123 (long->date-time long)) " GMT"))