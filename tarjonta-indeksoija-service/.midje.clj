(require '[mount.core :as mount]
         '[tarjonta-indeksoija-service.embedded-elastic :as es])
(mount/start)

(when (running-in-repl?)
  (do (change-defaults :print-level :print-namespaces)
      (let [port (es/get-embedded-elastic)]
        (mount/start-with-states {#'tarjonta-indeksoija-service.conf/elastic-port {:start #(port)
                                                                                   :stop #()}}))))