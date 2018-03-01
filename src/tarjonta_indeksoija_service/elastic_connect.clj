(ns tarjonta-indeksoija-service.elastic-connect
  (:require [tarjonta-indeksoija-service.conf :as conf :refer [env boost-values]]
            [tarjonta-indeksoija-service.util.tools :refer [with-error-logging with-error-logging-value]]
            [environ.core]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [taoensso.timbre :as log]
            [cheshire.core :refer [generate-string]]))

(defn index-name
  [name]
  (str name (when (Boolean/valueOf (:test environ.core/env)) "_test")))

(defn join-names
  [name-or-names]
  (clojure.string/join "," (flatten [name-or-names])))

(defn elastic-url
  ([index mapping-type]
   (str (:elastic-url env) "/" index "/" mapping-type))
  ([index mapping-type operation]
   (str (elastic-url index mapping-type) "/" operation )))

(defn elastic-post
  [url body]
  (-> (http/post url {:body (if (instance? String body) body (json/encode body)) :content-type :json})
      (:body)
      (json/decode true)))

(defn elastic-put
  [url body]
  (-> (http/put url {:body (if (instance? String body) body (json/encode body)) :content-type :json})
  (:body)
  (json/decode true)))

(defn elastic-get
  [url]
  (-> (http/get url)
  (:body)
  (json/decode true)))

(defn create [index mapping-type document]
  (elastic-post (elastic-url index mapping-type) document))

(defn search [index mapping-type & query-params]
  (let [query-map (apply array-map query-params)]
    (elastic-post (elastic-url index mapping-type "_search") query-map)))

(defn get-document [index mapping-type id] ;TODO URL encoding
  (elastic-get (elastic-url index mapping-type id)))

(defn bulk [index mapping-type data]
  (if (not (empty? data))
    (let [bulk-json (map json/encode data)
          bulk-json (-> bulk-json
                        (interleave (repeat "\n"))
                        (clojure.string/join))]
      (elastic-post (elastic-url index mapping-type "_bulk") bulk-json))))