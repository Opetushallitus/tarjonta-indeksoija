(ns kouta-indeksoija-service.util.urls
  (:require [kouta-indeksoija-service.util.conf :refer [env]])
  (:import (fi.vm.sade.properties OphProperties)))

(def ^fi.vm.sade.properties.OphProperties url-properties (atom nil))

(defn- load-config
  []
  (let [{:keys [virkailija-internal cas kouta-backend kouta-external notifier-target]
         :or {virkailija-internal "" cas "" kouta-backend "" kouta-external ""}} (:hosts env)]
    (reset! url-properties
      (doto (OphProperties. (into-array String ["/kouta-indeksoija-oph.properties"]))
              (.addDefault "host-kouta-backend" kouta-backend)
              (.addDefault "host-kouta-external" kouta-external)
              (.addDefault "host-virkailija-internal" virkailija-internal)
              (.addDefault "host-notifier-target" notifier-target)
              (.addDefault "host-cas" cas)))))

(defn resolve-url
  [key & params]
  (when (nil? @url-properties)
    (load-config))
  (.url @url-properties (name key) (to-array (or params []))))
