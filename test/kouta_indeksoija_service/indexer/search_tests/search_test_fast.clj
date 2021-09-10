(ns kouta-indeksoija-service.indexer.search-tests.search-test-fast
  (:require [clojure.test :refer [deftest testing is]]
            [kouta-indeksoija-service.indexer.tools.search :as search]))

(deftest number-or-nil
  (testing "leaves opintojenlaajuus koodiArvo as it is because it is a number"
    (is (= "60"
           (search/number-or-nil "60"))))

  (testing "sets opintojenlaajuus koodiarvo as nil when it has a letter in it"
    (is (= nil
           (search/number-or-nil "v53")))))
