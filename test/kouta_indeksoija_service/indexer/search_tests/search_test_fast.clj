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

(def default-haut [{:tila "julkaistu"
                    :hakukohteet [{:tila "julkaistu"
                                   :jarjestyspaikkaOid "1.2.246.562.10.4444444444444"
                                   :toteutusOid "1.2.246.562.17.00000000000000006388"
                                   :nimi {:fi "Isännöinnin ammattitutkinto"}
                                   :hakuOid "1.2.246.562.29.00000000000000000045"
                                   :hakukohdeOid "1.2.246.562.20.00000000000000010664"
                                   :jarjestaaUrheilijanAmmKoulutusta true
                                   :organisaatioOid "1.2.246.562.10.96162204109"}]
                    :nimi {:fi "Jatkuva haku Gradia Jyväskylä"}
                    :organisaatioOid "1.2.246.562.10.48442622063"}])

(deftest jarjestaako-tarjoaja-urheilijan-amm-koulutusta
  (testing "returns true for one tarjoaja and corresponding oppilaitoksen osa that has true for jarjestaaUrheilijanAmmKoulutusta"
    (is (= true
           (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
             ["1.2.246.562.10.44802853312"]
             [{:tila "julkaistu"
               :oid "1.2.246.562.10.44802853312"
               :oppilaitosOid "1.2.246.562.10.96162204109"
               :metadata {:esittely {:fi "<p>Lorem ipsum</p>"}
                          :jarjestaaUrheilijanAmmKoulutusta true}
               :organisaatioOid "1.2.246.562.10.44802853312"}]
             default-haut
             )))

    (testing "returns false for one tarjoaja and corresponding oppilaitoksen osa that has false for jarjestaaUrheilijanAmmKoulutusta"
        (is (= false
           (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
             ["1.2.246.562.10.44802853312"]
             [{:tila "julkaistu"
               :oid "1.2.246.562.10.44802853312"
               :oppilaitosOid "1.2.246.562.10.96162204109"
               :metadata {:esittely {:fi "<p>Lorem ipsum</p>"}
                          :jarjestaaUrheilijanAmmKoulutusta false}
               :organisaatioOid "1.2.246.562.10.44802853312"}]
               default-haut
             ))))

    (testing "returns false for one tarjoaja when corresponding oppilaitoksen osa has false for jarjestaaUrheilijanAmmKoulutusta"
      (is (= false
             (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
               ["1.2.246.562.10.44802853312"]
               [{:tila "julkaistu"
                 :oid "1.2.246.562.10.4444444444444"
                 :oppilaitosOid "1.2.246.562.10.96162204109"
                 :metadata {:esittely {:fi "<p>Lorem ipsum</p>"}
                            :jarjestaaUrheilijanAmmKoulutusta true}
                 :organisaatioOid "1.2.246.562.10.44802853312"}
                {:tila "julkaistu"
                 :oid "1.2.246.562.10.44802853312"
                 :oppilaitosOid "1.2.246.562.10.96162204109"
                 :metadata {:esittely {:fi "<p>Lorem ipsum</p>"}
                            :jarjestaaUrheilijanAmmKoulutusta false}
                 :organisaatioOid "1.2.246.562.10.44802853312"}]
               default-haut
               ))))

    (testing "returns true when oppilaitoksen osat have true for one of the tarjoajat"
      (is (= true
             (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
               ["1.2.246.562.10.44802853312" "1.2.246.562.10.4444444444444" ]
               [{:tila "julkaistu"
                 :oid "1.2.246.562.10.4444444444444"
                 :oppilaitosOid "1.2.246.562.10.96162204109"
                 :metadata {:esittely {:fi "<p>Lorem ipsum</p>"}
                            :jarjestaaUrheilijanAmmKoulutusta true}
                 :organisaatioOid "1.2.246.562.10.44802853312"}
                {:tila "julkaistu"
                 :oid "1.2.246.562.10.44802853312"
                 :oppilaitosOid "1.2.246.562.10.96162204109"
                 :metadata {:esittely {:fi "<p>Lorem ipsum</p>"}
                            :jarjestaaUrheilijanAmmKoulutusta false}
                 :organisaatioOid "1.2.246.562.10.44802853312"}]
               default-haut
               ))))

    (testing "returns false when oppilaitoksen osat have false for all of the tarjoajat"
      (is (= false
             (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
               ["1.2.246.562.10.44802853312" "1.2.246.562.10.4444444444444"]
               [{:tila "julkaistu"
                 :oid "1.2.246.562.10.4444444444444"
                 :oppilaitosOid "1.2.246.562.10.96162204109"
                 :metadata {:esittely {:fi "<p>Lorem ipsum</p>"}
                            :jarjestaaUrheilijanAmmKoulutusta false}
                 :organisaatioOid "1.2.246.562.10.44802853312"}
                {:tila "julkaistu"
                 :oid "1.2.246.562.10.44802853312"
                 :oppilaitosOid "1.2.246.562.10.96162204109"
                 :metadata {:esittely {:fi "<p>Lorem ipsum</p>"}
                            :jarjestaaUrheilijanAmmKoulutusta false}
                 :organisaatioOid "1.2.246.562.10.44802853312"}]
               default-haut
               ))))

    (testing "returns false if tarjoaja is not in the oppilaitoksen osat list"
      (is (= false
             (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
               ["1.2.246.562.10.44111111111"]
               [{:tila "julkaistu"
                 :oid "1.2.246.562.10.4444444444444"
                 :oppilaitosOid "1.2.246.562.10.96162204109"
                 :metadata {:esittely {:fi "<p>Lorem ipsum</p>"}
                            :jarjestaaUrheilijanAmmKoulutusta true}
                 :organisaatioOid "1.2.246.562.10.44802853312"}
                {:tila "julkaistu"
                 :oid "1.2.246.562.10.44802853312"
                 :oppilaitosOid "1.2.246.562.10.96162204109"
                 :metadata {:esittely {:fi "<p>Lorem ipsum</p>"}
                            :jarjestaaUrheilijanAmmKoulutusta true}
                 :organisaatioOid "1.2.246.562.10.44802853312"}]
               default-haut
               ))))

    (testing "returns true if tarjoaja is oppilaitos and one of the hakukohteet has jarjestaja that has true for jarjestaaUrheilijanAmmKoulutusta"
      (let [tarjoajat ["1.2.246.562.10.96162204109"]
            oppilaitoksen-osat [{:tila "julkaistu"
                                 :oid "1.2.246.562.10.4444444444444"
                                 :oppilaitosOid "1.2.246.562.10.96162204109"
                                 :metadata {:esittely {:fi "<p>Lorem ipsum</p>"}
                                            :jarjestaaUrheilijanAmmKoulutusta true}
                                 :organisaatioOid "1.2.246.562.10.44802853312"}
                                {:tila "julkaistu"
                                 :oid "1.2.246.562.10.44802853312"
                                 :oppilaitosOid "1.2.246.562.10.96162204109"
                                 :metadata {:esittely {:fi "<p>Lorem ipsum</p>"}
                                            :jarjestaaUrheilijanAmmKoulutusta true}
                                 :organisaatioOid "1.2.246.562.10.44802853312"}]
            haut [{:tila "julkaistu"
                   :hakukohteet [{:tila "julkaistu"
                                  :jarjestyspaikkaOid "1.2.246.562.10.4444444444444"
                                  :toteutusOid "1.2.246.562.17.00000000000000006388"
                                  :nimi {:fi "Isännöinnin ammattitutkinto"}
                                  :hakuOid "1.2.246.562.29.00000000000000000045"
                                  :hakukohdeOid "1.2.246.562.20.00000000000000010664"
                                  :jarjestaaUrheilijanAmmKoulutusta true
                                  :organisaatioOid "1.2.246.562.10.96162204109"}]
                   :nimi {:fi "Jatkuva haku Gradia Jyväskylä"}
                   :organisaatioOid "1.2.246.562.10.48442622063"}]]
        (is (= true
             (search/jarjestaako-tarjoaja-urheilijan-amm-koulutusta
               tarjoajat oppilaitoksen-osat haut)))))
    ))

(deftest jarjestaako-hakukohteen-jarjestaja-urheilijan-amm-koulutusta
  (testing "returns true if the only hakukohde in the only haku has true for jarjestaaUrheilijanAmmKoulutusta"
    (is (= true
           (search/jarjestaako-hakukohteen-jarjestaja-urheilijan-amm-koulutusta
             ["1.2.246.562.10.96162204109"]
             [{:tila "julkaistu"
               :hakukohteet [{:tila "julkaistu"
                              :jarjestyspaikkaOid "1.2.246.562.10.4444444444444"
                              :toteutusOid "1.2.246.562.17.00000000000000006388"
                              :nimi {:fi "Isännöinnin ammattitutkinto"}
                              :hakuOid "1.2.246.562.29.00000000000000000045"
                              :hakukohdeOid "1.2.246.562.20.00000000000000010664"
                              :jarjestaaUrheilijanAmmKoulutusta true
                              :organisaatioOid "1.2.246.562.10.96162204109"}]
               :nimi {:fi "Jatkuva haku Gradia Jyväskylä"}
               :organisaatioOid "1.2.246.562.10.48442622063"}]))))

  (testing "returns false if haut is empty"
    (is (= false
           (search/jarjestaako-hakukohteen-jarjestaja-urheilijan-amm-koulutusta
             ["1.2.246.562.10.96162204109"]
             []
             ))))

  (testing "returns false if hakukohteet is empty"
    (is (= false
           (search/jarjestaako-hakukohteen-jarjestaja-urheilijan-amm-koulutusta
             ["1.2.246.562.10.96162204109"]
             [{:tila "julkaistu"
               :hakukohteet []
               :nimi {:fi "Jatkuva haku Gradia Jyväskylä"}
               :organisaatioOid "1.2.246.562.10.48442622063"}]
             ))))

  (testing "returns false if the only hakukohde in the only haku has different organisaatioOid than tarjoaja oid"
    (is (= false
           (search/jarjestaako-hakukohteen-jarjestaja-urheilijan-amm-koulutusta
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

  (testing "returns true when the second hakukohde in the only haku has correct organisaatioOid and jarjestaaUrheilijanAmmKoulutusta has true"
    (is (= true
           (search/jarjestaako-hakukohteen-jarjestaja-urheilijan-amm-koulutusta
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
                              :jarjestyspaikkaOid "1.2.246.562.10.4444444444444"
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
           (search/jarjestaako-hakukohteen-jarjestaja-urheilijan-amm-koulutusta
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
                              :jarjestyspaikkaOid "1.2.246.562.10.4444444444444"
                              :toteutusOid "1.2.246.562.17.00000000000000006388"
                              :nimi {:fi "Isännöinnin ammattitutkinto"}
                              :hakuOid "1.2.246.562.29.00000000000000000045"
                              :hakukohdeOid "1.2.246.562.20.00000000000000010664"
                              :jarjestaaUrheilijanAmmKoulutusta true
                              :organisaatioOid "1.2.246.562.10.96162204109"}]
               :nimi {:fi "Yhteishaku"}
               :organisaatioOid "1.2.246.562.10.48442622063"}]))))
  )

(use 'clojure.test)
(run-all-tests)
