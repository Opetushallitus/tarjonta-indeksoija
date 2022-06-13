(ns kouta-indeksoija-service.indexer.tools.general-test-fast
  (:require [clojure.test :refer [deftest testing is]]
            [kouta-indeksoija-service.indexer.tools.general :as general]))

(deftest tuva?
  (testing "returns false for koulutustyyppi that is not tuva"
    (let [koulutus {:tila "julkaistu"
                    :johtaaTutkintoon true
                    :nimi {:fi "Autoalan perustutkinto 0 fi" :sv "Autoalan perustutkinto 0 sv"}
                    :metadata {:tyyppi "amm" :kuvaus {:fi "Kuvaus fi" :sv "Kuvaus sv"}}
                    :koulutustyyppi "amm"}]
      (is (= false
             (general/tuva? koulutus)))))

  (testing "returns true for tuva koulutustyyppi"
    (let [koulutus {:tila "julkaistu"
                    :johtaaTutkintoon false
                    :nimi {:fi "Tutkintokoulutukseen valmentava koulutus (TUVA)" , :sv "Tutkintokoulutukseen valmentava koulutus (TUVA)"}
                    :metadata {:kuvaus {:fi "Kuvaus fi" :sv "Kuvaus sv"}}
                    :koulutustyyppi "tuva"}]
      (is (= true
             (general/tuva? koulutus))))))

(deftest remove-version-from-koodiuri
  (testing "removes version from hakutapa koodiuri"
    (let [haku {:hakutapa {:koodiUri "hakutapa_02#1" :nimi {:sv "Separata antagningar" :en "Separate application" :fi "Erillishaku"}}
                :metadata {:koulutuksenAlkamiskausi {:alkamiskausityyppi "alkamiskausi ja -vuosi"
                                                     :henkilokohtaisenSuunnitelmanLisatiedot {}
                                                     :koulutuksenAlkamiskausi {:koodiUri "kausi_s#1"
                                                                               :nimi {:sv "Höst" :en "Autumn" :fi "Syksy"}}
                                                     :koulutuksenAlkamisvuosi "2022"}}}
          result {:hakutapa {:koodiUri "hakutapa_02" :nimi {:sv "Separata antagningar" :en "Separate application" :fi "Erillishaku"}}
                  :metadata {:koulutuksenAlkamiskausi {:alkamiskausityyppi "alkamiskausi ja -vuosi"
                                                       :henkilokohtaisenSuunnitelmanLisatiedot {}
                                                       :koulutuksenAlkamiskausi {:koodiUri "kausi_s#1"
                                                                                 :nimi {:sv "Höst" :en "Autumn" :fi "Syksy"}}
                                                       :koulutuksenAlkamisvuosi "2022"}}}]
      (is (= result
             (general/remove-version-from-koodiuri haku [:hakutapa :koodiUri])))))

  (testing "removes version from koulutuksenAlkamiskausi koodiuri"
    (let [haku {:hakutapa {:koodiUri "hakutapa_02#1" :nimi {:sv "Separata antagningar" :en "Separate application" :fi "Erillishaku"}}
                :metadata {:koulutuksenAlkamiskausi {:alkamiskausityyppi "alkamiskausi ja -vuosi"
                                                     :henkilokohtaisenSuunnitelmanLisatiedot {}
                                                     :koulutuksenAlkamiskausi {:koodiUri "kausi_s#1"
                                                                               :nimi {:sv "Höst" :en "Autumn" :fi "Syksy"}}
                                                     :koulutuksenAlkamisvuosi "2022"}}}
          result {:hakutapa {:koodiUri "hakutapa_02#1" :nimi {:sv "Separata antagningar" :en "Separate application" :fi "Erillishaku"}}
                  :metadata {:koulutuksenAlkamiskausi {:alkamiskausityyppi "alkamiskausi ja -vuosi"
                                                       :henkilokohtaisenSuunnitelmanLisatiedot {}
                                                       :koulutuksenAlkamiskausi {:koodiUri "kausi_s"
                                                                                 :nimi {:sv "Höst" :en "Autumn" :fi "Syksy"}}
                                                       :koulutuksenAlkamisvuosi "2022"}}}]
      (is (= (general/remove-version-from-koodiuri haku [:metadata :koulutuksenAlkamiskausi :koulutuksenAlkamiskausi :koodiUri])
             result))))

  (testing "does not try to remove version from koulutuksenAlkamiskausi koodiuri if it does not exist"
    (let [haku {:hakutapa {:koodiUri "hakutapa_02#1" :nimi {:sv "Separata antagningar" :en "Separate application" :fi "Erillishaku"}}
                :metadata {:koulutuksenAlkamiskausi {:alkamiskausityyppi "alkamiskausi ja -vuosi"
                                                     :henkilokohtaisenSuunnitelmanLisatiedot {}
                                                     :koulutuksenAlkamiskausi {}
                                                     :koulutuksenAlkamisvuosi "2022"}}}
          result {:hakutapa {:koodiUri "hakutapa_02#1" :nimi {:sv "Separata antagningar" :en "Separate application" :fi "Erillishaku"}}
                  :metadata {:koulutuksenAlkamiskausi {:alkamiskausityyppi "alkamiskausi ja -vuosi"
                                                       :henkilokohtaisenSuunnitelmanLisatiedot {}
                                                       :koulutuksenAlkamiskausi {}
                                                       :koulutuksenAlkamisvuosi "2022"}}}]
      (is (= (general/remove-version-from-koodiuri haku [:metadata :koulutuksenAlkamiskausi :koulutuksenAlkamiskausi :koodiUri])
             result)))))

(deftest not-poistettu?
  (testing "returns true for entry with tila as tallennettu"
    (let [entry {:tila "tallennettu"}]
      (is (= true (general/not-poistettu? entry)))))

  (testing "returns false for entry with tila as poistettu"
    (let [entry {:tila "poistettu"}]
      (is (= false (general/not-poistettu? entry)))))

  (testing "returns false for entry that doesn't exist"
    (is (= false (general/not-poistettu? nil))))

  (testing "returns false for entry that doesn't have tila set"
    (is (= false (general/not-poistettu? {}))))
  )
