(ns kouta-indeksoija-service.indexer.kouta-koulutus-search-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.test-tools :refer [contains-same-elements-in-any-order?]]
            [kouta-indeksoija-service.indexer.tools.hakuaika :refer [->real-hakuajat]]
            [kouta-indeksoija-service.indexer.tools.search :refer [hakutapaKoodiUrit pohjakoulutusvaatimusKoodiUrit valintatapaKoodiUrit]]))

(defn- mock-koodisto-koulutustyyppi
  [koodi-uri alakoodi-uri]
  (vector
   { :koodiUri "koulutustyyppi_1" :nimi {:fi "joku nimi" :sv "joku nimi sv"}}
   { :koodiUri "koulutustyyppi_4" :nimi {:fi "joku nimi2" :sv "joku nimi sv2"}}))

(deftest filter-erityisopetus-koulutustyyppi
  (testing "If not ammatillinen perustutkinto erityisopetuksena, filter out erityisopetus koulutustyyppi from koodisto response"
    (with-redefs [kouta-indeksoija-service.rest.koodisto/list-alakoodi-nimet-with-cache mock-koodisto-koulutustyyppi]
      (let [koulutus {:koulutustyyppi "amm"}
            toteutus {:ammatillinenPerustutkintoErityisopetuksena false}
            result (kouta-indeksoija-service.indexer.tools.search/deduce-koulutustyypit koulutus toteutus)]
        (is (= ["koulutustyyppi_1" "amm"] result))))))

(deftest add-only-erityisopetus-koulutustyyppi-koodi
  (testing "If ammatillinen perustutkinto erityisopetuksena, add only erityisopetus koulutustyyppi koodi"
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

        expected1     {:alkaa "2031-04-02T12:00" :paattyy "2031-05-02T12:00"}
        expected2     {:alkaa "2032-04-02T12:00" :paattyy "2032-05-02T12:00"}
        expected3     {:alkaa "2033-04-02T12:00" :paattyy "2033-05-02T12:00"}
        expected4     {:alkaa "2034-04-02T12:00" :paattyy "2034-05-02T12:00"}

        haku1         {:hakuajat [hakuaika1, hakuaika2] :hakukohteet []}
        haku2         {:hakuajat [hakuaika3, hakuaika4] :hakukohteet []}
        haku          {:hakuajat []                     :hakukohteet []}
        hakukohde1    {:hakuajat [hakuaika1, hakuaika2] :kaytetaanHaunAikataulua false}
        hakukohde2    {:hakuajat [hakuaika3]            :kaytetaanHaunAikataulua false}
        hakukohde3    {:hakuajat [hakuaika4]            :kaytetaanHaunAikataulua false}]

    (testing "Hakuajat"
      (testing "should not contain anything if hakukohteet is empty"
        (is (empty? (->real-hakuajat {:haut [haku1, haku2]}))))

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
              (->real-hakuajat {:haut
                                [(merge haku1 {:hakukohteet [(merge hakukohde2 {:kaytetaanHaunAikataulua true})]})
                                 (merge haku  {:hakukohteet [hakukohde1] })
                                 haku1]})))))))

(deftest hakutieto-tools-test
  (let [hakutieto {:haut [{:hakutapaKoodiUri "hakutapa_03#1"
                           :hakukohteet [{:valintatapaKoodiUrit ["valintatapajono_av#1", "valintatapajono_tv#1"]
                                          :pohjakoulutusvaatimusKoodiUrit ["pohjakoulutusvaatimuskouta_104#1"]},
                                         {:valintatapaKoodiUrit ["valintatapajono_cv#1"]}]},
                          {:hakutapaKoodiUri "hakutapa_02#1"
                           :hakukohteet [{:valintatapaKoodiUrit []
                                          :pohjakoulutusvaatimusKoodiUrit ["pohjakoulutusvaatimuskouta_104#1", "pohjakoulutusvaatimuskouta_109#1"]},
                                         {:valintatapaKoodiUrit ["valintatapajono_cv#1", "valintatapajono_tv#1"]}]}
                          {:hakutapaKoodiUri "hakutapa_03#1"}
                          ]}]
    (testing "valintatapaKoodiUrit should parse properly"
      (is (contains-same-elements-in-any-order?
           ["valintatapajono_av#1", "valintatapajono_tv#1", "valintatapajono_cv#1"]
           (valintatapaKoodiUrit hakutieto))))

    (testing "hakutapaKoodiUrit should parse properly"
      (is (contains-same-elements-in-any-order?
           ["hakutapa_02#1", "hakutapa_03#1"]
           (hakutapaKoodiUrit hakutieto)
           )))

    (testing "pohjakoulutusvaatimusKoodiUrit should parse properly"
      (is (contains-same-elements-in-any-order?
           ["pohjakoulutusvaatimuskouta_104#1", "pohjakoulutusvaatimuskouta_109#1"]
           (pohjakoulutusvaatimusKoodiUrit hakutieto)
           )))))