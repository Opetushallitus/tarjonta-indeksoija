(ns kouta-indeksoija-service.indexer.kouta-koulutus-search-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.test-tools :refer [contains-same-elements-in-any-order?]]
            [kouta-indeksoija-service.indexer.tools.hakuaika :refer [->real-hakuajat]]))

(defn- mock-koodisto-koulutustyyppi
  [koodi-uri alakoodi-uri]
  (vector
   { :koodiUri "koulutustyyppi_1" :nimi {:fi "joku nimi" :sv "joku nimi sv"}}
   { :koodiUri "koulutustyyppi_4" :nimi {:fi "joku nimi2" :sv "joku nimi sv2"}}))

(deftest testia
  (testing "amm perustutkinto (TODO)"
    (with-redefs [kouta-indeksoija-service.rest.koodisto/list-alakoodi-nimet-with-cache mock-koodisto-koulutustyyppi]
      (let [koulutus {:koulutustyyppi "amm"}
            toteutus {:ammatillinenPerustutkintoErityisopetuksena false}
            result (kouta-indeksoija-service.indexer.tools.search/deduce-koulutustyypit koulutus toteutus)]
        (is (= ["koulutustyyppi_1" "amm"] result))))))

(deftest testia2
  (testing "amm perustutkinto (TODOOOOOOOO)"
    (with-redefs [kouta-indeksoija-service.rest.koodisto/list-alakoodi-nimet-with-cache mock-koodisto-koulutustyyppi]
      (let [koulutus {:koulutustyyppi "amm"}
            toteutus {:ammatillinenPerustutkintoErityisopetuksena true}
            result (kouta-indeksoija-service.indexer.tools.search/deduce-koulutustyypit koulutus toteutus)]
        (is (= ["koulutustyyppi_4" "amm"] result))))))

(deftest hakuajat-test

  (let [hakuaika1     {:alkaa "2031-04-02T12:00" :paattyy "2031-05-02T12:00"}
        hakuaika2     {:alkaa "2032-04-02T12:00" :paattyy "2032-05-02T12:00"}
        hakuaika3     {:alkaa "2033-04-02T12:00" :paattyy "2033-05-02T12:00"}
        hakuaika4     {:alkaa "2034-04-02T12:00" :paattyy "2034-05-02T12:00"}

        expected1     {:gte "2031-04-02T12:00" :lt "2031-05-02T12:00"}
        expected2     {:gte "2032-04-02T12:00" :lt "2032-05-02T12:00"}
        expected3     {:gte "2033-04-02T12:00" :lt "2033-05-02T12:00"}
        expected4     {:gte "2034-04-02T12:00" :lt "2034-05-02T12:00"}

        haku1         {:hakuajat [hakuaika1, hakuaika2] :hakukohteet []}
        haku2         {:hakuajat [hakuaika3, hakuaika4] :hakukohteet []}
        haku          {:hakuajat []                     :hakukohteet []}
        hakukohde1    {:hakuajat [hakuaika1, hakuaika2] :kaytetaanHaunAikataulua false}
        hakukohde2    {:hakuajat [hakuaika3]            :kaytetaanHaunAikataulua false}
        hakukohde3    {:hakuajat [hakuaika4]            :kaytetaanHaunAikataulua false}]

    (testing "Hakuajat"
      (testing "should contain hakujen hakuajat"
        (is (contains-same-elements-in-any-order?
              [expected1, expected2, expected3, expected4]
              (->real-hakuajat {:haut [haku1, haku2]}))))

      (testing "should contain hakukohteiden hakuajat"
        (is (contains-same-elements-in-any-order?
              [expected1, expected2, expected3, expected4]
              (->real-hakuajat {:haut [(merge haku {:hakukohteet [hakukohde1, hakukohde2]}),
                                       (merge haku {:hakukohteet [hakukohde3]})]}))))

      (testing "should ignore haun hakuajat if not used"
        (is (contains-same-elements-in-any-order?
              [expected3]
              (->real-hakuajat {:haut [(merge haku1 {:hakukohteet [hakukohde2]})]}))))

      (testing "should ignore hakukohteen hakuajat if not used"
        (is (contains-same-elements-in-any-order?
              [expected1, expected2]
              (->real-hakuajat {:haut [(merge haku1 {:hakukohteet [(merge hakukohde2 {:kaytetaanHaunAikataulua true})]})]}))))

      (testing "should remove duplicates"
        (is (contains-same-elements-in-any-order?
              [expected1, expected2]
              (->real-hakuajat {:haut [haku1 (merge haku (:hakukohteet hakukohde1)) haku1]})))))))