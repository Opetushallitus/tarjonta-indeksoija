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
