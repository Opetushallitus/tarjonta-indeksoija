(ns kouta-indeksoija-service.util.time
  (:require [clj-time.format :as format]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]))

(defonce formatter-with-seconds (format/with-zone (format/formatter "yyyy-MM-dd'T'HH:mm:ss") (time/time-zone-for-id "EET")))

(defonce formatter-with-time (format/with-zone (format/formatter "yyyy-MM-dd HH:mm") (time/default-time-zone)))

(defonce formatter-rfc1123 (format/formatter "EEE, dd MMM yyyy HH:mm:ss"))

(defn long->date-time
  [long]
  (coerce/from-long long))

(defn date-time->date-time-string
  ([datetime formatter]
   (format/unparse formatter datetime))
  ([datetime]
   (date-time->date-time-string datetime formatter-with-time)))

(defn long->date-time-string
  ([long]
   (date-time->date-time-string (long->date-time long)))
  ([long formatter]
   (date-time->date-time-string (long->date-time long) formatter)))

(defn long->indexed-date-time
  [long]
  (long->date-time-string long formatter-with-seconds))

;purkkaratkaisu, clj-time formatoi time zonen väärin (UTC eikä GMT, https://stackoverflow.com/questions/25658897/is-utc-a-valid-timezone-name-for-rfc-1123-specification)
(defn long->rfc1123
  [long]
  (str (format/unparse formatter-rfc1123 (long->date-time long)) " GMT"))