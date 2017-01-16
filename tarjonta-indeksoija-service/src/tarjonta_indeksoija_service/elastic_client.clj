(ns tarjonta-indeksoija-service.elastic-client
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest.response :as esrsp]
            [clojure.pprint :as pp]
            [clojurewerkz.elastisch.rest.index :as esi]))

(defn query
  [oid]
  (let [conn (esr/connect "http://127.0.0.1:9200")
        res (esd/search conn "koulutus" "koulutus" :query (q/term :oid oid))]
    (esrsp/hits-from res)))

(defn index
  [& args]
  (let [conn (esr/connect "http://127.0.0.1:9200")
        doc {:oid "1.2.3.4"}]
    (println (esd/create conn "koulutus" "koulutus" doc))))

