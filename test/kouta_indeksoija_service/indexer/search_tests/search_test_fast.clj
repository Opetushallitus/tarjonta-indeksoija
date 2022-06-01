(ns kouta-indeksoija-service.indexer.search-tests.search-test-fast
  (:require [clojure.test :refer [deftest testing is]]
            [kouta-indeksoija-service.indexer.tools.search :as search]))

(deftest number-or-nil
  (testing "leaves opintojenlaajuus koodiArvo as it is because it is a number"
    (is (= "60"
           (search/number-or-nil "60"))))

  (testing "sets opintojenlaajuus koodiarvo as nil when it has a letter in it"
    (is (= nil
           (search/number-or-nil "v53")))))

(deftest jarjestaako-tarjoaja-urheilijan-amm-koulutusta
  (testing "returns true for one tarjoaja when one hakukohde in one haku that has corresponding jarjestyspaikkaOid has true for jarjestaaUrheilijanAmmKoulutusta"
    (is (= true
           (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
             ["1.2.246.562.10.96162204109"]
             [{:tila "julkaistu"
               :hakukohteet [{:tila "julkaistu"
                              :jarjestyspaikkaOid "1.2.246.562.10.96162204109"
                              :toteutusOid "1.2.246.562.17.00000000000000006388"
                              :nimi {:fi "Isännöinnin ammattitutkinto"}
                              :hakuOid "1.2.246.562.29.00000000000000000045"
                              :hakukohdeOid "1.2.246.562.20.00000000000000010664"
                              :jarjestaaUrheilijanAmmKoulutusta true
                              :organisaatioOid "1.2.246.562.10.96162204109"}]
               :nimi {:fi "Jatkuva haku Joku Oppilaitos"}
               :organisaatioOid "1.2.246.562.10.96162204109"}]))))

    (testing "returns false for one tarjoaja when hakukohde with corresponding jarjestyspaikkaOid has false for jarjestaaUrheilijanAmmKoulutusta"
      (is (= false
             (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
               ["1.2.246.562.10.44802853312"]
               [{:tila "julkaistu"
                 :hakukohteet [{:tila "julkaistu"
                                :jarjestyspaikkaOid "1.2.246.562.10.4444444444444"
                                :toteutusOid "1.2.246.562.17.00000000000000006388"
                                :nimi {:fi "Isännöinnin ammattitutkinto"}
                                :hakuOid "1.2.246.562.29.00000000000000000045"
                                :hakukohdeOid "1.2.246.562.20.00000000000000010664"
                                :jarjestaaUrheilijanAmmKoulutusta true
                                :organisaatioOid "1.2.246.562.10.96162204109"}
                               {:tila "julkaistu"
                                :jarjestyspaikkaOid "1.2.246.562.10.44802853312"
                                :toteutusOid "1.2.246.562.17.00000000000000006388"
                                :nimi {:fi "Isännöinnin ammattitutkinto 2"}
                                :hakuOid "1.2.246.562.29.00000000000000000045"
                                :hakukohdeOid "1.2.246.562.20.00000000000000010665"
                                :jarjestaaUrheilijanAmmKoulutusta false
                                :organisaatioOid "1.2.246.562.10.44802853312"}]
                 :nimi {:fi "Jatkuva haku Joku Oppilaitos"}
                 :organisaatioOid "1.2.246.562.10.48442622063"}]
               ))))

  (testing "returns true if the only hakukohde in the only haku has true for jarjestaaUrheilijanAmmKoulutusta"
    (is (= true
           (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
             ["1.2.246.562.10.96162204109"]
               [{:tila "julkaistu"
                 :hakukohteet [{:tila "julkaistu"
                                :jarjestyspaikkaOid "1.2.246.562.10.96162204109"
                                :toteutusOid "1.2.246.562.17.00000000000000006388"
                                :nimi {:fi "Isännöinnin ammattitutkinto"}
                                :hakuOid "1.2.246.562.29.00000000000000000045"
                                :hakukohdeOid "1.2.246.562.20.00000000000000010664"
                                :jarjestaaUrheilijanAmmKoulutusta true
                                :organisaatioOid "1.2.246.562.10.96162204109"}]
                 :nimi {:fi "Jatkuva haku Joku Oppilaitos"}
                 :organisaatioOid "1.2.246.562.10.48442622063"}]))))

  (testing "returns false if haut is empty"
    (is (= false
           (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
             ["1.2.246.562.10.96162204109"]
             []
             ))))

  (testing "returns false if hakukohteet is empty"
    (is (= false
           (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
             ["1.2.246.562.10.96162204109"]
             [{:tila "julkaistu"
               :hakukohteet []
               :nimi {:fi "Jatkuva haku Gradia Jyväskylä"}
               :organisaatioOid "1.2.246.562.10.48442622063"}]
             ))))

  (testing "returns false if the only hakukohde in the only haku has different jarjestyspaikkaOid than tarjoaja oid"
    (is (= false
           (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
             ["1.2.246.562.10.96162204109"]
             [{:tila "julkaistu"
               :hakukohteet [{:tila "julkaistu"
                              :jarjestyspaikkaOid "1.2.246.562.10.4444444444444"
                              :toteutusOid "1.2.246.562.17.00000000000000006388"
                              :nimi {:fi "Isännöinnin ammattitutkinto"}
                              :hakuOid "1.2.246.562.29.00000000000000000045"
                              :hakukohdeOid "1.2.246.562.20.00000000000000010664"
                              :jarjestaaUrheilijanAmmKoulutusta true
                              :organisaatioOid "1.2.246.562.10.11111111111"}]
               :nimi {:fi "Jatkuva haku Joku Oppilaitos"}
               :organisaatioOid "1.2.246.562.10.48442622063"}]))))

  (testing "returns true when the second hakukohde in the only haku has correct jarjestyspaikkaOid and jarjestaaUrheilijanAmmKoulutusta has true"
    (is (= true
           (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
             ["1.2.246.562.10.96162204109"]
             [{:tila "julkaistu"
               :hakukohteet [{:tila "julkaistu"
                              :jarjestyspaikkaOid "1.2.246.562.10.4444444444444"
                              :toteutusOid "1.2.246.562.17.00000000000000006388"
                              :nimi {:fi "Isännöinnin ammattitutkinto"}
                              :hakuOid "1.2.246.562.29.00000000000000000045"
                              :hakukohdeOid "1.2.246.562.20.00000000000000010664"
                              :jarjestaaUrheilijanAmmKoulutusta false
                              :organisaatioOid "1.2.246.562.10.11111111111"}
                             {:tila "julkaistu"
                              :jarjestyspaikkaOid "1.2.246.562.10.96162204109"
                              :toteutusOid "1.2.246.562.17.00000000000000006388"
                              :nimi {:fi "Isännöinnin ammattitutkinto"}
                              :hakuOid "1.2.246.562.29.00000000000000000045"
                              :hakukohdeOid "1.2.246.562.20.00000000000000010664"
                              :jarjestaaUrheilijanAmmKoulutusta true
                              :organisaatioOid "1.2.246.562.10.96162204109"}]
               :nimi {:fi "Jatkuva haku Joku Oppilaitos"}
               :organisaatioOid "1.2.246.562.10.48442622063"}]))))

  (testing "returns true when some of the tarjoajat has hakukohde that has true for jarjestaaUrheilijanAmmKoulutusta"
    (is (= true
           (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
             ["1.2.246.562.10.12121212121" "1.2.246.562.10.96162204109"]
             [{:tila "julkaistu"
               :hakukohteet [{:tila "julkaistu"
                              :jarjestyspaikkaOid "1.2.246.562.10.4444444444444"
                              :toteutusOid "1.2.246.562.17.00000000000000006388"
                              :nimi {:fi "Isännöinnin ammattitutkinto"}
                              :hakuOid "1.2.246.562.29.00000000000000000045"
                              :hakukohdeOid "1.2.246.562.20.00000000000000010664"
                              :jarjestaaUrheilijanAmmKoulutusta false
                              :organisaatioOid "1.2.246.562.10.11111111111"}
                             {:tila "julkaistu"
                              :jarjestyspaikkaOid "1.2.246.562.10.4444444444444"
                              :toteutusOid "1.2.246.562.17.00000000000000006388"
                              :nimi {:fi "Isännöinnin ammattitutkinto"}
                              :hakuOid "1.2.246.562.29.00000000000000000045"
                              :hakukohdeOid "1.2.246.562.20.00000000000000010664"
                              :jarjestaaUrheilijanAmmKoulutusta false
                              :organisaatioOid "1.2.246.562.10.12121212121"}
                             {:tila "julkaistu"
                              :jarjestyspaikkaOid "1.2.246.562.10.4444444444555"
                              :toteutusOid "1.2.246.562.17.00000000000000006399"
                              :nimi {:fi "Hevosammattitutkinto"}
                              :hakuOid "1.2.246.562.29.00000000000000000045"
                              :hakukohdeOid "1.2.246.562.20.00000000000000010666"
                              :jarjestaaUrheilijanAmmKoulutusta false
                              :organisaatioOid "1.2.246.562.10.96162204109"}
                             {:tila "julkaistu"
                              :jarjestyspaikkaOid "1.2.246.562.10.96162204109"
                              :toteutusOid "1.2.246.562.17.00000000000000006388"
                              :nimi {:fi "Isännöinnin ammattitutkinto"}
                              :hakuOid "1.2.246.562.29.00000000000000000045"
                              :hakukohdeOid "1.2.246.562.20.00000000000000010664"
                              :jarjestaaUrheilijanAmmKoulutusta true
                              :organisaatioOid "1.2.246.562.10.96162204109"}]
               :nimi {:fi "Yhteishaku"}
               :organisaatioOid "1.2.246.562.10.48442622063"}]))))
    )
