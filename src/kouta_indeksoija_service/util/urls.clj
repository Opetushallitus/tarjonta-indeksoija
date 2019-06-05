(ns kouta-indeksoija-service.util.urls
  (:require [kouta-indeksoija-service.util.conf :refer [env]])
  (:import (fi.vm.sade.properties OphProperties)))

(def ^fi.vm.sade.properties.OphProperties url-properties (atom nil))

(defn- load-config
  []
  (let [{:keys [virkailija-internal cas kouta-backend]
         :or {virkailija-internal "" cas "" kouta-backend ""}} (:hosts env)]
    (reset! url-properties
      (doto (OphProperties. (into-array String ["/kouta-indeksoija-oph.properties"]))
              (.addDefault "host-kouta-backend" kouta-backend)
              (.addDefault "host-virkailija-internal" virkailija-internal)
              (.addDefault "host-cas" cas)))))

(defn resolve-url
  [key & params]
  (when (nil? @url-properties)
    (load-config))
  (.url @url-properties (name key) (to-array (or params []))))