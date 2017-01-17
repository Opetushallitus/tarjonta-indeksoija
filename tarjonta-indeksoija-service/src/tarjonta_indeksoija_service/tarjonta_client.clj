(ns tarjonta-indeksoija-service.tarjonta-client
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [clj-http.client :as client]))

(defn get-koulutus
  [oid]
  (-> (str (:tarjonta-hakukohde-url env) oid)
      (client/get {:as :json})
      :body
      :result))
