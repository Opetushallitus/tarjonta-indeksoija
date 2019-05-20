(ns kouta-indeksoija-service.util.time
  (:require [clj-time.format :as format]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]))

(defonce formatter-with-time (format/with-zone (format/formatter "yyyy-MM-dd HH:mm") (time/default-time-zone)))

(defn convert-to-datetime [long] (coerce/from-long long))

(defn format-long [long] (format/unparse formatter-with-time (convert-to-datetime long)))

(defn convert-to-long [datetime] (coerce/to-long datetime))

(defn format-with-time [datetime] (format/unparse formatter-with-time datetime))

(defn format-long-with-time [long] (format-with-time (convert-to-datetime long)))

(defonce formatter-rfc1123 (format/formatter "EEE, dd MMM yyyy HH:mm:ss"))

;purkkaratkaisu, clj-time formatoi time zonen väärin (UTC eikä GMT, https://stackoverflow.com/questions/25658897/is-utc-a-valid-timezone-name-for-rfc-1123-specification)
(defn format-long-to-rfc1123 [long] (str (format/unparse formatter-rfc1123 (convert-to-datetime long)) " GMT"))

(defonce formatter-kouta (format/with-zone (format/formatter "yyyy-MM-dd'T'HH:mm") (time/default-time-zone)))

(defn parse-kouta-string
  [string]
  (format/parse formatter-kouta string))

(defn kouta-date-to-long
  [string]
  (convert-to-long (parse-kouta-string string)))
