(ns kouta-indeksoija-service.util.conf
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [defstate start]]
            [clojurewerkz.quartzite.scheduler :as qs]))

(defstate env :start (load-config :merge [(source/from-system-props) (source/from-env)]))

(defstate job-pool :start (qs/start (qs/initialize)))

(defn- ->not-blank
  [s]
  (if (not (clojure.string/blank? s))
    s))

(defstate sqs-endpoint :start (or (->not-blank (:sqs-endpoint env)) (->not-blank (:sqs-region env))))
