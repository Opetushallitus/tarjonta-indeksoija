(ns kouta-indeksoija-service.indexer.common-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.indexer.kouta.common :refer [koodi-uri?]]))

(deftest is-koodi-uri-test
  (testing "Check what is interpreted as koodiUri and what is not"
    (is (some? (koodi-uri? "tutkintonimikekk_411#2")))
    (is (some? (koodi-uri? "tutkintonimikekk_221-1#233")))
    (is (some? (koodi-uri? "koulutus_371101#1")))
    (is (some? (koodi-uri? "koulutus_371101")))
    (is (empty? (koodi-uri? "tutkintonim?ikekk_221-1#234")))))