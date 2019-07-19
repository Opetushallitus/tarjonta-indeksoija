(ns kouta-indeksoija-service.util.conf
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [defstate start]]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojure.string :refer [blank?]]))

(defonce env (load-config :merge [(source/from-system-props) (source/from-env)]))



(defn- ->not-blank
  [s]
  (if (not (blank? s))
    s))

(defonce sqs-endpoint (or (->not-blank (:sqs-endpoint env)) (->not-blank (:sqs-region env))))
