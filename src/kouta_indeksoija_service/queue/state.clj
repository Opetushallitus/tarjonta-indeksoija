(ns kouta-indeksoija-service.queue.state
  (:require [clojure.tools.logging :as log]))

(defn set-state!
  [state, message execution-id]
  ;; TODO Mark in db: timestamp, status = <status>
  (log/info (str "ID: " execution-id " Setting SQS Queue message state: " state " " message ".")))


(defn set-states!
  [state messages execution-id]
  (doseq [msg messages] (set-state! state msg execution-id)))
