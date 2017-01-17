(ns tarjonta-indeksoija-service.elastic-client
  (:require [tarjonta-indeksoija-service.conf :refer [env elastic-port]]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest.response :as esrsp]))

(defn elastic-url
  []
  (str (:elastic-url env) ":" @elastic-port))

(defn query
  [index mapping-type & params]
  (let [conn (esr/connect (elastic-url))
        res (esd/search conn index mapping-type :query (apply q/terms params))]
    (esrsp/hits-from res)))

(defn index
  [index mapping-type doc & options]
  (let [conn (esr/connect (elastic-url))]
    (apply esd/create conn index mapping-type doc options)))
