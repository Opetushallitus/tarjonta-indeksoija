(ns konfo-indeksoija-service.search-data-test
  (:require [konfo-indeksoija-service.converter.koulutus-search-data-appender :as appender]
            [konfo-indeksoija-service.converter.oppilaitos-search-data-appender :as org-appender]
            [konfo-indeksoija-service.rest.tarjonta :as tarjonta-client]
            [konfo-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [konfo-indeksoija-service.rest.koodisto :as koodisto-client]
            [clj-test-utils.elasticsearch-mock-utils :refer [init-elastic-test stop-elastic-test]]
            [konfo-indeksoija-service.test-tools :refer [reset-test-data]]
            [mocks.externals-mock :refer [with-externals-mock]]
            [midje.sweet :refer :all]))

(against-background
  [(before :contents (init-elastic-test))
   (after :facts (reset-test-data))
   (after :contents (stop-elastic-test))]

  (fact "should calculate opintopolun näyttäminen loppuu correctly"
    (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" :opintopolunNayttaminenLoppuu "2018-02-03" :hakuaikas [{:loppuPvm 1522529940000}]},
                                                    {:oid "1.2.4" :opintopolunNayttaminenLoppuu "2018-02-02"}] []) => "2018-02-03"
    (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" :hakuaikas [{:loppuPvm 1522529940000}, {:loppuPvm 1511938800000}]},
                                                    {:oid "1.2.4" :opintopolunNayttaminenLoppuu "2018-02-02"}] []) => "2018-02-02"
    (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" :hakuaikas [{:loppuPvm 1522529940000}, {:loppuPvm 1511938800000}]},
                                                    {:oid "1.2.4" :hakuaikas [{:loppuPvm 1512220000000}]}] []) => "2018-09-30"
    (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" :hakuaikas [{:loppuPvm 1522529940000}, {:loppuPvm 1511938800000}]},
                                                    {:oid "1.2.4" :hakuaikas [{:loppuPvm 1512220000000}]}]
                                                   [{:oid "1.1.1" :hakuaikaRyhma "Syksy 2016 (2016-08-23 12:00 - 2016-12-22 12:00)"},
                                                    {:oid "1.1.1" :hakuaikaRyhma "Syksy 2015 (2015-08-23 12:00 - 2015-12-22 12:00)"}]) => "2017-06-22"
    (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" :hakuaikas [{:loppuPvm 1522529940000}, {:loppuPvm 1511938800000}]},
                                                    {:oid "1.2.4" :opintopolunNayttaminenLoppuu "2018-02-02"}]
                                                   [{:oid "1.1.1" :hakuaikaRyhma "Syksy 2016 (2016-08-23 12:00 - 2016-12-22 12:00)"}]) => "2018-02-02"
    (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" :hakuaikas [{:loppuPvm 1522529940000}, {:loppuPvm 1511938800000}]},
                                                    {:oid "1.2.4" :opintopolunNayttaminenLoppuu "2018-02-02"}] []) => "2018-02-02"
    (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" :hakuaikas [{:alkuPvm 1522529940000}]},
                                                    {:oid "1.2.4" }] []) => nil
    (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" :hakuaikas [{:alkuPvm 1522529940000}, {:loppuPvm nil}]},
                                                    {:oid "1.2.4" }] []) => nil
    (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" },
                                                    {:oid "1.2.4" }] []) => nil)

  (fact "assoc correct search data"
    (with-redefs [tarjonta-client/get-hakukohteet-for-koulutus (fn [x] [{:oid "hakukohdeOid" :nimi {:fi "Hakukohteen nimi"} :hakuOid "hakuOid"}])
                  tarjonta-client/get-haut-by-oids (fn [x] [{:oid "hakuOid" :opintopolunNayttaminenLoppuu "2019-07-07"}])
                  organisaatio-client/get-doc (fn [x f] {:oid "organisaatioOid" :nimi {:fi "Organisaation nimi"}})]
      (let [res (appender/append-search-data {:oid "oid" :koulutuskoodi { :nimi { :kieli_fi "Kiva koulutus"}} :organisaatio {:oid "organisaatioOid"}})]
        res => {:oid "oid"
                :koulutuskoodi { :nimi { :kieli_fi "Kiva koulutus"}}
                :organisaatio {:oid "organisaatioOid"}
                :searchData { :haut [{:oid "hakuOid" :opintopolunNayttaminenLoppuu "2019-07-07"}]
                             :hakukohteet [{:oid "hakukohdeOid" :nimi {:kieli_fi "Hakukohteen nimi"} :hakuOid "hakuOid"}]
                             :organisaatio {:oid "organisaatioOid" :nimi {:kieli_fi "Organisaation nimi"}}
                             :nimi { :kieli_fi "Kiva koulutus"}
                             :tyyppi "muu"
                             :opintopolunNayttaminenLoppuu "2019-07-07"}})))

  (fact "assoc correct search data 2"
    (with-redefs [tarjonta-client/get-hakukohteet-for-koulutus (fn [x] [{:oid "hakukohdeOid" :nimi {:fi "Hakukohteen nimi"} :hakuOid "hakuOid" :hakuaikaRyhma "Syksy 2015 (2015-08-23 12:00 - 2015-12-22 12:00)"}])
                  tarjonta-client/get-haut-by-oids (fn [x] [{:oid "hakuOid"}])
                  organisaatio-client/get-doc (fn [x f] {:oid "organisaatioOid" :nimi {:fi "Organisaation nimi"}})]
      (let [oppiaineet [ { :oppiaine "kemia" :kieliKoodi "kieli_fi" }, { :oppiaine "chemistry" :kieliKoodi "kieli_en" }, { :oppiaine "physics" :kieliKoodi "kieli_en" }]
            res (appender/append-search-data {:oid "oid" :organisaatio {:oid "organisaatioOid"}
                                              :oppiaineet oppiaineet})]
        res => {:oid "oid"
                :organisaatio {:oid "organisaatioOid"}
                :oppiaineet oppiaineet
                :searchData {:haut [{:oid "hakuOid"}]
                             :hakukohteet [{:oid "hakukohdeOid" :nimi {:kieli_fi "Hakukohteen nimi"} :hakuOid "hakuOid" :hakuaika {:alkuPvm 1440320400000, :loppuPvm 1450778400000}}]
                             :organisaatio {:oid "organisaatioOid" :nimi {:kieli_fi "Organisaation nimi"}}
                             :nimi { :kieli_fi "Hakukohteen nimi"}
                             :tyyppi "muu"
                             :oppiaineet [{:kieli_fi "kemia"}, {:kieli_en "chemistry"}, {:kieli_en "physics"}]
                             :opintopolunNayttaminenLoppuu "2016-06-22"
                             }})))

  (fact "assoc correct lukio nimi"
    (with-redefs [tarjonta-client/get-hakukohteet-for-koulutus (fn [x] [{:oid "hakukohdeOid" :nimi {:fi "Hakukohteen nimi"} :hakuOid "hakuOid"}])
                  tarjonta-client/get-haut-by-oids (fn [x] [{:oid "hakuOid"}])
                  organisaatio-client/get-doc (fn [x f] {:oid "organisaatioOid" :nimi {:fi "Kiva lukio" :sv "Jättekiva lukio"}})]
      (let [res (appender/append-search-data {:oid "oid"
                                              :organisaatio {:oid "organisaatioOid"}
                                              :koulutustyyppi {:uri "koulutustyyppi_2"}
                                              :koulutusohjelma {:nimi {:kieli_fi "Lukio" :kieli_sv "Gymnasium"}}})]
        res => {:oid "oid"
                :organisaatio {:oid "organisaatioOid"}
                :koulutustyyppi {:uri "koulutustyyppi_2"}
                :koulutusohjelma {:nimi {:kieli_fi "Lukio" :kieli_sv "Gymnasium"}}
                :searchData {:haut [{:oid "hakuOid"}]
                             :hakukohteet [{:oid "hakukohdeOid" :nimi {:kieli_fi "Hakukohteen nimi"} :hakuOid "hakuOid"}]
                             :organisaatio {:oid "organisaatioOid" :nimi {:kieli_fi "Kiva lukio" :kieli_sv "Jättekiva lukio"}}
                             :tyyppi "lk"
                             :nimi { :kieli_fi "Lukio" :kieli_sv "Gymnasium"}}})))

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
                :searchData {:oppilaitostyyppi { :koodiUri "oppilaitostyyppi_22#1" :nimi { :fi "Koulu" :en "School"}} :tyyppi "amm" }})))

  (fact "parse hakuaikaRyhmä"
    (let [ryhma1 "(2017-08-01 08:00 - 2017-08-15 23:59)"
          ryhma2 "Syksy 2016 (2016-08-23 12:00 - 2016-12-22 12:00)"]
      (appender/parse-hakuaika-ryhma ryhma1) => {:alkuPvm 1501563600000 :loppuPvm 1502830740000}
      (appender/parse-hakuaika-ryhma ryhma2) => {:alkuPvm 1471942800000 :loppuPvm 1482400800000})))
