(ns tarjonta-indeksoija-service.conf
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [defstate start]]
            [overtone.at-at :as at]))

(defstate env :start (load-config :merge [(source/from-system-props) (source/from-env)]))

(defstate recur-pool :start (at/mk-pool))
