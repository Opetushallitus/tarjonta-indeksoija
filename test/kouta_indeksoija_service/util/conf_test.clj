(ns kouta-indeksoija-service.util.conf-test
  (:require [clojure.test :refer :all]))

(deftest conf-test
  (let [template-conf (read-string (slurp "./oph-configuration/config.edn.template"))]

    (testing "Test that ci configuration is correct"
          (let [ci-conf-template  (read-string (slurp "./ci_resources/config.edn.template"))
                ci-conf           (read-string (slurp "./ci_resources/config.edn"))]
            (is (= (keys template-conf)    (keys ci-conf-template)))
            (is (= (keys ci-conf-template) (keys ci-conf)))))

    (testing "Test that dev configuration is correct"
          (let [test-conf-template  (read-string (slurp "./dev_resources/config.edn.template"))]
            (is (= (keys template-conf) (keys test-conf-template)))
            (when-let [test-conf (try (read-string (slurp "./dev_resources/config.edn")) (catch Exception e nil))]
              (is (= (keys test-conf-template) (keys test-conf))))))

    (testing "Test that test configuration is correct"
      (let [test-conf-template  (read-string (slurp "./test_resources/config.edn.template"))]
        (is (= (keys template-conf) (keys test-conf-template)))
        (when-let [test-conf (try (read-string (slurp "./test_resources/config.edn")) (catch Exception e nil))]
          (is (= (keys test-conf-template) (keys test-conf))))))))