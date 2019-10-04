(ns mocks.notifier-target-mock)

(def received (atom {}))

(defn clear [] (reset! received {}))

(defn add
  [body]
  (let [id (or (:oid body) (:id body))]
    (swap! received assoc id body)))

(defn notifier-mock-fixture [tests] (tests) (clear))
