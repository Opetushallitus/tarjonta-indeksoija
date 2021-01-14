(ns kouta-indeksoija-service.indexer.kouta-hakukohde-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer :all]))

(use-fixtures :each fixture/indices-fixture)
(use-fixtures :each common-indexer-fixture)

