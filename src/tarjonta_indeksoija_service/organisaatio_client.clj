(ns tarjonta-indeksoija-service.organisaatio-client
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [clj-http.client :as client]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

(defn get-doc
  [obj]
  (try
    (let [url (str (:organisaatio-service-url env) (:oid obj))
          params {:includeImage false}]
      (-> (client/get url {:query-params params :as :json})
          :body))
    (catch Exception e (log/error e))))

(defn- extract-docs [result]
  (->> result
       :body
       :organisaatiot
       (map #(conj (str/split (:parentOidPath %) #"/") (:oid %)))
       flatten
       distinct
       (map #(assoc {} :type "organisaatio" :oid %))))

(defn find-docs
  [params]
  (try
    (let [params-with-defaults (merge {:aktiiviset true :suunnitellut true :lakkautetut true} params)
          url (str (:organisaatio-service-url env) "v2/hae")]
      (extract-docs (client/get url {:query-params params-with-defaults, :as :json})))
    (catch Exception e (log/error e))))