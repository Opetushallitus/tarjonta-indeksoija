(ns tarjonta-indeksoija-service.tarjonta-client
  (require [clj-http.client :as client]))

(defn get-koulutus
  [oid]
  (-> (str "https://testi.virkailija.opintopolku.fi:443/tarjonta-service/rest/v1/hakukohde/" oid)
      (client/get {:as :json})
      :body
      :result))
