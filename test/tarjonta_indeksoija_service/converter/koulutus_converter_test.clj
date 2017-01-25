(ns tarjonta-indeksoija-service.converter.koulutus-converter-test
  (:require [midje.sweet :refer :all]
            [tarjonta-indeksoija-service.converter.koulutus-converter :as converter]))

;; Abysmal data structures from tarjonta koulutus api
(let [koodi {:koulutustyyppi {:uri    "koulutustyyppi_3"
                              :versio 2
                              :arvo   "3"
                              :nimi   "Korkeakoulutus"
                              :meta   {:kieli_sv {:kieliUri    "kieli_sv"
                                                  :kieliVersio 1
                                                  :kieliArvo   "SV"
                                                  :versio      1
                                                  :nimi        "Högskoleexamen"}
                                       :kieli_fi {:kieliUri    "kieli_fi"
                                                  :kieliVersio 1
                                                  :kieliArvo   "FI"
                                                  :versio      1
                                                  :nimi        "Korkeakoulutus"}}}}
      expected-koodi {:koulutustyyppi {:uri  "koulutustyyppi_3"
                                       :nimi {:fi "Korkeakoulutus"
                                              :sv "Högskoleexamen"
                                              :en nil}}}

      koodi-list {:opetuskielis {:versio 1
                                 :meta   {:kieli_fi {:uri    "kieli_fi"
                                                     :versio 1
                                                     :arvo   "FI"
                                                     :nimi   "suomi"
                                                     :meta   {:kieli_fi {:kieliUri    "kieli_fi"
                                                                         :kieliVersio 1
                                                                         :kieliArvo   "FI"
                                                                         :versio      1
                                                                         :nimi        "suomi"}
                                                              :kieli_sv {:kieliUri    "kieli_sv"
                                                                         :kieliVersio 1
                                                                         :kieliArvo   "SV"
                                                                         :versio      1
                                                                         :nimi        "finska"}
                                                              :kieli_en {:kieliUri    "kieli_en"
                                                                         :kieliVersio 1
                                                                         :kieliArvo   "EN"
                                                                         :versio      1
                                                                         :nimi        "Finnish"}}}
                                          :kieli_sv {:uri    "kieli_sv"
                                                     :versio 1
                                                     :arvo   "SV"
                                                     :nimi   "ruotsi"
                                                     :meta   {:kieli_fi {:kieliUri    "kieli_fi"
                                                                         :kieliVersio 1
                                                                         :kieliArvo   "FI"
                                                                         :versio      1
                                                                         :nimi        "ruotsi"}
                                                              :kieli_sv {:kieliUri    "kieli_sv"
                                                                         :kieliVersio 1
                                                                         :kieliArvo   "SV"
                                                                         :versio      1
                                                                         :nimi        "svenska"}
                                                              :kieli_en {:kieliUri    "kieli_en"
                                                                         :kieliVersio 1
                                                                         :kieliArvo   "EN"
                                                                         :versio      1
                                                                         :nimi        "Swedish"}}}}
                                 :uris   {:kieli_fi 1
                                          :kieli_sv 1}}}
      expected-koodi-list {:opetuskielis [{:uri  "kieli_fi"
                                           :nimi {:fi "suomi"
                                                  :sv "finska"
                                                  :en "Finnish"}},
                                          {:uri  "kieli_sv"
                                           :nimi {:fi "ruotsi"
                                                  :sv "svenska"
                                                  :en "Swedish"}}]}


      kuvaus {:kuvausKomo {:JATKOOPINTO_MAHDOLLISUUDET {:versio  1
                                                        :meta    {:kieli_sv {:kieliUri    "kieli_sv"
                                                                             :kieliVersio 1
                                                                             :kieliArvo   "SV"
                                                                             :versio      1}
                                                                  :kieli_fi {:kieliUri    "kieli_fi"
                                                                             :kieliVersio 1
                                                                             :kieliArvo   "FI"
                                                                             :versio      1}}
                                                        :tekstis {:kieli_sv "<p>JATKOOPINTO_MAHDOLLISUUDET_SV</p>"
                                                                  :kieli_fi "<p>JATKOOPINTO_MAHDOLLISUUDET_FI</p>"}}
                           :TAVOITTEET                 {:versio              1
                                                        :meta                {:kieli_sv {:kieliUri    "kieli_sv"
                                                                                         :kieliVersio 1
                                                                                         :kieliArvo   "SV"
                                                                                         :versio      1}
                                                                              :kieli_fi {:kieliUri    "kieli_fi"
                                                                                         :kieliVersio 1
                                                                                         :kieliArvo   "FI"
                                                                                         :versio      1}}
                                                        :tekstis             {:kieli_sv "<p>TAVOITTEET_SV</p>"
                                                                              :kieli_fi "<p>TAVOITTEET_FI</p>"}
                                                        :KOULUTUKSEN_RAKENNE {:versio  1
                                                                              :meta    {:kieli_sv {:kieliUri    "kieli_sv"
                                                                                                   :kieliVersio 1
                                                                                                   :kieliArvo   "SV"
                                                                                                   :versio      1}
                                                                                        :kieli_fi {:kieliUri    "kieli_fi"
                                                                                                   :kieliVersio 1
                                                                                                   :kieliArvo   "FI"
                                                                                                   :versio      1}}
                                                                              :tekstis {:kieli_sv "<p>KOULUTUKSEN_RAKENNE_SV</p>"
                                                                                        :kieli_fi "<p>KOULUTUKSEN_RAKENNE_FI</p>"}}}}}
      expected-kuvaus {:kuvausKomo {:TAVOITTEET                 {:kieli_sv "<p>TAVOITTEET_SV</p>"
                                                                 :kieli_fi "<p>TAVOITTEET_FI</p>"}
                                    :JATKOOPINTO_MAHDOLLISUUDET {:kieli_sv "<p>JATKOOPINTO_MAHDOLLISUUDET_SV</p>"
                                                                 :kieli_fi "<p>JATKOOPINTO_MAHDOLLISUUDET_FI</p>"}
                                    :KOULUTUKSEN_RAKENNE        {:kieli_sv "<p>KOULUTUKSEN_RAKENNE_SV</p>"
                                                                 :kieli_fi "<p>KOULUTUKSEN_RAKENNE_FI</p>"}}}


      kielivalikoima {:kielivalikoima {:B2KIELI {:versio 1
                                                 :meta {:kieli_la {:uri "kieli_la"
                                                                   :versio 1
                                                                   :arvo "LA"
                                                                   :nimi "latina"
                                                                   :meta {:kieli_sv {:kieliUri "kieli_sv"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "SV"
                                                                                     :versio 1
                                                                                     :nimi "latin"}
                                                                          :kieli_fi {:kieliUri "kieli_fi"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "FI"
                                                                                     :versio 1
                                                                                     :nimi "latina"}
                                                                          :kieli_en {:kieliUri "kieli_en"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "EN"
                                                                                     :versio 1
                                                                                     :nimi "Latin"}}}}
                                                 :uris {:kieli_la 1}}
                                       :B3KIELI {:versio 1
                                                 :meta {:kieli_es {:uri "kieli_es"
                                                                   :versio 1
                                                                   :arvo "ES"
                                                                   :nimi "espanja"
                                                                   :meta {:kieli_fi {:kieliUri "kieli_fi"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "FI"
                                                                                     :versio 1
                                                                                     :nimi "espanja"}
                                                                          :kieli_sv {:kieliUri "kieli_sv"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "SV"
                                                                                     :versio 1
                                                                                     :nimi "spanska"}
                                                                          :kieli_en {:kieliUri "kieli_en"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "EN"
                                                                                     :versio 1
                                                                                     :nimi "Spanish"}}}
                                                        :kieli_de {:uri "kieli_de"
                                                                   :versio 1
                                                                   :arvo "DE"
                                                                   :nimi "saksa"
                                                                   :meta {:kieli_fi {:kieliUri "kieli_fi"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "FI"
                                                                                     :versio 1
                                                                                     :nimi "saksa"}
                                                                          :kieli_sv {:kieliUri "kieli_sv"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "SV"
                                                                                     :versio 1
                                                                                     :nimi "tyska"}
                                                                          :kieli_en {:kieliUri "kieli_en"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "EN"
                                                                                     :versio 1
                                                                                     :nimi "German"}}}}
                                                 :uris {:kieli_es 1
                                                        :kieli_de 1}}
                                       :A1A2KIELI {:versio 1
                                                   :meta {:kieli_de {:uri "kieli_de"
                                                                     :versio 1
                                                                     :arvo "DE"
                                                                     :nimi "saksa"
                                                                     :meta {:kieli_fi {:kieliUri "kieli_fi"
                                                                                       :kieliVersio 1
                                                                                       :kieliArvo "FI"
                                                                                       :versio 1
                                                                                       :nimi "saksa"}
                                                                            :kieli_sv {:kieliUri "kieli_sv"
                                                                                       :kieliVersio 1
                                                                                       :kieliArvo "SV"
                                                                                       :versio 1
                                                                                       :nimi "tyska"}
                                                                            :kieli_en {:kieliUri "kieli_en"
                                                                                       :kieliVersio 1
                                                                                       :kieliArvo "EN"
                                                                                       :versio 1
                                                                                       :nimi "German"}}}}
                                                   :uris {:kieli_de 1}}
                                       :B1KIELI {:versio 1
                                                 :meta {:kieli_sv {:uri "kieli_sv"
                                                                   :versio 1
                                                                   :arvo "SV"
                                                                   :nimi "ruotsi"
                                                                   :meta {:kieli_fi {:kieliUri "kieli_fi"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "FI"
                                                                                     :versio 1
                                                                                     :nimi "ruotsi"}
                                                                          :kieli_sv {:kieliUri "kieli_sv"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "SV"
                                                                                     :versio 1
                                                                                     :nimi "svenska"}
                                                                          :kieli_en {:kieliUri "kieli_en"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "EN"
                                                                                     :versio 1
                                                                                     :nimi "Swedish"}}}
                                                        :kieli_en {:uri "kieli_en"
                                                                   :versio 1
                                                                   :arvo "EN"
                                                                   :nimi "englanti"
                                                                   :meta {:kieli_sv {:kieliUri "kieli_sv"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "SV"
                                                                                     :versio 1
                                                                                     :nimi "engelska"}
                                                                          :kieli_fi {:kieliUri "kieli_fi"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "FI"
                                                                                     :versio 1
                                                                                     :nimi "englanti"}
                                                                          :kieli_en {:kieliUri "kieli_en"
                                                                                     :kieliVersio 1
                                                                                     :kieliArvo "EN"
                                                                                     :versio 1
                                                                                     :nimi "English"}}}}
                                                 :uris {:kieli_sv 1
                                                        :kieli_en 1}}}}
      expected-kielivalikoima {:kielivalikoima {
                                                :B2KIELI [{:uri "kieli_la"
                                                           :nimi {:fi "latina"
                                                                  :sv "latin"
                                                                  :en "Latin"}}]
                                                :B3KIELI [{:uri "kieli_es"
                                                           :nimi {:fi "espanja"
                                                                  :sv "spanska"
                                                                  :en "Spanish"}}
                                                          {:uri "kieli_de"
                                                           :nimi {:fi "saksa"
                                                                  :sv "tyska"
                                                                  :en "German"}}]
                                                :A1A2KIELI [{:uri "kieli_de"
                                                             :nimi {:fi "saksa"
                                                                    :sv "tyska"
                                                                    :en "German"}}]
                                                :B1KIELI [{:uri "kieli_sv"
                                                           :nimi {:fi "ruotsi"
                                                                  :sv "svenska"
                                                                  :en "Swedish"}}
                                                          {:uri "kieli_en"
                                                           :nimi {:fi "englanti"
                                                                  :sv "engelska"
                                                                  :en "English"}}]}}
      valmistava-koulutus {:valmistavaKoulutus {:kuvaus {:KOHDERYHMA {:versio 1,
                                                                      :meta {:kieli_fi {:kieliUri "kieli_fi",
                                                                                        :kieliVersio 1,
                                                                                        :kieliArvo "FI",
                                                                                        :versio 1}},
                                                                      :tekstis {:kieli_fi ""}},
                                                         :OPISKELUN_HENKILOKOHTAISTAMINEN {:versio 1,
                                                                                           :meta {:kieli_fi {:kieliUri "kieli_fi",
                                                                                                             :kieliVersio 1,
                                                                                                             :kieliArvo "FI",
                                                                                                             :versio 1}},
                                                                                           :tekstis {:kieli_fi ""}},
                                                         :KANSAINVALISTYMINEN {:versio 1,
                                                                               :meta {:kieli_fi {:kieliUri "kieli_fi",
                                                                                                 :kieliVersio 1,
                                                                                                 :kieliArvo "FI",
                                                                                                 :versio 1}},
                                                                               :tekstis {:kieli_fi ""}},
                                                         :SISALTO {:versio 1,
                                                                   :meta {:kieli_fi {:kieliUri "kieli_fi",
                                                                                     :kieliVersio 1,
                                                                                     :kieliArvo "FI",
                                                                                     :versio 1}},
                                                                   :tekstis {:kieli_fi "<p>SISALTO_FI</p>"}}},
                                                :suunniteltuKestoArvo "0-2",
                                                :suunniteltuKestoTyyppi {:uri "suunniteltukesto_01",
                                                                         :versio 1,
                                                                         :arvo "01",
                                                                         :nimi "vuotta",
                                                                         :meta {:kieli_fi {:kieliUri "kieli_fi",
                                                                                           :kieliVersio 1,
                                                                                           :kieliArvo "FI",
                                                                                           :versio 1,
                                                                                           :nimi "vuotta"},
                                                                                :kieli_sv {:kieliUri "kieli_sv",
                                                                                           :kieliVersio 1,
                                                                                           :kieliArvo "SV",
                                                                                           :versio 1,
                                                                                           :nimi "år"},
                                                                                :kieli_en {:kieliUri "kieli_en",
                                                                                           :kieliVersio 1,
                                                                                           :kieliArvo "EN",
                                                                                           :versio 1,
                                                                                           :nimi "years"}}},
                                                :hintaString "",
                                                :opintojenMaksullisuus false,
                                                :linkkiOpetussuunnitelmaan "",
                                                :opetusmuodos {:versio 1,
                                                               :meta {:opetusmuotokk_3 {:uri "opetusmuotokk_3",
                                                                                        :versio 1,
                                                                                        :arvo "3",
                                                                                        :nimi "Monimuoto-opetus",
                                                                                        :meta {:kieli_sv {:kieliUri "kieli_sv",
                                                                                                          :kieliVersio 1,
                                                                                                          :kieliArvo "SV",
                                                                                                          :versio 1,
                                                                                                          :nimi "Flerformsundervisning"},
                                                                                               :kieli_fi {:kieliUri "kieli_fi",
                                                                                                          :kieliVersio 1,
                                                                                                          :kieliArvo "FI",
                                                                                                          :versio 1,
                                                                                                          :nimi "Monimuoto-opetus"},
                                                                                               :kieli_en {:kieliUri "kieli_en",
                                                                                                          :kieliVersio 1,
                                                                                                          :kieliArvo "EN",
                                                                                                          :versio 1,
                                                                                                          :nimi "Blended learning"}}}},
                                                               :uris {:opetusmuotokk_3 1}},
                                                :opetusAikas {:versio 1,
                                                              :meta {:opetusaikakk_1 {:uri "opetusaikakk_1",
                                                                                      :versio 1,
                                                                                      :arvo "1",
                                                                                      :nimi "Päiväopetus",
                                                                                      :meta {:kieli_sv {:kieliUri "kieli_sv",
                                                                                                        :kieliVersio 1,
                                                                                                        :kieliArvo "SV",
                                                                                                        :versio 1,
                                                                                                        :nimi "Dagundervisning"},
                                                                                             :kieli_fi {:kieliUri "kieli_fi",
                                                                                                        :kieliVersio 1,
                                                                                                        :kieliArvo "FI",
                                                                                                        :versio 1,
                                                                                                        :nimi "Päiväopetus"},
                                                                                             :kieli_en {:kieliUri "kieli_en",
                                                                                                        :kieliVersio 1,
                                                                                                        :kieliArvo "EN",
                                                                                                        :versio 1,
                                                                                                        :nimi "Day time teaching"}}}},
                                                              :uris {:opetusaikakk_1 1}},
                                                :opetusPaikkas {:versio 1,
                                                                :meta {:opetuspaikkakk_1 {:uri "opetuspaikkakk_1",
                                                                                          :versio 1,
                                                                                          :arvo "1",
                                                                                          :nimi "Lähiopetus",
                                                                                          :meta {:kieli_fi {:kieliUri "kieli_fi",
                                                                                                            :kieliVersio 1,
                                                                                                            :kieliArvo "FI",
                                                                                                            :versio 1,
                                                                                                            :nimi "Lähiopetus"},
                                                                                                 :kieli_sv {:kieliUri "kieli_sv",
                                                                                                            :kieliVersio 1,
                                                                                                            :kieliArvo "SV",
                                                                                                            :versio 1,
                                                                                                            :nimi "Närundervisning"},
                                                                                                 :kieli_en {:kieliUri "kieli_en",
                                                                                                            :kieliVersio 1,
                                                                                                            :kieliArvo "EN",
                                                                                                            :versio 1,
                                                                                                            :nimi "Contact teaching"}}}},
                                                                :uris {:opetuspaikkakk_1 1}},
                                                :yhteyshenkilos [
                                                                 {:nimi "Hakutoimisto",
                                                                  :sahkoposti "hakutoimisto@hyria.fi",
                                                                  :puhelin "020 690 159",
                                                                  :kielet [],
                                                                  :henkiloTyyppi "YHTEYSHENKILO"}
                                                                 ]}}
      expected-valmistava-koulutus {:valmistavaKoulutus {:kuvaus {:KOHDERYHMA {:kieli_fi ""},
                                                                  :KANSAINVALISTYMINEN {:kieli_fi ""},
                                                                  :OPISKELUN_HENKILOKOHTAISTAMINEN {:kieli_fi ""},
                                                                  :SISALTO {:kieli_fi "<p>SISALTO_FI</p>"}},
                                                         :suunniteltuKestoArvo "0-2",
                                                         :hintaString "",
                                                         :opintojenMaksullisuus false,
                                                         :linkkiOpetussuunnitelmaan "",
                                                         :opetusmuodos {:uri "opetusmuotokk_3"
                                                                        :nimi {:fi "Monimuoto-opetus"
                                                                               :sv "Flerformsundervisning"
                                                                               :en "Blended learning"}},
                                                         :opetusAikas {:uri "opetusaikakk_1"
                                                                       :nimi {:fi "Päiväopetus"
                                                                              :sv "Dagundervisning"
                                                                              :en "Day time teaching"}},
                                                         :opetusPaikkas {:uri "opetuspaikkakk_1"
                                                                         :nimi {:fi "Lähiopetus"
                                                                                :sv "Närundervisning"
                                                                                :en "Contact teaching"}},
                                                         :suunniteltuKestoTyyppi {:uri "suunniteltukesto_01",
                                                                                  :nimi {:fi "vuotta",
                                                                                         :sv "år",
                                                                                         :en "years"}}}}
      ]
  (facts "Converter"
         (fact
           "should convert values"
           (let [dto {:oid "1234"}
                 expected {:oid "1234"}]
             (converter/convert dto) => expected))

         (fact "should convert koodi" (converter/convert koodi) => expected-koodi)

         (fact "should convert koodilist" (converter/convert koodi-list) => expected-koodi-list)

         (fact "should convert kuvaus" (converter/convert kuvaus) => expected-kuvaus)

         (fact "should convert kielivalikoima" (converter/convert kielivalikoima) => expected-kielivalikoima)

         (fact "should convert valmistava-koulutus" (converter/convert valmistava-koulutus) => expected-valmistava-koulutus)
         ))