(ns user
  (:require [clojure.tools.namespace.repl :as tn]
            [tarjonta-indeksoija-service.conf]
            [mount.core :as mount]))

(defn start []
  (mount/start))

(defn stop []
  (mount/stop))

(defn go []
  (start)
  :ready)

(defn reset []
  (stop)
  (tn/refresh :after 'user/go))
