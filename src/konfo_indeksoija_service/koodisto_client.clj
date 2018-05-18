(ns konfo-indeksoija-service.koodisto-client
  (:require [konfo-indeksoija-service.conf :refer [env]]
            [clj-log.error-log :refer [with-error-logging]]
            [clj-http.client :as client]))

(defn get-koodi
  [koodisto koodi-uri]
  (with-error-logging
   (let [url (str (:koodisto-service-url env) koodisto "/koodi/" koodi-uri)]
     (:body (client/get url {:as :json})))))