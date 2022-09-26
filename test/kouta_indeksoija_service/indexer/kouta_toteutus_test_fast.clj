(ns kouta-indeksoija-service.indexer.kouta.toteutus-test-fast
  (:require [clojure.test :refer [deftest testing is]]
            [kouta-indeksoija-service.indexer.kouta.toteutus :as toteutus]))

(deftest assoc-opintojaksot
  (testing "returns toteutus with one liitetty opintojakso attached"
    (let [toteutus {:tila "julkaistu"
                    :koulutusOid "1.2.246.562.13.00000000000000003145"
                    :nimi {:fi "testiopintokokonaisuustoteutus"}
                    :oid "1.2.246.562.17.00000000000000009816"
                    :metadata {:liitetytOpintojaksot ["1.2.246.562.17.00000000000000009999"]
                               :kuvaus {:fi "<p>Opintokokonaisuuden kuvaus</p>"}
                               :tyyppi "kk-opintokokonaisuus"}
                    :organisaatioOid "1.2.246.562.10.75204242195"}
          liitetyt-opintojaksot [{:tila "julkaistu"
                                  :koulutusOid "1.2.246.562.13.00000000000000003333"
                                  :nimi {:fi "testiopintojaksototeutus"}
                                  :oid "1.2.246.562.17.00000000000000009999"
                                  :metadata {:liitetytOpintojaksot nil
                                             :kuvaus {:fi "<p>Opintojakson kuvaus</p>"}
                                             :tyyppi "kk-opintojakso"}
                                  :organisaatioOid "1.2.246.562.10.75204242195"}]
          enriched-toteutus {:tila "julkaistu"
                             :koulutusOid "1.2.246.562.13.00000000000000003145"
                             :nimi {:fi "testiopintokokonaisuustoteutus"}
                             :oid "1.2.246.562.17.00000000000000009816"
                             :metadata {:liitetytOpintojaksot ["1.2.246.562.17.00000000000000009999"]
                                        :kuvaus {:fi "<p>Opintokokonaisuuden kuvaus</p>"}
                                        :tyyppi "kk-opintokokonaisuus"}
                             :organisaatioOid "1.2.246.562.10.75204242195"
                             :liitetytOpintojaksot [{:nimi {:fi "testiopintojaksototeutus"}
                                                     :oid "1.2.246.562.17.00000000000000009999"
                                                     :kuvaus {:fi "<p>Opintojakson kuvaus</p>"}}]}]
      (is (= enriched-toteutus (toteutus/assoc-opintojaksot toteutus liitetyt-opintojaksot)))))

  (testing "returns toteutus with one liitetty opintojakso attached"
    (let [toteutus {:tila "julkaistu"
                    :koulutusOid "1.2.246.562.13.00000000000000003145"
                    :nimi {:fi "testiopintokokonaisuustoteutus"}
                    :oid "1.2.246.562.17.00000000000000009816"
                    :metadata {:liitetytOpintojaksot ["1.2.246.562.17.00000000000000009999"
                                                      "1.2.246.562.17.00000000000000008888"]
                               :kuvaus {:fi "<p>Opintokokonaisuuden kuvaus</p>"}
                               :tyyppi "kk-opintokokonaisuus"}
                    :organisaatioOid "1.2.246.562.10.75204242195"}
          liitetyt-opintojaksot [{:tila "julkaistu"
                                  :koulutusOid "1.2.246.562.13.00000000000000003333"
                                  :nimi {:fi "testiopintojaksototeutus"}
                                  :oid "1.2.246.562.17.00000000000000009999"
                                  :metadata {:liitetytOpintojaksot nil
                                             :kuvaus {:fi "<p>Opintojakson kuvaus</p>"}
                                             :tyyppi "kk-opintojakso"}
                                  :organisaatioOid "1.2.246.562.10.75204242195"}
                                 {:tila "julkaistu"
                                  :koulutusOid "1.2.246.562.13.00000000000000003333"
                                  :nimi {:fi "testiopintojaksototeutus nro 2"}
                                  :oid "1.2.246.562.17.00000000000000008888"
                                  :metadata {:liitetytOpintojaksot nil
                                             :kuvaus {:fi "<p>Opintojakson nro 2 kuvaus</p>"}
                                             :tyyppi "kk-opintojakso"}
                                  :organisaatioOid "1.2.246.562.10.75204242195"}]
          enriched-toteutus {:tila "julkaistu"
                             :koulutusOid "1.2.246.562.13.00000000000000003145"
                             :nimi {:fi "testiopintokokonaisuustoteutus"}
                             :oid "1.2.246.562.17.00000000000000009816"
                             :metadata {:liitetytOpintojaksot ["1.2.246.562.17.00000000000000009999" "1.2.246.562.17.00000000000000008888"]
                                        :kuvaus {:fi "<p>Opintokokonaisuuden kuvaus</p>"}
                                        :tyyppi "kk-opintokokonaisuus"}
                             :organisaatioOid "1.2.246.562.10.75204242195"
                             :liitetytOpintojaksot [{:nimi {:fi "testiopintojaksototeutus"}
                                                     :oid "1.2.246.562.17.00000000000000009999"
                                                     :kuvaus {:fi "<p>Opintojakson kuvaus</p>"}}
                                                    {:nimi {:fi "testiopintojaksototeutus nro 2"}
                                                     :oid "1.2.246.562.17.00000000000000008888"
                                                     :kuvaus {:fi "<p>Opintojakson nro 2 kuvaus</p>"}}]}]
      (is (= enriched-toteutus (toteutus/assoc-opintojaksot toteutus liitetyt-opintojaksot)))))
  )

(use 'clojure.test)
(run-tests)
