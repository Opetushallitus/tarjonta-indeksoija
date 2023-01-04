(ns kouta-indeksoija-service.indexer.search-tests.search-test-fast
  (:require [clojure.test :refer [deftest testing is]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
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

(deftest opintojen-laajuus
  (testing "returns opintojenLaajuusYksikkoKoodiUri for relevant koulutukset"
    (is (= "opintojenlaajuusyksikko_2#1" (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "yo"})))
    (is (= "opintojenlaajuusyksikko_2#1" (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "amk"})))
    (is (= "opintojenlaajuusyksikko_2#1" (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "amm-ope-erityisope-ja-opo"})))
    (is (= "opintojenlaajuusyksikko_2#1" (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "ope-pedag-opinnot"})))
    (is (= "opintojenlaajuusyksikko_2#1" (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "kk-opintojakso"})))
    (is (= "opintojenlaajuusyksikko_2#1" (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "kk-opintokokonaisuus"})))
    (is (= "opintojenlaajuusyksikko_2#1" (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "erikoistumiskoulutus"})))
    (is (= nil (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "erikoislaakari"})))
    (is (= "opintojenlaajuusyksikko_2#1" (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "vapaa-sivistystyo-opistovuosi"})))
    (is (= "opintojenlaajuusyksikko_8#1" (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "tuva"})))
    (is (= "opintojenlaajuusyksikko_6#1" (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "telma"})))
    (is (= "opintojenlaajuusyksikko_2#1" (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "lk"})))
    (is (= "opintojenlaajuusyksikko_8#1" (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "vapaa-sivistystyo-muu" :metadata fixture/vapaa-sivistystyo-muu-metadata})))
    (is (= "opintojenlaajuusyksikko_4#1" (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "amm-muu" :metadata fixture/amm-muu-koulutus-metadata})))
    (is (= "opintojenlaajuusyksikko_2#1" (search/opintojen-laajuusyksikko-koodi-uri {:koulutustyyppi "aikuisten-perusopetus" :metadata fixture/aikuisten-perusopetus-koulutus-metadata})))
  )

  (testing "returns laajuusnumero for relevant koulutukset"
    (is (= 11 (search/opintojen-laajuus-numero {:koulutustyyppi "amm-muu" :metadata fixture/amm-muu-koulutus-metadata})))
    (is (= 26 (search/opintojen-laajuus-numero {:koulutustyyppi "yo" :metadata fixture/yo-koulutus-metadata})))
    (is (= 27 (search/opintojen-laajuus-numero {:koulutustyyppi "amk" :metadata fixture/amk-koulutus-metadata})))
    (is (= 60 (search/opintojen-laajuus-numero {:koulutustyyppi "amm-ope-erityisope-ja-opo" :metadata {:tyyppi "amm-ope-erityisope-ja-opo" :opintojenLaajuusNumero 60}})))
    (is (= 60 (search/opintojen-laajuus-numero {:koulutustyyppi "ope-pedag-opinnot" :metadata {:tyyppi "ope-pedag-opinnot" :opintojenLaajuusNumero 60}})))
    (is (= 38 (search/opintojen-laajuus-numero {:koulutustyyppi "tuva" :metadata fixture/tuva-koulutus-metadata})))
    (is (= 41 (search/opintojen-laajuus-numero {:koulutustyyppi "telma" :metadata fixture/telma-koulutus-metadata})))
    (is (= 25 (search/opintojen-laajuus-numero {:koulutustyyppi "lk" :metadata fixture/lukio-koulutus-metadata})))
    (is (= 38 (search/opintojen-laajuus-numero {:koulutustyyppi "vapaa-sivistystyo-muu" :metadata fixture/vapaa-sivistystyo-muu-metadata})))
    (is (= 20 (search/opintojen-laajuus-numero {:koulutustyyppi "vapaa-sivistystyo-opistovuosi" :metadata {:tyyppi "vapaa-sivistystyo-opistovuosi" :opintojenLaajuusNumero 20}})))
    (is (= 13 (search/opintojen-laajuus-numero {:koulutustyyppi "aikuisten-perusopetus" :metadata fixture/aikuisten-perusopetus-koulutus-metadata})))
    (is (= nil (search/opintojen-laajuus-numero {:koulutustyyppi "erikoislaakari" :metadata {:tyyppi "erikoislaakari" :opintojenLaajuusNumero 60}})))
    (is (= nil (search/opintojen-laajuus-numero {:koulutustyyppi "kk-opintojakso" :metadata {:tyyppi "kk-opintojakso" :opintojenLaajuusNumero 60}})))
    (is (= nil (search/opintojen-laajuus-numero {:koulutustyyppi "kk-opintokokonaisuus" :metadata {:tyyppi "kk-opintokokonaisuus" :opintojenLaajuusNumero 60}})))
    (is (= nil (search/opintojen-laajuus-numero {:koulutustyyppi "erikoistumiskoulutus" :metadata {:tyyppi "erikoistumiskoulutus" :opintojenLaajuusNumero 60}})))
  )

  (testing "return laajuusnumero min and max for relevant koulutukset"
    (is (= 14 (search/opintojen-laajuus-numero-min {:koulutustyyppi "kk-opintojakso" :metadata fixture/kk-opintojakso-koulutus-metadata})))
    (is (= 15 (search/opintojen-laajuus-numero-max {:koulutustyyppi "kk-opintojakso" :metadata fixture/kk-opintojakso-koulutus-metadata})))
    (is (= 24 (search/opintojen-laajuus-numero-min {:koulutustyyppi "kk-opintokokonaisuus" :metadata fixture/kk-opintokokonaisuus-koulutus-metadata})))
    (is (= 25 (search/opintojen-laajuus-numero-max {:koulutustyyppi "kk-opintokokonaisuus" :metadata fixture/kk-opintokokonaisuus-koulutus-metadata})))
    (is (= 5 (search/opintojen-laajuus-numero-min {:koulutustyyppi "erikoistumiskoulutus" :metadata fixture/erikoistumiskoulutus-metadata})))
    (is (= 10 (search/opintojen-laajuus-numero-max {:koulutustyyppi "erikoistumiskoulutus" :metadata fixture/erikoistumiskoulutus-metadata})))
    (is (= nil (search/opintojen-laajuus-numero-min {:koulutustyyppi "yo" :metadata fixture/yo-koulutus-metadata})))
    (is (= nil (search/opintojen-laajuus-numero-max {:koulutustyyppi "yo" :metadata fixture/yo-koulutus-metadata})))
  )
)