(ns kouta-indeksoija-service.util.cache
    (:require [clojure.core.cache :as cache]
              [clojure.core.memoize :as memo]))

;yhdistelmä FIFO & TTL cachesta
;https://github.com/clojure/core.cache/wiki/Composing
(defn with-fifo-ttl-cache
  ([f ttl-millis fifo-threshold seed]
    (let [cache (-> {}
                    (cache/fifo-cache-factory :threshold fifo-threshold)
                    (cache/ttl-cache-factory :ttl ttl-millis))]
      (memo/memoizer f cache seed)))
  ([f ttl-millis fifo-threshold]
    (with-fifo-ttl-cache f ttl-millis fifo-threshold {})))
