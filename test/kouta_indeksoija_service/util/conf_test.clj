(ns kouta-indeksoija-service.util.conf-test
  (:require [midje.sweet :refer :all]))

(let [template-conf (read-string (slurp "./oph-configuration/config.edn.template"))]

  (fact "Test that ci configuration is correct"
     (let [ci-conf-template  (read-string (slurp "./ci_resources/config.edn.template"))
           ci-conf           (read-string (slurp "./ci_resources/config.edn"))]
       (keys ci-conf-template) => (keys template-conf)
       (keys ci-conf)          => (keys ci-conf-template)))

  (fact "Test that dev configuration is correct"
    (let [test-conf-template  (read-string (slurp "./dev_resources/config.edn.template"))]
      (keys test-conf-template) => (keys template-conf)
      (when-let [test-conf (try (read-string (slurp "./dev_resources/config.edn")) (catch Exception e nil))]
        (keys test-conf)          => (keys test-conf-template)))))