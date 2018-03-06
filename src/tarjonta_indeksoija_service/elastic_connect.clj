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

(defn create-index [index settings]            ;TODO mappings can be also created here
  (log/info (str "Creating new index ") index)
  (let [json {:settings settings}]
    (elastic-put (str (:elastic-url env) "/" index) json)))

(defn search [index mapping-type & query-params]
  (let [query-map (apply array-map query-params)]
    (elastic-post (elastic-url index mapping-type "_search") query-map)))

(defn get-document [index mapping-type id] ;TODO URL encoding
  (try
    (elastic-get (elastic-url index mapping-type id))
    (catch Exception e
      (if (= 404 ((ex-data e) :status)) {:found false} (throw e)))))

;MAX request payload size in AWS ElasticSearch
(defonce max-payload-size 10485760)

(defn bulk-partitions [data]
  (let [bulk-entries (map #(str (json/encode %) "\n") data)
        cur-bytes (atom 0)
        partitioner (fn [e]
                      (let [bytes (count (.getBytes e))]
                        (if (> max-payload-size (+ @cur-bytes bytes))
                          (do (reset! cur-bytes (+ @cur-bytes bytes)) true)
                          (do (reset! cur-bytes 0) false))))]
    (map #(clojure.string/join %) (partition-by partitioner bulk-entries))))

(defn bulk [index mapping-type data]
  (log/info "Executing bulk operation....")
  (if (not (empty? data))
    (let [partitions (bulk-partitions data)]
      (doall (map #(elastic-post (elastic-url index mapping-type "_bulk") %) partitions)))))

(comment defn bulk [index mapping-type data]
  (if (not (empty? data))
    (elastic-post (elastic-url index mapping-type "_bulk") data)))

(defn index-exists [index]
  (try
    (-> (http/head (str (:elastic-url env) "/" index))
        (:status)
        (= 200))
    (catch Exception e
      (if (= 404 ((ex-data e) :status)) false (throw e)))))