(ns konfo-indeksoija-service.queue.state)

(defn set-state!
  [state, message])
  ;; TODO Mark in db: timestamp, status = <status>


(defn set-states! [state messages] (doseq [msg messages] (set-state! state msg)))
