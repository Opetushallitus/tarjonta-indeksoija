(ns konfo-indeksoija-service.util.time
  (:require [clj-time.format :as format]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]))

(defonce formatter (format/formatter "yyyy-MM-dd"))

(defonce formatter-with-time (format/with-zone (format/formatter "yyyy-MM-dd HH:mm") (time/default-time-zone)))

(defn convert-to-datetime [long] (coerce/from-long long))

(defn add-months [datetime months] (time/plus datetime (time/months months)))

(defn format [datetime] (format/unparse formatter datetime))

(defn format-long [long] (format/unparse formatter-with-time (convert-to-datetime long)))

(defn convert-to-long [datetime] (coerce/to-long datetime))

(defn parse-with-time [string] (format/parse formatter-with-time string))

(defn format-with-time [datetime] (format/unparse formatter-with-time datetime))

(defn format-long-with-time [long] (format-with-time (convert-to-datetime long)))