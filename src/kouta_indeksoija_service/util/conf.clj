(ns kouta-indeksoija-service.util.conf
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [defstate start]]
            [clojurewerkz.quartzite.scheduler :as qs]))

(defonce env (load-config :merge [(source/from-system-props) (source/from-env)]))