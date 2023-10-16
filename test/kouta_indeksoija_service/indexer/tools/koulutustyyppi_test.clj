(ns kouta-indeksoija-service.indexer.tools.koulutustyyppi-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.indexer.tools.koulutustyyppi :refer [assoc-koulutustyyppi-path]]))

(let [opintojakso-koulutus {:koulutustyyppi "kk-opintojakso"}
      opintokokonaisuus-koulutus {:koulutustyyppi "kk-opintokokonaisuus"}
      erikoislaakari-koulutus {:koulutustyyppi "erikoislaakari"}]
  (deftest koulutustyyppi-path
    (testing "returns correct koulutustyyppiPath for avoin opintojakso toteutus"
      (let [toteutus (assoc-koulutustyyppi-path {} (assoc-in opintojakso-koulutus [:metadata :isAvoinKorkeakoulutus] true))]
        (is (= "kk-muu/kk-opintojakso-avoin" (:koulutustyyppiPath toteutus)))))
    (testing "returns correct koulutustyyppiPath for avoin opintokokonaisuus toteutus"
      (let [toteutus (assoc-koulutustyyppi-path {} (assoc-in opintokokonaisuus-koulutus [:metadata :isAvoinKorkeakoulutus] true))]
        (is (= "kk-muu/kk-opintokokonaisuus-avoin" (:koulutustyyppiPath toteutus)))))
    (testing "returns correct koulutustyyppiPath for non-avoin opintokojakso toteutus"
      (let [toteutus (assoc-koulutustyyppi-path {} opintojakso-koulutus)]
        (is (= "kk-muu/kk-opintojakso" (:koulutustyyppiPath toteutus)))))
    (testing "returns correct koulutustyyppiPath for non-avoin opintokokonaisuus toteutus"
      (let [toteutus (assoc-koulutustyyppi-path {} opintokokonaisuus-koulutus)]
        (is (= "kk-muu/kk-opintokokonaisuus" (:koulutustyyppiPath toteutus)))))
    (testing "returns correct koulutustyyppiPath for any kk-muu toteutus"
      (let [toteutus (assoc-koulutustyyppi-path {} erikoislaakari-koulutus)]
        (is (= "kk-muu/erikoislaakari" (:koulutustyyppiPath toteutus)))))))