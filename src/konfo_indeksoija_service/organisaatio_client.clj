(ns konfo-indeksoija-service.organisaatio-client
  (:require [konfo-indeksoija-service.conf :refer [env]]
            [konfo-indeksoija-service.util.tools :refer [with-error-logging]]
            [clj-http.client :as client]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

(defn get-doc
  ([obj include-image]
   (with-error-logging
     (let [url (str (:organisaatio-service-url env) (:oid obj) "?includeImage=" include-image)
           params {:includeImage false}]
       (:body (client/get url {:query-params params :as :json})))))
  ([obj]
   (get-doc obj false)))

(defn- extract-docs [result]
  (->> result
       :body
       :organisaatiot
       (map #(conj (str/split (:parentOidPath %) #"/") (:oid %)))
       flatten
       distinct
       (map #(assoc {} :type "organisaatio" :oid %))))

(defn find-docs
  [oid]
  (with-error-logging
    (if (nil? oid)
      (let [res (client/get (str (:organisaatio-service-url env)) {:as :json})]
        (->> res
             :body
             (map #(assoc {} :type "organisaatio" :oid %))))
      (let [params {:aktiiviset true :suunnitellut true :lakkautetut true :oid oid}
            url (str (:organisaatio-service-url env) "v2/hae")]
        (extract-docs (client/get url {:query-params params, :as :json}))))))
