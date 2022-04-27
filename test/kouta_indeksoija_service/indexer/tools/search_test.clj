(ns kouta-indeksoija-service.indexer.tools.search-test
  (:require [clojure.test :refer [deftest testing is]]
            [kouta-indeksoija-service.indexer.tools.search :as search]))

(deftest get-haun-julkaistut-hakukohteet-tests
  (testing "filters hakukohteet with tila luonnos from hakutiedot"
    (let [toteutus {:oid "1.2.246.562.13.00000000000000000009"}
          hakutiedot [{
                      :toteutusOid "1.2.246.562.13.00000000000000000009"
                      :haut [
                             {:hakuOid "1.2.246.562.29.00000000000000000009",
                      :tila "julkaistu",
                      :hakukohteet [{
                                     :hakukohdeOid "1.2.246.562.20.00000000000000000009",
                                     :tila "tallennettu"
                                     }]
                      }]}]
          julkaistut-hakutiedot (search/get-toteutuksen-julkaistut-hakutiedot hakutiedot toteutus)]
      (is (empty? (:haut julkaistut-hakutiedot))))))

(defonce oppilaitoksen-osat [{:tila "julkaistu"
                              :oid "1.2.246.562.10.44802853312"
                              :oppilaitosOid "1.2.246.562.10.96162204109"
                              :metadata {:kampus {}
                                         :esittely {:fi "<p>Lorem ipsum</p>"}
                                         :jarjestaaUrheilijanAmmKoulutusta true}
                              :organisaatioOid "1.2.246.562.10.44802853312"}
                             {:tila "julkaistu"
                              :oid "1.2.246.562.10.34178172895"
                              :oppilaitosOid "1.2.246.562.10.96162204109"
                              :metadata {:kampus {}
                                         :esittely {:fi "<p>Lorem ipsum</p>"}
                                         :jarjestaaUrheilijanAmmKoulutusta true}
                              :organisaatioOid "1.2.246.562.10.34178172895"}
                             {:tila "julkaistu"
                              :oid "1.2.246.562.10.87939127624"
                              :oppilaitosOid "1.2.246.562.10.96162204109"
                              :metadata {:kampus {}
                                         :esittely {:fi "<p>Lorem ipsum</p>"}
                                         :jarjestaaUrheilijanAmmKoulutusta true}
                              :organisaatioOid "1.2.246.562.10.87939127624"}
                             {:tila "julkaistu"
                              :oid "1.2.246.562.10.72130231946"
                              :oppilaitosOid "1.2.246.562.10.96162204109"
                              :metadata {:kampus {:fi "Gradia Jyväskylä, Lievestuore"}
                                         :esittely {:fi "<p>Lorem ipsum</p>"}
                                         :jarjestaaUrheilijanAmmKoulutusta false}
                              :organisaatioOid "1.2.246.562.10.72130231946"}
                             {:tila "julkaistu"
                              :oid "1.2.246.562.10.26426892241"
                              :oppilaitosOid "1.2.246.562.10.96162204109"
                              :metadata {:kampus {}
                                         :esittely {:fi "<p>Lorem ipsum</p>"}
                                         :jarjestaaUrheilijanAmmKoulutusta true}
                              :organisaatioOid "1.2.246.562.10.26426892241"}
                             {:tila "julkaistu"
                              :oid "1.2.246.562.10.92008999028"
                              :oppilaitosOid "1.2.246.562.10.96162204109"
                              :metadata {:kampus {}
                                         :esittely {:fi "<p>Lorem ipsum</p>"}
                                         :jarjestaaUrheilijanAmmKoulutusta true}
                              :organisaatioOid "1.2.246.562.10.92008999028"}
                             {:tila "julkaistu"
                              :oid "1.2.246.562.10.25303897067"
                              :oppilaitosOid "1.2.246.562.10.96162204109"
                              :metadata {:kampus {}
                                         :esittely {:fi "<p>Lorem ipsum</p>"}
                                         :jarjestaaUrheilijanAmmKoulutusta true}
                              :organisaatioOid "1.2.246.562.10.25303897067"}])
