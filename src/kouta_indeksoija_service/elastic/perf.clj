(ns kouta-indeksoija-service.elastic.perf
  (:require [kouta-indeksoija-service.elastic.tools :as t]
            [clj-log.error-log :refer [with-error-logging with-error-logging-value]]
            [clj-elasticsearch.elastic-connect :as e]))

(def ^:const indexing-perf-index "indexing_perf")
(def ^:const query-perf-index "query_perf")

(defn insert-indexing-perf
  [indexed-amount duration started]
  (with-error-logging
   (e/create
    (t/index-name indexing-perf-index)
    (t/index-name indexing-perf-index)
    {:created              (System/currentTimeMillis)
     :started              started
     :duration_mills       duration
     :indexed_amount       indexed-amount
     :avg_mills_per_object (if (= 0 indexed-amount) 0 (/ duration indexed-amount))})))

(defn- get-perf
  [type since]
  (let [res (e/search (t/index-name type)
                      (t/index-name type)
                      :query {:range {:created {:gte since}}}
                      :sort [{:started "desc"} {:created "desc"}]
                      :aggs {:max_avg_mills_per_object {:max {:field "avg_mills_per_object"}}
                             :avg_mills_per_object {:avg {:field "avg_mills_per_object"}}
                             :max_time {:max {:field "duration_mills"}}
                             :avg_time {:avg {:field "duration_mills"}}}
                      :size 10000)]
    (merge (:aggregations res)
           {:results (map :_source (get-in res [:hits :hits]))})))

(defn get-elastic-performance-info
  [since]
  {:indexing_performance (get-perf indexing-perf-index since)
   :query_performance (get-perf query-perf-index since)})
