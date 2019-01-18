(ns konfo-indeksoija-service.util.seq-test
  (:require [midje.sweet :refer :all]
            [konfo-indeksoija-service.util.seq :refer :all]))

(fact "collect-first should return first matching value of mapping"
      (let [seq [1 2 3 4 5 6]]
        (collect-first #(* 3 %) #(= 9 %) seq) => 9
        (collect-first #(+ 2 %) #(= 8 %) seq) => 8))

(fact "collect-first should execute 'f' only until it finds first match"
      (let [seq [1 2 3 4 5 6]
            f (fn [a]
                (if (> a 3)
                  (throw (Exception. "Execution went too far"))
                  (* 3 a)))
            check? #(= 9 %)]
        (collect-first f check? seq) => 9))

(fact "collect-first should return 'nil' if no match was found"
      (let [seq [1 2 3 4 5 6]]
        (collect-first nil? (fn [a] false) seq) => nil))
