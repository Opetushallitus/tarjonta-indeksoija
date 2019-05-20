(ns kouta-indeksoija-service.search-data-test
  (:require [kouta-indeksoija-service.search-data.oppilaitos :as org-appender]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.rest.koodisto :as koodisto-client]
            [clj-test-utils.elasticsearch-mock-utils :refer [init-elastic-test stop-elastic-test]]
            [kouta-indeksoija-service.test-tools :refer [reset-test-data]]
            [kouta-indeksoija-service.util.time :refer [convert-to-long]]
            [mocks.externals-mock :refer [with-externals-mock]]
            [midje.sweet :refer :all]))

(against-background
  [(before :contents (init-elastic-test))
   (after :facts (reset-test-data))
   (after :contents (stop-elastic-test))]

  (fact "assoc correct search data for oppilaitos"
    (with-redefs [organisaatio-client/get-tyyppi-hierarkia (fn [x] { :organisaatiot [ { :oid "super-super-parent-oid"
                                                                                       :oppilaitostyyppi "oppilaitostyyppi_21#1"
                                                                                       :children [{ :oid "super-parent-oid"
                                                                                                   :children [{ :oid "parent-oid"
                                                                                                               :oppilaitostyyppi "oppilaitostyyppi_22#1"
                                                                                                               :children [{ :oid "oid"}]}]}]}]})
                  koodisto-client/get-koodi-with-cache (fn [x y] {:metadata [{:nimi "Koulu" :kieli "FI"}, {:nimi "School" :kieli "EN"}]})]
      (let [res (org-appender/append-search-data {:oid "oid"})]
        res => {:oid "oid"
                :searchData {:oppilaitostyyppi { :koodiUri "oppilaitostyyppi_22#1" :nimi { :fi "Koulu" :en "School"}} :tyyppi "amm" }}))))
