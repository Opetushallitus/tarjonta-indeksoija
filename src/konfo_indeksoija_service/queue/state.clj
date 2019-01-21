(ns konfo-indeksoija-service.queue.state)

(defn set
  [state, message]
  ;; TODO Mark in db: timestamp, status = <status>
  )

(defn set-all [state messages] (doseq [msg messages] (set state msg)))
