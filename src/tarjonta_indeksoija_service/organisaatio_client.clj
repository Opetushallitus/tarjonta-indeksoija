(ns tarjonta-indeksoija-service.organisaatio-client
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [clj-http.client :as client]
            [clojure.string :as str]))

(defn get-doc
  [obj]
  (let [url (str (:organisaatio-service-url env) (:oid obj))
        params {:includeImage false}]
    (-> (client/get url {:query-params params :as :json})
        :body)))

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
  (let [params-with-defaults (merge {:aktiiviset true :suunnitellut true :lakkautetut true} params)
        url (str (:organisaatio-service-url env) "v2/hae")]
    (println url)
    (extract-docs (client/get url {:query-params params-with-defaults, :as :json}))))