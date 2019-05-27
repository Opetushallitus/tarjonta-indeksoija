(ns kouta-indeksoija-service.indexer.search-data-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.indexer.docs.organisaatio :as org-appender]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.rest.koodisto :as koodisto-client]
            [kouta-indeksoija-service.test-tools :refer [reset-test-data]]
            [mocks.externals-mock :refer [with-externals-mock]]))

(use-fixtures :each (fn [test] (test) (reset-test-data false)))

(deftest organisaatio-search-data-test
  (testing "assoc correct search data for oppilaitos"
    (with-redefs [organisaatio-client/get-tyyppi-hierarkia (fn [x] { :organisaatiot [ { :oid "super-super-parent-oid"
                                                                                       :oppilaitostyyppi "oppilaitostyyppi_21#1"
                                                                                       :children [{ :oid "super-parent-oid"
                                                                                                   :children [{ :oid "parent-oid"
                                                                                                               :oppilaitostyyppi "oppilaitostyyppi_22#1"
                                                                                                               :children [{ :oid "oid"}]}]}]}]})
                  koodisto-client/get-koodi-with-cache (fn [x y] {:metadata [{:nimi "Koulu" :kieli "FI"}, {:nimi "School" :kieli "EN"}]})]
      (let [res (org-appender/append-search-data {:oid "oid"})]
        (is (= {:oid "oid" :searchData {:oppilaitostyyppi { :koodiUri "oppilaitostyyppi_22#1" :nimi { :fi "Koulu" :en "School"}} :tyyppi "amm" }} res))))))
