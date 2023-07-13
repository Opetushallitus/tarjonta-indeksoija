(ns kouta-indeksoija-service.indexer.search-tests.kouta-koulutus-search-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.rest.koodisto :refer [list-alakoodi-nimet-with-cache]]
            [kouta-indeksoija-service.indexer.tools.search :refer [deduce-koulutustyypit]]))

(defn- mock-koodiuri-fn [koodiuri]
  (fn [koodi-uri alakoodi-uri] (vector {:koodiUri koodiuri :nimi {:fi "joku nimi" :sv "joku nimi sv"}})))

(defn- mock-koodisto-koulutustyyppi
  [koodi-uri alakoodi-uri]
  (vector
   { :koodiUri "koulutustyyppi_26" :nimi {:fi "joku nimi" :sv "joku nimi sv"}}
   { :koodiUri "koulutustyyppi_4" :nimi {:fi "joku nimi2" :sv "joku nimi sv2"}}))

(deftest filter-erityisopetus-koulutustyyppi
  (testing "If not ammatillinen perustutkinto erityisopetuksena, filter out erityisopetus koulutustyyppi from koodisto response"
    (with-redefs [list-alakoodi-nimet-with-cache mock-koodisto-koulutustyyppi]
      (let [koulutus {:koulutustyyppi "amm"}
            toteutus-metadata {:ammatillinenPerustutkintoErityisopetuksena false}
            result (deduce-koulutustyypit koulutus toteutus-metadata)]
        (is (= ["amm" "koulutustyyppi_26"] result))))))

(deftest add-amm-erityisopetus-koulutustyyppi-koodi
  (testing "If ammatillinen perustutkinto erityisopetuksena, add only erityisopetus koulutustyyppi koodi"
    (with-redefs [list-alakoodi-nimet-with-cache mock-koodisto-koulutustyyppi]
      (let [koulutus {:koulutustyyppi "amm"}
            toteutus-metadata {:ammatillinenPerustutkintoErityisopetuksena true}
            result (deduce-koulutustyypit koulutus toteutus-metadata)]
        (is (= ["amm" "koulutustyyppi_4"] result))))))

(deftest add-muu-amm-tutkinto-koulutustyyppi
  (testing "If no known amm koulutuskoodi, add muu-amm-tutkinto"
    (with-redefs [list-alakoodi-nimet-with-cache (mock-koodiuri-fn "koulutustyyppi_123")]
      (let [koulutus {:koulutustyyppi "amm", :koulutuksetKoodiUri []}
            toteutus-metadata nil
            result (deduce-koulutustyypit koulutus toteutus-metadata)]
        (is (= ["amm" "muu-amm-tutkinto"] result))))))

(deftest add-tuva-normal-koulutustyyppi
  (testing "If tuva without erityisopetus, add 'tuva-normal' koulutustyyppi"
      (let [koulutus {:koulutustyyppi "tuva"}
            toteutus-metadata {:jarjestetaanErityisopetuksena false}
            result (deduce-koulutustyypit koulutus toteutus-metadata)]
        (is (= ["tuva" "tuva-normal"] result)))))

(deftest add-tuva-erityisopetus-koulutustyyppi
  (testing "If tuva erityisopetuksena, add 'tuva-erityisopetus' koulutustyyppi"
      (let [koulutus {:koulutustyyppi "tuva"}
            toteutus-metadata {:jarjestetaanErityisopetuksena true}
            result (deduce-koulutustyypit koulutus toteutus-metadata)]
        (is (= ["tuva" "tuva-erityisopetus"] result)))))

(deftest add-vapaa-sivistystyo-koulutustyyppi-when-opistovuosi
  (testing "If vapaa-sivistystyo-opistovuosi, add 'vapaa-sivistystyo' koulutustyyppi"
      (let [koulutus {:koulutustyyppi "vapaa-sivistystyo-opistovuosi"}
            result (deduce-koulutustyypit koulutus)]
        (is (= ["vapaa-sivistystyo" "vapaa-sivistystyo-opistovuosi"] result)))))

(deftest add-vapaa-sivistystyo-koulutustyyppi-when-muu
  (testing "If vapaa-sivistystyo-muu, add 'vapaa-sivistystyo' koulutustyyppi"
      (let [koulutus {:koulutustyyppi "vapaa-sivistystyo-muu"}
            result (deduce-koulutustyypit koulutus)]
        (is (= ["vapaa-sivistystyo" "vapaa-sivistystyo-muu"] result)))))

(deftest add-kk-muu-when-yo-ope
  (testing "If ope-pedag-opinnot, add 'kk-muu' koulutustyyppi"
    (let [koulutus {:koulutustyyppi "ope-pedag-opinnot"}
          result (deduce-koulutustyypit koulutus)]
      (is (= ["kk-muu" "ope-pedag-opinnot"] result)))))

(deftest add-kk-muu-when-erikoislaakari
  (testing "If erikoislaakari, add 'kk-muu' koulutustyyppi"
    (let [koulutus {:koulutustyyppi "erikoislaakari"}
          result (deduce-koulutustyypit koulutus)]
      (is (= ["kk-muu" "erikoislaakari"] result)))))

(deftest add-kk-muu-when-erikoistumiskoulutus
  (testing "If erikoistumiskoulutus, add 'kk-muu' koulutustyyppi"
    (let [koulutus {:koulutustyyppi "erikoistumiskoulutus"}
          result (deduce-koulutustyypit koulutus)]
      (is (= ["kk-muu" "erikoistumiskoulutus"] result)))))

(deftest add-kk-muu-when-kk-opintojakso
  (testing "If kk-opintojakso, add 'kk-muu' koulutustyyppi"
    (let [koulutus {:koulutustyyppi "kk-opintojakso"}
          result (deduce-koulutustyypit koulutus)]
      (is (= ["kk-muu" "kk-opintojakso" "kk-opintojakso-normal"] result)))))

(deftest add-kk-muu-when-kk-opintokokonaisuus
  (testing "If kk-opintokokonaisuus, add 'kk-muu' koulutustyyppi"
    (let [koulutus {:koulutustyyppi "kk-opintokokonaisuus"}
          result (deduce-koulutustyypit koulutus)]
      (is (= ["kk-muu" "kk-opintokokonaisuus" "kk-opintokokonaisuus-normal"] result)))))
