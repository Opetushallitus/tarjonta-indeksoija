(ns konfo-indeksoija-service.conf-test
  (:require [midje.sweet :refer :all]))

(fact "Test that oph-configuration/config.edn.template and dev-resources/config.edn contain the same keys"
  (let [template-conf (read-string (slurp "./oph-configuration/config.edn.template"))
        dev-conf (read-string (slurp "./dev_resources/config.edn.template"))]
    (keys dev-conf) => (keys template-conf)))