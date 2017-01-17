(ns tarjonta-indeksoija-service.conf
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [defstate start]]))

(defstate env :start (load-config :merge [(source/from-system-props) (source/from-env)]))
