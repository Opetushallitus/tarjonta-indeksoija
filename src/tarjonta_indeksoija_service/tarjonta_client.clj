(ns tarjonta-indeksoija-service.tarjonta-client
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [clj-http.client :as client]))

(defn get-url
  [obj]
  (condp = (:type obj)
    "hakukohde" (str (:tarjonta-service-url env) "hakukohde/" (:oid obj))
    "koulutus" nil
    "haku" nil))

(defn get-doc
  [obj]
  (let [url (get-url obj)]
  (-> (client/get url {:as :json})
      :body
      :result)))
