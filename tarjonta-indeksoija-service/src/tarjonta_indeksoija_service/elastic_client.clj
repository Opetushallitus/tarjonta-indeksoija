(ns tarjonta-indeksoija-service.elastic-client
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest.response :as esrsp]
            [clojure.pprint :as pp]
            [clojurewerkz.elastisch.rest.index :as esi]))

(defn query
  [index mapping-type & params]
  (let [conn (esr/connect (:elastic-url env))
        res (esd/search conn index mapping-type :query (apply q/terms params))]
    (esrsp/hits-from res)))

(defn index
  [index mapping-type doc & options]
  (let [conn (esr/connect (:elastic-url env))]
    (apply esd/create conn index mapping-type doc options)))
