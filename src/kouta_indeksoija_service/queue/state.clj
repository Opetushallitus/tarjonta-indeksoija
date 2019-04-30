(ns kouta-indeksoija-service.queue.state
  (:require [clojure.tools.logging :as log]))

(defn set-state!
  [state, message]
  ;; TODO Mark in db: timestamp, status = <status>
  (log/info (str "set-state! " state " " message)))


(defn set-states!
  [state messages]
  (doseq [msg messages] (set-state! state msg)))
