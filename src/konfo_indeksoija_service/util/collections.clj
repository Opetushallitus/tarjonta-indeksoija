(ns konfo-indeksoija-service.util.collections)

(defn collect-first
  "get first mapped value that matches 'check?' or nil"
  ([f check? seq]
   (loop [values seq]
     (when (not (empty? values))
       (let [current (first values)
             mapped (f current)]
         (if (check? mapped)
           mapped
           (recur (rest values))))))))


(defn in?
  "true if coll contains element"
  [coll element]
  (true? (some #(= element %) coll)))