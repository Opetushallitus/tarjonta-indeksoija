(ns kouta-indeksoija-service.indexer.kouta-koulutus-test-fast
  (:require [clojure.test :refer [deftest testing is]]
            [kouta-indeksoija-service.indexer.kouta.koulutus :as indexer.kouta.koulutus]))

(deftest enrich-tuva-metadata
  (testing "adds opintojenLaajuusyksikko to tuva metadata"
    (let [koulutus {:tila "julkaistu"
                    :johtaaTutkintoon false
                    :nimi {:fi "Tutkintokoulutukseen valmentava koulutus (TUVA)" , :sv "Tutkintokoulutukseen valmentava koulutus (TUVA)"}
                    :metadata {:kuvaus {:fi "Kuvaus fi" :sv "Kuvaus sv"}}
                    :koulutustyyppi "tuva"}]
      (is (= {:kuvaus {:fi "Kuvaus fi" :sv "Kuvaus sv"}
              :opintojenLaajuusyksikko {:koodiUri "opintojenlaajuusyksikko_8#1" :nimi {:fi "viikkoa"}}}
             (:metadata (indexer.kouta.koulutus/enrich-tuva-metadata koulutus)))))))
