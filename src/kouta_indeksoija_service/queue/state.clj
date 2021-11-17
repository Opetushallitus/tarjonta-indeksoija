(ns kouta-indeksoija-service.queue.state
  (:require [clojure.tools.logging :as log]))

(defn set-state!
  [state, message execution-id]
  ;; TODO Mark in db: timestamp, status = <status>
  (log/info (str "Setting SQS Queue message state: " state " " message " ID: " (vec (flatten execution-id)))))


(defn set-states!
  [state messages execution-id]
  (doseq [msg messages] (set-state! state msg execution-id)))
