(ns tarjonta-indeksoija-service.elastic-client
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest.response :as esrsp]))

(defn delete-index
  [index-name]
  (let [conn (esr/connect (:elastic-url env))]
    (esi/delete conn index-name)))

(defn get-by-id
  [index type id]
  (let [conn (esr/connect (:elastic-url env))
        res (esd/get conn index type id)]
    (:_source res)))

(defn upsert
  [index type id doc]
  (let [conn (esr/connect (:elastic-url env))]
    (esd/upsert conn index type id (assoc doc :timestamp (System/currentTimeMillis)))))

(defn delete-by-id
  [index type id]
  (let [conn (esr/connect (:elastic-url env))]
    (esd/delete conn index type id)))

(defn get-first-from-queue
  [& {:keys [index type]
      :or {index "indexdata"
           type "indexdata"}}]
  (let [conn (esr/connect (:elastic-url env))]
    (try
      (-> (esd/search conn index type :query (q/match-all) :sort {:timestamp "asc"} :size 1)
          :hits
          :hits
          first
          :_source)
      (catch Exception e nil)))) ;; TODO: fixme

(defn push-to-indexing-queue
  [oid object-type & {:keys [index type]
                      :or {index "indexdata"
                           type "indexdata"}}]
  (upsert index type oid {:oid oid :type object-type}))
