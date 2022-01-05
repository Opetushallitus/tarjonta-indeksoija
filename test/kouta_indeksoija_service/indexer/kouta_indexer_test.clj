(ns kouta-indeksoija-service.indexer.kouta-indexer-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer :all]
            [kouta-indeksoija-service.indexer.indexer :as i]
            [kouta-indeksoija-service.indexer.kouta.koulutus :as koulutus]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus-search]
            [kouta-indeksoija-service.indexer.kouta.toteutus :as toteutus]
            [kouta-indeksoija-service.indexer.kouta.haku :as haku]
            [kouta-indeksoija-service.indexer.kouta.valintaperuste :as valintaperuste]
            [kouta-indeksoija-service.indexer.kouta.hakukohde :as hakukohde]
            [kouta-indeksoija-service.indexer.kouta.sorakuvaus :as sorakuvaus]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos :as oppilaitos]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos-search]
            [kouta-indeksoija-service.indexer.eperuste.eperuste :as eperuste]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.test-tools :refer [parse compare-json debug-pretty]]
            [kouta-indeksoija-service.indexer.cache.hierarkia]
            [kouta-indeksoija-service.rest.organisaatio]
            [kouta-indeksoija-service.rest.kouta]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [mocks.externals-mock :as mock]))

(use-fixtures :each fixture/indices-fixture)
(use-fixtures :each common-indexer-fixture)

(defn mock-organisaatio-hierarkia
  [oid & {:as params}]
  (parse (str "test/resources/organisaatiot/1.2.246.562.10.10101010101-hierarkia.json")))

(defn mock-organisaatio-hierarkia-v4
  [oid]
  (kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-organisaatio-hierarkia-v4 oid))

(deftest index-haku-test-1
  (fixture/with-mocked-indexing
   (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 mock-organisaatio-hierarkia-v4]
     (testing "Indexer should index haku to haku index and update related indexes"
     (check-all-nil)
     (i/index-haut [haku-oid] (. System (currentTimeMillis)))
     (compare-json (no-timestamp (json "kouta-haku-result"))
                   (no-timestamp (get-doc haku/index-name haku-oid)))
     (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (nil? (get-doc koulutus/index-name koulutus-oid)))
     (is (nil? (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))))))

(deftest index-haku-hakulomakelinkki-test
  (fixture/with-mocked-indexing
   (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 mock-organisaatio-hierarkia-v4]
     (testing "Indexer should create hakulomakeLinkki from haku oid"
     (check-all-nil)
     (fixture/update-haku-mock haku-oid :hakulomaketyyppi "ataru")
     (i/index-haut [haku-oid] (. System (currentTimeMillis)))
     (compare-json (:hakulomakeLinkki (get-doc haku/index-name haku-oid))
                   {:fi (str "http://localhost/hakemus/haku/" haku-oid "?lang=fi")
                    :sv (str "http://localhost/hakemus/haku/" haku-oid "?lang=sv")
                    :en (str "http://localhost/hakemus/haku/" haku-oid "?lang=en")})))))

(deftest index-valintaperuste-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index valintaperuste to valintaperuste index and related hakukohde to hakukohde index"
     (check-all-nil)
     (i/index-valintaperusteet [valintaperuste-id] (. System (currentTimeMillis)))
     (compare-json (no-timestamp (json "kouta-valintaperuste-result"))
                   (no-timestamp (get-doc valintaperuste/index-name valintaperuste-id)))
     (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
     (is (= hakukohde-oid2 (:oid (get-doc hakukohde/index-name hakukohde-oid2))))
     (is (= ei-julkaistun-haun-julkaistu-hakukohde-oid (:oid (get-doc hakukohde/index-name ei-julkaistun-haun-julkaistu-hakukohde-oid))))
     (fixture/update-hakukohde-mock hakukohde-oid :tila "poistettu")
     (fixture/update-hakukohde-mock hakukohde-oid2 :tila "poistettu")
     (fixture/update-hakukohde-mock ei-julkaistun-haun-julkaistu-hakukohde-oid :tila "poistettu")
     (fixture/update-valintaperuste-mock valintaperuste-id :tila "poistettu")
     (i/index-valintaperusteet [valintaperuste-id] (. System (currentTimeMillis)))
     (is (nil? (get-doc valintaperuste/index-name valintaperuste-id)))
     (is (nil? (get-doc hakukohde/index-name hakukohde-oid)))
     (is (nil? (get-doc hakukohde/index-name hakukohde-oid2)))
     (is (nil? (get-doc hakukohde/index-name ei-julkaistun-haun-julkaistu-hakukohde-oid)))
     )))

(deftest index-sorakuvaus-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index sorakuvaus to sorakuvaus index and koulutus related to sorakuvaus to koulutus index"
     (check-all-nil)
     (i/index-sorakuvaukset [sorakuvaus-id] (. System (currentTimeMillis)))
     (is (= koulutus-oid (:oid (get-doc koulutus/index-name koulutus-oid))))
     (is (= koulutus-oid2 (:oid (get-doc koulutus/index-name koulutus-oid2))))
     (compare-json (no-timestamp (json "kouta-sorakuvaus-result"))
                   (no-timestamp (get-doc sorakuvaus/index-name sorakuvaus-id)))
     (fixture/update-sorakuvaus-mock sorakuvaus-id :tila "poistettu")
     (fixture/update-koulutus-mock koulutus-oid :tila "poistettu")
     (fixture/update-koulutus-mock koulutus-oid2 :tila "poistettu")
     (i/index-sorakuvaukset [sorakuvaus-id] (. System (currentTimeMillis)))
     (is (nil? (get-doc sorakuvaus/index-name sorakuvaus-id)))
     (is (nil? (get-doc koulutus/index-name koulutus-oid)))
     (is (nil? (get-doc koulutus/index-name koulutus-oid2))))))

(defn- add-toteutus-for-oppilaitos []
  (fixture/add-koulutus-mock "1.2.246.562.13.00000000000000000002"
                             :tila "julkaistu"
                             :nimi "Autoalan perustutkinto 1"
                             :muokkaaja "1.2.246.562.24.62301161440"
                             :julkinen "true"
                             :modified "2019-01-31T09:11:23"
                             :tarjoajat oppilaitos-oid))

(deftest index-oppilaitos-test
  (fixture/with-mocked-indexing
   (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-for-oid-from-cache mock-organisaatio-hierarkia
                 kouta-indeksoija-service.rest.organisaatio/get-by-oid-cached kouta-indeksoija-service.fixture.external-services/mock-organisaatio]
     (testing "Indexer should index oppilaitos and it's osat to oppilaitos index"
       (check-all-nil)
       (add-toteutus-for-oppilaitos)
       (i/index-oppilaitokset [oppilaitos-oid] (. System (currentTimeMillis)))
       (compare-json (no-timestamp (json "kouta-oppilaitos-result"))
                     (no-timestamp (get-doc oppilaitos/index-name oppilaitos-oid)))))))

(deftest index-oppilaitos-test-2
 (fixture/with-mocked-indexing
  (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-for-oid-from-cache mock-organisaatio-hierarkia
                kouta-indeksoija-service.rest.organisaatio/get-by-oid-cached kouta-indeksoija-service.fixture.external-services/mock-organisaatio]
    (testing "Indexer should index oppilaitos and it's osat to oppilaitos index when given oppilaitoksen osa oid"
      (check-all-nil)
      (add-toteutus-for-oppilaitos)
      (i/index-oppilaitos oppilaitoksen-osa-oid)
      (compare-json (no-timestamp (json "kouta-oppilaitos-result"))
                    (no-timestamp (get-doc oppilaitos/index-name oppilaitos-oid)))))))

(deftest index-oppilaitos-test-3
  (fixture/with-mocked-indexing
    (with-redefs [kouta-indeksoija-service.indexer.cache.hierarkia/get-hierarkia (fn [oid]
                  (update-in (parse (str "test/resources/organisaatiot/1.2.246.562.10.10101010101-hierarkia.json"))
                             [:organisaatiot 0 :children 0 :organisaatiotyypit]
                             (constantly ["organisaatiotyyppi_02", "organisaatiotyyppi_06"])))
                  kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 (update-in (parse (str "test/resources/organisaatiot/1.2.246.562.10.10101010101-hierarkia-v4.json"))
                                                                                         [:organisaatiot 0 :children 0 :organisaatiotyypit]
                                                                                         (constantly ["organisaatiotyyppi_02", "organisaatiotyyppi_06"]))]
     (testing "Indexer should not index oppilaitos when invalid organisaatiotyyppi"
       (check-all-nil)
       (i/index-oppilaitos oppilaitos-oid)
       (check-all-nil)))))

(deftest index-oppilaitos-test-4
  (fixture/with-mocked-indexing
   (testing "Indexer should index also koulutus when indexing oppilaitos"
     (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-for-oid-without-parents mocks/mock-organisaatio]
       (check-all-nil)
       (i/index-oppilaitokset [mocks/Oppilaitos1] (. System (currentTimeMillis)))
       (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
       (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))))))

(deftest index-oppilaitoksen-osa
  (fixture/with-mocked-indexing
    (testing "Indexer should index hakukohde when indexing oppilaitoksen osa that is set as järjestyspaikka on that hakukohde"
      (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-for-oid-without-parents mocks/mock-organisaatio]
        (check-all-nil)
        (i/index-oppilaitokset [default-jarjestyspaikka-oid] (. System (currentTimeMillis)))
        (is (= "1.2.246.562.20.00000000000000000002" (:oid (get-doc hakukohde/index-name "1.2.246.562.20.00000000000000000002"))))))))

(deftest index-organisaatio-no-oppilaitokset-test
  (fixture/with-mocked-indexing
   (with-redefs [kouta-indeksoija-service.rest.kouta/get-koulutukset-by-tarjoaja (fn [oid] (throw (Exception. (str "I was called with [" oid "]"))))
                 kouta-indeksoija-service.indexer.cache.hierarkia/get-hierarkia (fn [oid] {:numHits 1,
                                                                                           :organisaatiot [{:oid oid,
                                                                                                            :alkuPvm 313106400000,
                                                                                                            :parentOid "1.2.246.562.10.00000000001",
                                                                                                            :parentOidPath (str oid  "/1.2.246.562.10.10101010100"),
                                                                                                            :nimi {:fi "nimi fi"}
                                                                                                            :kieletUris ["oppilaitoksenopetuskieli_1#1"],
                                                                                                            :kotipaikkaUri "kunta_091",
                                                                                                            :organisaatiotyypit [ "organisaatiotyyppi_01" ],
                                                                                                            :status "AKTIIVINEN"
                                                                                                            :children []}]})]
     (testing "Indexer should not index organisaatio without oppilaitos"
       (check-all-nil)
       (i/index-oppilaitos "1.2.246.562.10.101010101012222222")
       (check-all-nil)))))

(deftest index-passiivinen-oppilaitos-test
  (fixture/with-mocked-indexing
   (testing "Indexer should delete passivoitu oppilaitos from indexes"
     (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-for-oid-from-cache mock-organisaatio-hierarkia
                   kouta-indeksoija-service.rest.organisaatio/get-by-oid-cached
                     kouta-indeksoija-service.fixture.external-services/mock-organisaatio]
       (check-all-nil)
       (i/index-oppilaitokset [oppilaitos-oid] (. System (currentTimeMillis))))
     (with-redefs [kouta-indeksoija-service.indexer.cache.hierarkia/get-hierarkia (fn [oid]
                   (update-in (parse (str "test/resources/organisaatiot/1.2.246.562.10.10101010101-hierarkia.json"))
                              [:organisaatiot 0 :children 0 :status]
                              (constantly "PASSIIVINEN")))
                   kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 (fn [oid]
                   (update-in (parse (str "test/resources/organisaatiot/1.2.246.562.10.10101010101-hierarkia-v4.json"))
                              [:organisaatiot 0 :status]
                              (constantly "PASSIIVINEN")))]
       (is (= oppilaitos-oid (:oid (get-doc oppilaitos/index-name oppilaitos-oid))))
       (is (= oppilaitos-oid (:oid (get-doc oppilaitos-search/index-name oppilaitos-oid))))
       (i/index-oppilaitokset [oppilaitos-oid] (. System (currentTimeMillis)))
       (is (= nil (:oid (get-doc oppilaitos/index-name oppilaitos-oid))))
       (is (= nil (:oid (get-doc oppilaitos-search/index-name oppilaitos-oid))))))))

(deftest index-all-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index all"
     (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 mock-organisaatio-hierarkia-v4]
       (let [eperuste-id 12321]
         (fixture/update-koulutus-mock koulutus-oid :ePerusteId eperuste-id)
         (check-all-nil)
         (is (nil? (eperuste/get-from-index eperuste-id)))
         (i/index-all-kouta)
         (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
         (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
         (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
         (is (= koulutus-oid (:oid (get-doc koulutus/index-name koulutus-oid))))
         (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
         (is (= oppilaitos-oid (:oid (get-doc oppilaitos-search/index-name oppilaitos-oid))))
         (is (= valintaperuste-id (:id (get-doc valintaperuste/index-name valintaperuste-id))))
         (is (= eperuste-id (:id (eperuste/get-from-index eperuste-id))))
         (fixture/update-koulutus-mock koulutus-oid :ePerusteId nil))))))

(deftest index-changes-oids-test
  (fixture/with-mocked-indexing
   (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 mock-organisaatio-hierarkia-v4]
     (testing "Indexer should index changed oids"
     (check-all-nil)
     (i/index-oids {:hakukohteet [hakukohde-oid]} (. System (currentTimeMillis)))
     (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
     (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
     (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
     (is (nil? (:oid (get-doc koulutus/index-name koulutus-oid))))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (nil? (:oid (get-doc oppilaitos-search/index-name oppilaitos-oid))))
     (is (nil? (:id (get-doc valintaperuste/index-name valintaperuste-id))))))))

(deftest index-changes-oids-test-2
  (fixture/with-mocked-indexing
   (testing "Indexer should index changed oids 2"
     (check-all-nil)
     (i/index-oids {:sorakuvaukset [sorakuvaus-id]} (. System (currentTimeMillis)))
     (is (nil? (:oid (get-doc haku/index-name haku-oid))))
     (is (nil? (:oid (get-doc hakukohde/index-name hakukohde-oid))))
     (is (nil? (:oid (get-doc toteutus/index-name toteutus-oid))))
     (is (= koulutus-oid (:oid (get-doc koulutus/index-name koulutus-oid))))
     (is (nil? (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (nil? (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
     (is (nil? (:id (get-doc valintaperuste/index-name valintaperuste-id)))))))

(deftest index-all-koulutukset-test
  (fixture/with-mocked-indexing
   (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 mock-organisaatio-hierarkia-v4]
     (testing "Indexer should index all koulutukset"
       (check-all-nil)
       (i/index-all-koulutukset)
       (is (nil? (get-doc haku/index-name haku-oid)))
       (is (nil? (get-doc hakukohde/index-name hakukohde-oid)))
       (is (nil? (get-doc toteutus/index-name toteutus-oid)))
       (is (= koulutus-oid (:oid (get-doc koulutus/index-name koulutus-oid))))
       (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
       (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
       (is (nil? (get-doc valintaperuste/index-name valintaperuste-id)))))))

(deftest index-all-toteutukset-test
  (fixture/with-mocked-indexing
   (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 mock-organisaatio-hierarkia-v4]
     (testing "Indexer should index all toteutukset"
       (check-all-nil)
       (i/index-all-toteutukset)
       (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
       (is (nil? (get-doc hakukohde/index-name hakukohde-oid)))
       (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
       (is (= koulutus-oid (:oid (get-doc koulutus/index-name koulutus-oid))))
       (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
       (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
       (is (nil? (get-doc valintaperuste/index-name valintaperuste-id)))))))

(defonce koulutus-oid-extra "1.2.246.562.13.00000000000000000099")
(defonce toteutus-oid-extra "1.2.246.562.17.00000000000000000099")
(defonce oppilaitos-oid-extra "1.2.246.562.10.77777777799")

(deftest index-all-hakukohteet-test
  (fixture/add-hakukohde-mock hakukohde-oid toteutus-oid haku-oid :valintaperuste valintaperuste-id :jarjestyspaikkaOid mocks/Oppilaitos1)

  (fixture/with-mocked-indexing
   (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 mock-organisaatio-hierarkia-v4]
     (testing "Indexer should index all hakukohteet"
       (check-all-nil)
       (i/index-all-hakukohteet)
       (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
       (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
       (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
       (is (nil? (get-doc koulutus/index-name koulutus-oid)))
       (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
       (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
       (is (nil? (get-doc valintaperuste/index-name valintaperuste-id)))))))

(deftest index-hakukohde-test
  (fixture/add-koulutus-mock koulutus-oid-extra :tarjoajat oppilaitos-oid-extra)
  (fixture/add-toteutus-mock toteutus-oid-extra koulutus-oid-extra :tarjoajat oppilaitos-oid-extra)
  (fixture/add-hakukohde-mock hakukohde-oid toteutus-oid haku-oid :valintaperuste valintaperuste-id :jarjestyspaikkaOid mocks/Oppilaitos1)
  (fixture/add-hakukohde-mock hakukohde-oid2 toteutus-oid-extra haku-oid :valintaperuste valintaperuste-id :jarjestyspaikkaOid oppilaitos-oid-extra)

  (fixture/with-mocked-indexing
   (testing "Indexer should index only hakukohde related koulutus and oppilaitos"
     (check-all-nil)
     (i/index-hakukohde hakukohde-oid)
     (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
     (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
     (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
     (is (= nil (:oid (get-doc toteutus/index-name toteutus-oid-extra))))
     (is (nil? (get-doc koulutus/index-name koulutus-oid)))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (= nil (:oid (get-doc koulutus-search/index-name koulutus-oid-extra))))
     (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
     (is (= nil (:oid (get-doc oppilaitos-search/index-name oppilaitos-oid-extra))))
     (is (nil? (get-doc valintaperuste/index-name valintaperuste-id))))))

(deftest index-all-haut-test
  (fixture/add-hakukohde-mock hakukohde-oid toteutus-oid haku-oid :valintaperuste valintaperuste-id :jarjestyspaikkaOid mocks/Oppilaitos1)

  (fixture/with-mocked-indexing
   (testing "Indexer should index all haut"
     (check-all-nil)
     (i/index-all-haut)
     (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
     (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
     (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
     (is (nil? (get-doc koulutus/index-name koulutus-oid)))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
     (is (nil? (get-doc valintaperuste/index-name valintaperuste-id))))))

(deftest index-haku-test-2
  (fixture/add-koulutus-mock koulutus-oid-extra :sorakuvausId sorakuvaus-id :tarjoajat oppilaitos-oid-extra)
  (fixture/add-toteutus-mock toteutus-oid-extra koulutus-oid-extra :tarjoajat oppilaitos-oid-extra)
  (fixture/add-hakukohde-mock hakukohde-oid toteutus-oid haku-oid :valintaperuste valintaperuste-id :jarjestyspaikkaOid mocks/Oppilaitos1)
  (fixture/add-hakukohde-mock hakukohde-oid2 toteutus-oid-extra haku-oid :valintaperuste valintaperuste-id :jarjestyspaikkaOid oppilaitos-oid-extra)

  (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 mock-organisaatio-hierarkia-v4]
    (fixture/with-mocked-indexing
    (testing "Indexer should index all haku related koulutukset and oppilaitokset"
      (check-all-nil)
       (i/index-haku haku-oid)
       (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
       (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
       (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
       (is (= toteutus-oid-extra (:oid (get-doc toteutus/index-name toteutus-oid-extra))))
       (is (nil? (get-doc koulutus/index-name koulutus-oid)))
       (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
       (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
       (is (= oppilaitos-oid-extra (:oid (get-doc oppilaitos-search/index-name oppilaitos-oid-extra))))
       (is (nil? (get-doc valintaperuste/index-name valintaperuste-id)))))))

(deftest index-all-valintaperusteet-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index all valintaperusteet"
     (check-all-nil)
     (i/index-all-valintaperusteet)
     (is (nil? (get-doc haku/index-name haku-oid)))
     (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
     (is (nil? (get-doc toteutus/index-name toteutus-oid)))
     (is (nil? (get-doc koulutus/index-name koulutus-oid)))
     (is (nil? (get-doc koulutus-search/index-name koulutus-oid)))
     (is (nil? (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
     (is (= valintaperuste-id (:id (get-doc valintaperuste/index-name valintaperuste-id)))))))