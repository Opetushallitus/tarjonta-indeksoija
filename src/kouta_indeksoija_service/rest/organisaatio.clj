(ns kouta-indeksoija-service.rest.organisaatio
  (:require [kouta-indeksoija-service.util.conf :refer [env]]
            [clj-log.error-log :refer [with-error-logging]]
            [kouta-indeksoija-service.rest.util :as client]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [kouta-indeksoija-service.util.time :refer :all]
            [clojure.core.memoize :as memoize]))

(defn get-doc
  ([obj include-image]
   (with-error-logging
     (let [url (str (:organisaatio-service-url env) (:oid obj))
           params {:includeImage include-image}]
       (:body (client/get url {:query-params params :as :json})))))
  ([obj]
   (get-doc obj false)))

(defn- extract-docs [result]
  (->> result
       :body
       :organisaatiot
       (map #(conj (string/split (:parentOidPath %) #"/") (:oid %)))
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

(defn get-all-oids
  []
  (set (map :oid (find-docs nil))))

(defn get-tyyppi-hierarkia
  [oid]
  (with-error-logging
   (let [url (str (:organisaatio-service-url env) "v2/hierarkia/hae/tyyppi")
         params {:aktiiviset true :suunnitellut true :lakkautetut true :oid oid}]
     (:body (client/get url {:query-params params, :as :json})))))

(defn find-last-changes [last-modified]
  (with-error-logging
    (let [date-string (format-long last-modified)
          url (str (:organisaatio-service-url env) "v2/muutetut/oid")
          params {:lastModifiedSince date-string}]
      (let [res (->> (client/get url {:query-params params, :as :json})
                     (:body)
                     (:oids)
                     (map (fn [x] {:oid x :type "organisaatio"}))
                     (filter (fn [x] (not (clojure.string/blank? (:oid x))))))]
        (log/info "Found " (count res) " changes since " date-string " from organisaatiopalvelu")
        res))))

(defn find-by-oids
  [oids]
  (if (empty? oids) []
    (with-error-logging
     (log/debug (str "Calling organisaatio service find-by-oids") )
     (let [url (str (:organisaatio-service-url env) "v4/findbyoids")
           body (str "[\"" (string/join "\", \"" oids) "\"]")]
       (:body (client/post url {:body body :content-type :json :as :json}))))))

(defn get-by-oid
  [oid]
  (with-error-logging
   (log/debug (str "Calling organisaatio service get-by-oid " oid) )
   (let [url (str (:organisaatio-service-url env) "v4/" oid)]
     (:body (client/get url {:as :json})))))

(def get-by-oid-cached
  (memoize/ttl get-by-oid {} :ttl/threshold 86400000))
