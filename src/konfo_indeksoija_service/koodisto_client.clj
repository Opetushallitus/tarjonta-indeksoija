(ns konfo-indeksoija-service.koodisto-client
  (:require [konfo-indeksoija-service.conf :refer [env]]
            [clj-log.error-log :refer [with-error-logging]]
            [konfo-indeksoija-service.konfo-indeksoija-service.rest-wrapper :as client]
            [clojure.tools.logging :as log]
            [clojure.core.memoize :as memo]))

(defn get-koodi
  [koodisto koodi-uri]
  (with-error-logging
   (let [url (str (:koodisto-service-url env) koodisto "/koodi/" koodi-uri)
         res (client/get url {:as :json})
         body (:body res)
         status (:status res)]
     (if (not (= status 200))
       (throw (RuntimeException.)))
     body)))

(def get-koodi-with-cache
  (memo/ttl get-koodi {} :ttl/threshold 86400000)) ;24 tunnin cache