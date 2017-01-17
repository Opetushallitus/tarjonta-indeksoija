(ns tarjonta-indeksoija-service.elastic-client
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest.response :as esrsp]
            [clojure.pprint :as pp]
            [clojurewerkz.elastisch.rest.index :as esi]))

(defn query
  [index mapping-type & params]
  (let [conn (esr/connect "http://127.0.0.1:9200")
        res (esd/search conn index mapping-type :query (apply q/terms params))]
    (esrsp/hits-from res)))

(defn index
  [index mapping-type doc & options]
  (let [conn (esr/connect "http://127.0.0.1:9200")]
    (apply esd/create conn index mapping-type doc options)))

