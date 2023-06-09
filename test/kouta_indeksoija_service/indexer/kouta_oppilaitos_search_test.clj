(ns kouta-indeksoija-service.indexer.kouta-oppilaitos-search-test
  (:require [clojure.test :refer [deftest testing is run-tests]]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos-search]))

(def oppilaitos
  {:children [{:oid "1.2.246.562.10.50472236657"
               :status "AKTIIVINEN"
               :organisaatiotyypit ["organisaatiotyyppi_03"]
               :parentOid "1.2.246.562.10.31756159625"
               :nimi {:fi "Opisto"
                      :sv "Opisto"
                      :en "Opisto"}
               :kotipaikkaUri "kunta_086"
               :kieletUris ["oppilaitoksenopetuskieli_1#1"]}]
   :kieletUris ["oppilaitoksenopetuskieli_1#1"]
   :organisaatiotyypit ["organisaatiotyyppi_02"]
   :parentOid "1.2.246.562.10.69445412302"
   :nimi {:sv "Opisto"
          :fi "Opisto"
          :en "Opisto"}
   :oid "1.2.246.562.10.31756159625"
   :oppilaitostyyppi "oppilaitostyyppi_63#1"
   :status "AKTIIVINEN"
   :kotipaikkaUri "kunta_086"})

(def koulutus
  {:tila "julkaistu"
   :johtaaTutkintoon false
   :teemakuva "https://konfo-files.opintopolku.fi/koulutus-teemakuva/1.2.246.562.13.00000000000000002123/08e7d481-e5f5-432e-bbd7-9272be936ddc.jpg"
   :tarjoajat ["1.2.246.562.10.31756159625"]
   :esikatselu false
   :modified "2022-11-02T15:24:05"
   :koulutuksetKoodiUri ["koulutus_999909#1"]
   :nimi {:fi "Opistovuosi oppivelvollisille kansanopistoissa"
          :sv "Folkhögskoleåret för läropliktiga"}
   :muokkaaja "1.2.246.562.24.16027275695"
   :oid "1.2.246.562.13.00000000000000002123"
   :_enrichedData {:muokkaajanNimi "Minna Muokkaaja"}
   :kielivalinta ["fi" "sv"]
   :julkinen true
   :metadata {:tyyppi "vapaa-sivistystyo-opistovuosi"
              :lisatiedot []
              :kuvaus {:fi "Kuvaus fi"
                       :sv "Kuvaus sv"}
              :linkkiEPerusteisiin {:fi "https://eperusteet.opintopolku.fi/beta/#/fi/vapaasivistystyo/7512390/tiedot"
                                    :sv "https://eperusteet.opintopolku.fi/beta/#/sv/vapaasivistystyo/7512390/tiedot"}
              :koulutusalaKoodiUrit ["kansallinenkoulutusluokitus2016koulutusalataso2_001#1"]
              :opintojenLaajuusNumero 53.0
              :opintojenLaajuusyksikkoKoodiUri "opintojenlaajuusyksikko_2#1"
              :isMuokkaajaOphVirkailija true}
   :organisaatioOid "1.2.246.562.10.00000000001"
   :koulutustyyppi "vapaa-sivistystyo-opistovuosi"})

(def toteutus
  {:tila "julkaistu"
   :teemakuva "https://konfo-files.opintopolku.fi/toteutus-teemakuva/1.2.246.562.17.00000000000000010979/ed43e1d1-7aa5-4b2b-a4b5-6d20581eeffb.jpg"
   :tarjoajat ["1.2.246.562.10.31756159625" "1.2.246.562.10.50472236657"]
   :esikatselu false
   :koulutusOid "1.2.246.562.13.00000000000000002123"
   :koulutuksetKoodiUri []
   :nimi {:fi "Opistovuosi oppivelvollisille kansanopistoissa"}
   :muokkaaja "1.2.246.562.24.29407417604"
   :oid "1.2.246.562.17.00000000000000010979"
   :_enrichedData {:esitysnimi {:fi "Opistovuosi oppivelvollisille kansanopistoissa"}}
   :kielivalinta ["fi"]
   :metadata {:kuvaus {:fi "Kuvaus"}
              :isMuokkaajaOphVirkailija false
              :ammattinimikkeet []
              :asiasanat [{:kieli "fi" :arvo "oppivelvollinen"}
                          {:kieli "fi" :arvo "opistovuosi oppivelvollisille"}]
              :isTyovoimakoulutus false
              :hasJotpaRahoitus false
              :isTaydennyskoulutus false
              :opetus {:opetustapaKoodiUrit ["opetuspaikkakk_1#1"]
                       :maksullisuustyyppi "maksuton" :maksullisuusKuvaus {:fi "Maksullisuuskuvaus"}
                       :opetuskieliKoodiUrit ["oppilaitoksenopetuskieli_1#2"]
                       :suunniteltuKestoKuvaus {:fi "Keston kuvaus"}
                       :koulutuksenAlkamiskausi {:alkamiskausityyppi "tarkka alkamisajankohta"
                                                 :henkilokohtaisenSuunnitelmanLisatiedot {}
                                                 :koulutuksenAlkamispaivamaara "2023-08-13T10:00"
                                                 :koulutuksenPaattymispaivamaara "2024-05-31T12:00"}
                       :suunniteltuKestoVuodet 1
                       :onkoApuraha false
                       :opetusaikaKoodiUrit ["opetusaikakk_1#1"]
                       :lisatiedot []
                       :suunniteltuKestoKuukaudet 0
                       :opetustapaKuvaus {:fi "Opetustapakuvaus"}
                       :opetuskieletKuvaus {:fi "Opetuskielenä on suomi."}
                       :opetusaikaKuvaus {:fi "Opetusaikakuvaus"}}
              :yhteyshenkilot [{:nimi {:fi "Minna Muokkaaja"}
                                :titteli {:fi "Linjavastaava"}
                                :sahkoposti {:fi "minna.muokkaaja@opisto.fi"}
                                :puhelinnumero {:fi "044 555 5555"}
                                :wwwSivu {:fi "http://www.opisto.fi"}
                                :wwwSivuTeksti {}}]
              :tyyppi "vapaa-sivistystyo-opistovuosi"}
   :organisaatioOid "1.2.246.562.10.31756159625"})

(def oppilaitos-search-terms-result
  {:sijainti ["kunta_086" "maakunta_05"]
   :koulutustyypit ["muu"]
   :lukiopainotukset []
   :koulutus_organisaationimi {:fi "Opisto"
                               :sv "Opisto"
                               :en "Opisto"}
   :opetuskielet ["oppilaitoksenopetuskieli_1"]
   :lukiolinjaterityinenkoulutustehtava []
   :osaamisalat []
   :nimi {:fi "Opisto"
          :sv "Opisto"
          :en "Opisto"}
   :oppilaitosOid "1.2.246.562.10.31756159625",
   :isTyovoimakoulutus false
   :hasJotpaRahoitus false
   :isTaydennyskoulutus false
   :metadata {:kunnat
              [{:koodiUri "kunta_086",
                :nimi {:sv "Hausjärvi", :fi "Hausjärvi"}}]}})

(def koulutus-search-terms-result
  {:sijainti ["kunta_086" "maakunta_05"]
   :koulutustyypit ["vapaa-sivistystyo" "vapaa-sivistystyo-opistovuosi"]
   :lukiopainotukset []
   :koulutus_organisaationimi {:fi "Opisto"
                               :sv "Opisto"
                               :en "Opisto"}
   :opetuskielet ["oppilaitoksenopetuskieli_1"]
   :lukiolinjaterityinenkoulutustehtava []
   :osaamisalat []
   :koulutusOid "1.2.246.562.13.00000000000000002123"
   :nimi {:fi "Opistovuosi oppivelvollisille kansanopistoissa"
          :sv "Folkhögskoleåret för läropliktiga"}
   :koulutusalat
   ["kansallinenkoulutusluokitus2016koulutusalataso2_001"]
   :kuva
   "https://konfo-files.opintopolku.fi/koulutus-teemakuva/1.2.246.562.13.00000000000000002123/08e7d481-e5f5-432e-bbd7-9272be936ddc.jpg"
   :onkoTuleva true
   :oppilaitosOid "1.2.246.562.10.31756159625"
   :isTyovoimakoulutus false
   :hasJotpaRahoitus false
   :isTaydennyskoulutus false
   :koulutusnimi
   {:fi "Opistovuosi oppivelvollisille kansanopistoissa"
    :sv "Folkhögskoleåret för läropliktiga"}
   :metadata {:koulutustyypit []
              :opintojenLaajuusyksikko
              {:koodiUri "opintojenlaajuusyksikko_2#1"
               :nimi
               {:en "ECTS credits"
                :fi "opintopistettä"
                :sv "studiepoäng"}}
              :opintojenLaajuusNumero 53.0
              :kunnat
              [{:koodiUri "kunta_086"
                :nimi {:sv "Hausjärvi" :fi "Hausjärvi"}}]
              :tutkintonimikkeet []
              :koulutustyyppi "vapaa-sivistystyo-opistovuosi"}})
(def toteutus-search-terms-result
  {:sijainti ["kunta_086" "maakunta_05"]
   :koulutustyypit ["vapaa-sivistystyo" "vapaa-sivistystyo-opistovuosi"]
   :lukiopainotukset []
   :koulutus_organisaationimi {:fi "Opisto"
                               :sv "Opisto"
                               :en "Opisto"}
   :opetuskielet ["oppilaitoksenopetuskieli_1"]
   :lukiolinjaterityinenkoulutustehtava []
   :osaamisalat []
   :koulutusOid "1.2.246.562.13.00000000000000002123"
   :toteutusOid "1.2.246.562.17.00000000000000010979"
   :asiasanat {:fi ["oppivelvollinen" "opistovuosi oppivelvollisille"]}
   :nimi {:fi "Opistovuosi oppivelvollisille kansanopistoissa"}
   :koulutusalat ["kansallinenkoulutusluokitus2016koulutusalataso2_001"]
   :kuva "https://konfo-files.opintopolku.fi/toteutus-teemakuva/1.2.246.562.17.00000000000000010979/ed43e1d1-7aa5-4b2b-a4b5-6d20581eeffb.jpg"
   :opetustavat ["opetuspaikkakk_1"]
   :onkoTuleva false
   :oppilaitosOid "1.2.246.562.10.31756159625"
   :toteutus_organisaationimi {:fi ["Opisto"]
                               :sv ["Opisto"]
                               :en ["Opisto"]}
   :isTyovoimakoulutus false
   :hasJotpaRahoitus false
   :isTaydennyskoulutus false
   :koulutusnimi {:fi "Opistovuosi oppivelvollisille kansanopistoissa"
                  :sv "Folkhögskoleåret för läropliktiga"}
   :jarjestaaUrheilijanAmmKoulutusta false
   :metadata {:tutkintonimikkeet []
              :opetusajat [{:koodiUri "opetusaikakk_1#1"
                            :nimi {:sv "Dagundervisning"
                                   :en "Day time teaching"
                                   :fi "Päiväopetus"}}]
              :maksullisuustyyppi "maksuton"
              :koulutustyyppi "vapaa-sivistystyo-opistovuosi"
              :kunnat [{:koodiUri "kunta_086"
                        :nimi {:sv "Hausjärvi" :fi "Hausjärvi"}}]}
   :toteutusNimi {:fi "Opistovuosi oppivelvollisille kansanopistoissa"}})

(deftest oppilaitos-search-terms
  (testing "returns search-terms for oppilaitos without koulutukset"
    (is (= oppilaitos-search-terms-result
           (oppilaitos-search/oppilaitos-search-terms oppilaitos)))))

(deftest koulutus-search-terms
  (testing "returns empty map if empty list given as a parameter"
    (is (= koulutus-search-terms-result
           (oppilaitos-search/koulutus-search-terms oppilaitos koulutus)))))

(deftest toteutus-search-terms
  (testing "returns empty map if empty list given as a parameter"
    (is (= toteutus-search-terms-result
           (oppilaitos-search/toteutus-search-terms oppilaitos koulutus [] toteutus)))))

(deftest search-terms
  (testing "returns a search-term for oppilaitos without any koulutus"
    (is (= oppilaitos-search-terms-result
           (oppilaitos-search/search-terms oppilaitos nil nil))))

  (testing "returns a search-term for oppilaitos with one koulutus"
    (is (= koulutus-search-terms-result
           (oppilaitos-search/search-terms oppilaitos koulutus nil))))

  (testing "returns empty map if empty list given as a parameter"
    (is (= toteutus-search-terms-result
           (oppilaitos-search/search-terms oppilaitos koulutus toteutus)))))
(run-tests)
