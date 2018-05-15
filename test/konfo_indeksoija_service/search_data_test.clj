(ns konfo-indeksoija-service.search-data-test
  (:require [konfo-indeksoija-service.converter.koulutus-search-data-appender :as appender]
            [konfo-indeksoija-service.tarjonta-client :as tarjonta-client]
            [konfo-indeksoija-service.organisaatio-client :as organisaatio-client]
            [konfo-indeksoija-service.test-tools :as tools :refer [reset-test-data init-elastic-test]]
            [mocks.externals-mock :refer [with-externals-mock]]
            [midje.sweet :refer :all]))

(init-elastic-test)

(fact "should calculate opintopolun näyttäminen loppuu correctly"
  (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" :opintopolunNayttaminenLoppuu "2018-02-03" :hakuaikas [{:loppuPvm 1522529940000}]},
                                                 {:oid "1.2.4" :opintopolunNayttaminenLoppuu "2018-02-02"}]) => "2018-02-03"
  (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" :hakuaikas [{:loppuPvm 1522529940000}, {:loppuPvm 1511938800000}]},
                                                 {:oid "1.2.4" :opintopolunNayttaminenLoppuu "2018-02-02"}]) => "2018-02-02"
  (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" :hakuaikas [{:loppuPvm 1522529940000}, {:loppuPvm 1511938800000}]},
                                                 {:oid "1.2.4" :hakuaikas [{:loppuPvm 1512220000000}]}]) => "2019-01-31"
  (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" :hakuaikas [{:loppuPvm 1522529940000}, {:loppuPvm 1511938800000}]},
                                                 {:oid "1.2.4" :opintopolunNayttaminenLoppuu "2018-02-02"}]) => "2018-02-02"
  (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" :hakuaikas [{:alkuPvm 1522529940000}]},
                                                 {:oid "1.2.4" }]) => nil
  (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" :hakuaikas [{:alkuPvm 1522529940000}, {:loppuPvm nil}]},
                                                  {:oid "1.2.4" }]) => nil
  (appender/count-opintopolun-nayttaminen-loppuu [{:oid "1.2.3" },
                                                 {:oid "1.2.4" }]) => nil)

(fact "assoc correct search data"
  (with-redefs [tarjonta-client/get-hakukohteet-for-koulutus (fn [x] [{:oid "hakukohdeOid" :nimi {:fi "Hakukohteen nimi"} :relatedOid "hakuOid"}])
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
                           :opintopolunNayttaminenLoppuu "2019-07-07"}})))

(fact "assoc correct search data 2"
  (with-redefs [tarjonta-client/get-hakukohteet-for-koulutus (fn [x] [{:oid "hakukohdeOid" :nimi {:fi "Hakukohteen nimi"} :relatedOid "hakuOid"}])
                tarjonta-client/get-haut-by-oids (fn [x] [{:oid "hakuOid"}])
                organisaatio-client/get-doc (fn [x f] {:oid "organisaatioOid" :nimi {:fi "Organisaation nimi"}})]
    (let [res (appender/append-search-data {:oid "oid" :organisaatio {:oid "organisaatioOid"}})]
      res => {:oid "oid"
              :organisaatio {:oid "organisaatioOid"}
              :searchData {:haut [{:oid "hakuOid"}]
                           :hakukohteet [{:oid "hakukohdeOid" :nimi {:kieli_fi "Hakukohteen nimi"} :hakuOid "hakuOid"}]
                           :organisaatio {:oid "organisaatioOid" :nimi {:kieli_fi "Organisaation nimi"}}
                           :nimi { :kieli_fi "Hakukohteen nimi"}}})))