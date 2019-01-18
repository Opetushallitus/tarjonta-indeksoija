(ns konfo-indeksoija-service.util.seq)

(defn collect-first
  ([f check? seq]
   (loop [values seq]
     (when (not (empty? values))
       (let [current (first values)
             mapped (f current)]
         (if (check? mapped)
           mapped
           (recur (rest values))))))))


