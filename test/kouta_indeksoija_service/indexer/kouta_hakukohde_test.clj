(ns kouta-indeksoija-service.indexer.kouta-hakukohde-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer :all]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.test-tools :refer [parse compare-json debug-pretty]]
            [kouta-indeksoija-service.indexer.kouta.hakukohde :as hakukohde]
            [kouta-indeksoija-service.indexer.kouta.haku :as haku]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus-search]
            [kouta-indeksoija-service.indexer.kouta.toteutus :as toteutus]
            [kouta-indeksoija-service.indexer.kouta.koulutus :as koulutus]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos-search]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [cheshire.core :refer [generate-string]])
  (:import (fi.oph.kouta.external KoutaFixtureTool$)))

(defonce KoutaFixtureTool KoutaFixtureTool$/MODULE$)

(use-fixtures :each fixture/indices-fixture)
(use-fixtures :each common-indexer-fixture)

(deftest index-hakukohde-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index hakukohde to hakukohde index and update related indexes"
     (check-all-nil)
     (i/index-hakukohteet [hakukohde-oid])
     (compare-json (no-timestamp (json "kouta-hakukohde-result"))
                   (no-timestamp (get-doc hakukohde/index-name hakukohde-oid)))
     (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
     (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (nil? (get-doc koulutus/index-name koulutus-oid)))
     (is (nil? (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1)))))))

(deftest index-lukio-hakukohde-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index hakukohde to hakukohde index and update related indexes"
     (check-all-nil)
     (fixture/update-koulutus-mock koulutus-oid :koulutustyyppi "lk" :metadata fixture/lk-koulutus-metadata)
     (fixture/update-toteutus-mock toteutus-oid :tila "tallennettu" :metadata (.lukioToteutusMedatada KoutaFixtureTool))
     (fixture/update-hakukohde-mock hakukohde-oid
                                    :metadata (generate-string {:hakukohteenLinja {:linja nil :alinHyvaksyttyKeskiarvo 6.5 :lisatietoa {:fi "fi-str", :sv "sv-str"}}
                                                                :kaytetaanHaunAlkamiskautta false
                                                                :koulutuksenAlkamiskausi {:alkamiskausityyppi "henkilokohtainen suunnitelma"}}))
     (i/index-hakukohteet [hakukohde-oid])
     (let [hakukohde (get-doc hakukohde/index-name hakukohde-oid)]
       (is (= (get-in hakukohde [:metadata :hakukohteenLinja]) {:alinHyvaksyttyKeskiarvo 6.5 :lisatietoa {:fi "fi-str", :sv "sv-str"}}))))))

(deftest index-hakukohde-without-alkamiskausi
  (fixture/with-mocked-indexing
   (testing "Koulutuksen alkamiskausi is not mandatory for haku and hakukohde. Previously yps calculation would fail if both were missing"
     (check-all-nil)
     (fixture/update-koulutus-mock koulutus-oid :koulutustyyppi "yo" :metadata fixture/yo-koulutus-metadata)
     (fixture/update-hakukohde-mock hakukohde-oid :tila "julkaistu" :kaytetaanHaunAlkamiskautta "true" :alkamiskausiKoodiUri "kausi_s#1" :alkamisvuosi nil)
     (fixture/update-haku-mock haku-oid :tila "julkaistu" :kohdejoukonTarkenneKoodiUri "haunkohdejoukontarkenne_3#1" :metadata (generate-string {:koulutuksenAlkamiskausi {:alkamiskausityyppi "henkilokohtainen suunnitelma"}}))
     (i/index-hakukohteet [hakukohde-oid])
     (let [hakukohde (get-doc hakukohde/index-name hakukohde-oid)
           yhden-paikan-saanto (:yhdenPaikanSaanto hakukohde)]
       (is (= hakukohde-oid (:oid hakukohde)))
       (is (true? (:voimassa yhden-paikan-saanto)))
       (is (= "Hakukohde on yhden paikan säännön piirissä" (:syy yhden-paikan-saanto)))))))

(deftest harkinnanvaraisuus-for-korkeakoulu
  (fixture/with-mocked-indexing
   (testing "Korkeakoulutus should never be harkinnanvarainen"
     (check-all-nil)
     (fixture/update-koulutus-mock koulutus-oid :koulutustyyppi "yo" :metadata fixture/yo-koulutus-metadata)
     (i/index-hakukohteet [hakukohde-oid])
     (let [hakukohde (get-doc hakukohde/index-name hakukohde-oid)]
       (is (false? (:onkoHarkinnanvarainenKoulutus hakukohde)))))))

(deftest index-hakukohde-hakulomakelinkki-test
  (fixture/with-mocked-indexing
   (testing "Indexer should create hakulomakeLinkki from haku oid"
     (check-all-nil)
     (fixture/update-hakukohde-mock hakukohde-oid :hakulomaketyyppi "ataru")
     (i/index-hakukohteet [hakukohde-oid])
     (compare-json (:hakulomakeLinkki (get-doc hakukohde/index-name hakukohde-oid))
                   {:fi (str "http://localhost/hakemus/haku/" haku-oid "?lang=fi")
                    :sv (str "http://localhost/hakemus/haku/" haku-oid "?lang=sv")
                    :en (str "http://localhost/hakemus/haku/" haku-oid "?lang=en")}))))

(deftest index-hakukohde-haun-hakulomakelinkki-test
  (fixture/with-mocked-indexing
   (testing "Indexer should create hakulomakeLinkki from haku oid"
     (check-all-nil)
     (fixture/update-haku-mock haku-oid :hakulomaketyyppi "ataru")
     (fixture/update-hakukohde-mock hakukohde-oid :hakulomaketyyppi "ataru" :kaytetaanHaunHakulomaketta "true")
     (i/index-hakukohteet [hakukohde-oid])
     (compare-json (:hakulomakeLinkki (get-doc haku/index-name haku-oid))
                   {:fi (str "http://localhost/hakemus/haku/" haku-oid "?lang=fi")
                    :sv (str "http://localhost/hakemus/haku/" haku-oid "?lang=sv")
                    :en (str "http://localhost/hakemus/haku/" haku-oid "?lang=en")}))))

(deftest index-hakukohde-yps-haku-luonnos-test
   (fixture/with-mocked-indexing
    (testing "Indexer should index hakukohde without yps if haku luonnos"
       (check-all-nil)
       (fixture/update-haku-mock haku-oid :tila "tallennettu")
       (i/index-hakukohteet [hakukohde-oid])
       (is (= "Haku on luonnos tilassa" (:syy (:yhdenPaikanSaanto (get-doc hakukohde/index-name hakukohde-oid)))))
       (is (= false (:voimassa (:yhdenPaikanSaanto (get-doc hakukohde/index-name hakukohde-oid))))))))

(deftest index-hakukohde-yps-hakukohde-luonnos-test
   (fixture/with-mocked-indexing
    (testing "Indexer should index hakukohde without yps if hakukohde luonnos"
       (check-all-nil)
       (fixture/update-hakukohde-mock hakukohde-oid :tila "tallennettu")
       (i/index-hakukohteet [hakukohde-oid])
       (is (= "Hakukohde on luonnos tilassa" (:syy (:yhdenPaikanSaanto (get-doc hakukohde/index-name hakukohde-oid)))))
       (is (= false (:voimassa (:yhdenPaikanSaanto (get-doc hakukohde/index-name hakukohde-oid))))))))

(deftest index-hakukohde-yps-not-korkeakoulutus-test
   (fixture/with-mocked-indexing
    (testing "Indexer should index hakukohde without yps if not korkeakoulutus"
       (check-all-nil)
       (fixture/update-haku-mock haku-oid :tila "julkaistu")
       (fixture/update-hakukohde-mock hakukohde-oid :tila "julkaistu")
       (fixture/update-koulutus-mock koulutus-oid :koulutustyyppi "amm")
       (i/index-hakukohteet [hakukohde-oid])
       (is (= "Ei korkeakoulutus koulutusta" (:syy (:yhdenPaikanSaanto (get-doc hakukohde/index-name hakukohde-oid)))))
       (is (= false (:voimassa (:yhdenPaikanSaanto (get-doc hakukohde/index-name hakukohde-oid))))))))

(deftest index-hakukohde-yps-not-jatkotutkintohaku-test
   (fixture/with-mocked-indexing
    (testing "Indexer should index hakukohde without yps if not jatkotutkintohaku"
       (check-all-nil)
       (fixture/update-koulutus-mock koulutus-oid :koulutustyyppi "amk" :johtaaTutkintoon "true" :metadata fixture/amk-koulutus-metadata)
       (fixture/update-hakukohde-mock hakukohde-oid :tila "julkaistu" :kaytetaanHaunAlkamiskautta "false" :alkamiskausiKoodiUri "kausi_s#1" :alkamisvuosi "2020")
       (fixture/update-haku-mock haku-oid :tila "julkaistu" :kohdejoukonTarkenneKoodiUri "haunkohdejoukontarkenne_1#1")
       (i/index-hakukohteet [hakukohde-oid])
       (is (= "Haun kohdejoukon tarkenne on haunkohdejoukontarkenne_1#1" (:syy (:yhdenPaikanSaanto (get-doc hakukohde/index-name hakukohde-oid)))))
       (is (= false (:voimassa (:yhdenPaikanSaanto (get-doc hakukohde/index-name hakukohde-oid))))))))

(deftest index-hakukohde-yps-jatkotutkintohaku-test
   (fixture/with-mocked-indexing
    (testing "Indexer should index hakukohde with yps if jatkotutkintohaku"
       (check-all-nil)
       (fixture/update-koulutus-mock koulutus-oid :koulutustyyppi "yo" :johtaaTutkintoon "true" :metadata fixture/yo-koulutus-metadata)
       (fixture/update-hakukohde-mock hakukohde-oid :tila "julkaistu" :kaytetaanHaunAlkamiskautta "false" :alkamiskausiKoodiUri "kausi_s#1" :alkamisvuosi "2020")
       (fixture/update-haku-mock haku-oid :tila "julkaistu" :kohdejoukonTarkenneKoodiUri "haunkohdejoukontarkenne_3#1")
       (i/index-hakukohteet [hakukohde-oid])
       (is (= "Hakukohde on yhden paikan säännön piirissä" (:syy (:yhdenPaikanSaanto (get-doc hakukohde/index-name hakukohde-oid)))))
       (is (= true (:voimassa (:yhdenPaikanSaanto (get-doc hakukohde/index-name hakukohde-oid))))))))

(deftest index-hakukohde-yps-no-tarkenne-test
   (fixture/with-mocked-indexing
    (testing "Indexer should index hakukohde with yps if no tarkenne"
       (check-all-nil)
       (fixture/update-koulutus-mock koulutus-oid :koulutustyyppi "amk" :johtaaTutkintoon "true" :metadata fixture/amk-koulutus-metadata)
       (fixture/update-hakukohde-mock hakukohde-oid :tila "julkaistu" :kaytetaanHaunAlkamiskautta "false" :alkamiskausiKoodiUri "kausi_s#1" :alkamisvuosi "2020")
       (fixture/update-haku-mock haku-oid :tila "julkaistu" :kohdejoukonTarkenneKoodiUri nil)
       (i/index-hakukohteet [hakukohde-oid])
       (is (= "Hakukohde on yhden paikan säännön piirissä" (:syy (:yhdenPaikanSaanto (get-doc hakukohde/index-name hakukohde-oid)))))
       (is (= true (:voimassa (:yhdenPaikanSaanto (get-doc hakukohde/index-name hakukohde-oid))))))))

(deftest index-hakukohde-yps-haun-alkamiskausi-test
   (fixture/with-mocked-indexing
    (testing "Indexer should index hakukohde using haun alkamiskausi with yps if no tarkenne"
       (check-all-nil)
       (fixture/update-koulutus-mock koulutus-oid :koulutustyyppi "amk" :johtaaTutkintoon "true" :metadata fixture/amk-koulutus-metadata)
       (fixture/update-hakukohde-mock hakukohde-oid :tila "julkaistu" :kaytetaanHaunAlkamiskautta "true")
       (fixture/update-haku-mock haku-oid :tila "julkaistu" :kohdejoukonTarkenneKoodiUri nil)
       (i/index-hakukohteet [hakukohde-oid])
       (is (= "Hakukohde on yhden paikan säännön piirissä" (:syy (:yhdenPaikanSaanto (get-doc hakukohde/index-name hakukohde-oid)))))
       (is (= true (:voimassa (:yhdenPaikanSaanto (get-doc hakukohde/index-name hakukohde-oid))))))))

