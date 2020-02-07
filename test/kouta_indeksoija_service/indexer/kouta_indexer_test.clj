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
            [kouta-indeksoija-service.indexer.kouta.oppilaitos :as oppilaitos]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos-search]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.test-tools :refer [parse compare-json debug-pretty]]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [kouta-indeksoija-service.fixture.external-services :as mocks]))

(use-fixtures :each fixture/indices-fixture)
(use-fixtures :once common-indexer-fixture)

(deftest index-tallennettu-koulutus-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index tallennettu koulutus only to koulutus index"
     (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu")
     (check-all-nil)
     (i/index-koulutukset [koulutus-oid])
     (is (nil? (get-doc koulutus-search/index-name koulutus-oid)))
     (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
     (compare-json (no-timestamp (merge (json "kouta-koulutus-result") {:tila "tallennettu"}))
                   (no-timestamp (get-doc koulutus/index-name koulutus-oid)))
     (fixture/update-koulutus-mock koulutus-oid :tila "julkaistu"))))

(deftest index-julkaistu-koulutus-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index julkaistu koulutus also to search index"
     (check-all-nil)
     (i/index-koulutukset [koulutus-oid])
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
     (compare-json (no-timestamp (merge (json "kouta-koulutus-result") {:tila "julkaistu"}))
                   (no-timestamp (get-doc koulutus/index-name koulutus-oid))))))

(deftest index-arkistoitu-koulutus-test
  (fixture/with-mocked-indexing
    (testing "Indexer should delete koulutus from search indexes when it's arkistoitu"
      (check-all-nil)
      (i/index-koulutukset [koulutus-oid])
      (is (= "julkaistu" (:tila (get-doc koulutus/index-name koulutus-oid))))
      (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
      (is (< 0 (count-hits-by-key oppilaitos-search/index-name mocks/Oppilaitos1 :koulutusOid koulutus-oid)))
      (fixture/update-koulutus-mock koulutus-oid :tila "arkistoitu")
      (i/index-koulutukset [koulutus-oid])
      (is (= "arkistoitu" (:tila (get-doc koulutus/index-name koulutus-oid))))
      (is (nil? (get-doc koulutus-search/index-name koulutus-oid)))
      (is (= 0 (count-hits-by-key oppilaitos-search/index-name mocks/Oppilaitos1 :koulutusOid koulutus-oid)))
      (fixture/update-koulutus-mock koulutus-oid :tila "julkaistu"))))

(deftest index-toteutus-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index toteutus to toteutus index and update related indexes"
     (check-all-nil)
     (i/index-toteutukset [toteutus-oid])
     (compare-json (no-timestamp (json "kouta-toteutus-result"))
                   (no-timestamp (get-doc toteutus/index-name toteutus-oid)))
     (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (= koulutus-oid (:oid (get-doc koulutus/index-name koulutus-oid)))))))

(deftest index-arkistoitu-toteutus-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index delete toteutus from search indexes when it's arkistoitu"
     (check-all-nil)
     (fixture/update-toteutus-mock toteutus-oid :tila "julkaistu")
     (i/index-toteutukset [toteutus-oid])
     (is (= "julkaistu" (:tila (get-doc toteutus/index-name toteutus-oid))))
     (is (< 0 (count-hits-by-key koulutus-search/index-name koulutus-oid :toteutusOid toteutus-oid)))
     (is (< 0 (count-hits-by-key oppilaitos-search/index-name mocks/Oppilaitos1 :toteutusOid toteutus-oid)))
     (fixture/update-toteutus-mock toteutus-oid :tila "arkistoitu")
     (i/index-toteutukset [toteutus-oid])
     (is (= "arkistoitu" (:tila (get-doc toteutus/index-name toteutus-oid))))
     (is (= 0 (count-hits-by-key koulutus-search/index-name koulutus-oid :toteutusOid toteutus-oid)))
     (is (= 0 (count-hits-by-key oppilaitos-search/index-name mocks/Oppilaitos1 :toteutusOid toteutus-oid))))))

(deftest index-haku-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index haku to haku index and update related indexes"
     (check-all-nil)
     (i/index-haut [haku-oid])
     (compare-json (no-timestamp (json "kouta-haku-result"))
                   (no-timestamp (get-doc haku/index-name haku-oid)))
     (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (nil? (get-doc koulutus/index-name koulutus-oid)))
     (is (nil? (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1)))))))

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

(deftest index-valintaperuste-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index valintaperuste to valintaperuste index"
     (check-all-nil)
     (i/index-valintaperusteet [valintaperuste-id])
     (compare-json (no-timestamp (json "kouta-valintaperuste-result"))
                   (no-timestamp (get-doc valintaperuste/index-name valintaperuste-id))))))

(deftest index-sorakuvaus-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index valintaperuste related to sorakuvaus to valintaperuste index"
     (check-all-nil)
     (i/index-sorakuvaukset [sorakuvaus-id])
     (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
     (compare-json (no-timestamp (json "kouta-valintaperuste-result"))
                   (no-timestamp (get-doc valintaperuste/index-name valintaperuste-id))))))

(defn mock-organisaatio-hierarkia
  [oid & {:as params}]
  (parse (str "test/resources/organisaatiot/1.2.246.562.10.10101010101-hierarkia-v4.json")))

(deftest index-oppilaitos-test
  (fixture/with-mocked-indexing
   (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 mock-organisaatio-hierarkia]
     (testing "Indexer should index oppilaitos and it's osat to oppilaitos index"
       (check-all-nil)
       (i/index-oppilaitokset [oppilaitos-oid])
       (compare-json (no-timestamp (json "kouta-oppilaitos-result"))
                     (no-timestamp (get-doc oppilaitos/index-name oppilaitos-oid)))))))

(deftest index-oppilaitos-test-2
 (fixture/with-mocked-indexing
  (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 mock-organisaatio-hierarkia]
    (testing "Indexer should index oppilaitos and it's osat to oppilaitos index when given oppilaitoksen osa oid"
      (check-all-nil)
      (i/index-oppilaitos oppilaitoksen-osa-oid)
      (compare-json (no-timestamp (json "kouta-oppilaitos-result"))
                    (no-timestamp (get-doc oppilaitos/index-name oppilaitos-oid)))))))

(deftest index-oppilaitos-test-3
  (fixture/with-mocked-indexing
    (with-redefs [kouta-indeksoija-service.indexer.cache.hierarkia/get-hierarkia (fn [oid]
                  (update-in (parse (str "test/resources/organisaatiot/1.2.246.562.10.10101010101-hierarkia-v4.json"))
                             [:organisaatiot 0 :children 0 :organisaatiotyypit]
                             (constantly ["organisaatiotyyppi_02", "organisaatiotyyppi_06"])))]
     (testing "Indexer should not index oppilaitos when invalid organisaatiotyyppi"
       (check-all-nil)
       (i/index-oppilaitos oppilaitos-oid)
       (check-all-nil)))))

(deftest index-oppilaitos-test-4
  (fixture/with-mocked-indexing
   (testing "Indexer should index also koulutus when indexing oppilaitos"
     (check-all-nil)
     (i/index-oppilaitokset [mocks/Oppilaitos1])
     (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid)))))))

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
     (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4 mock-organisaatio-hierarkia]
       (check-all-nil)
       (i/index-oppilaitokset [oppilaitos-oid]))
     (with-redefs [kouta-indeksoija-service.indexer.cache.hierarkia/get-hierarkia (fn [oid]
                   (update-in (parse (str "test/resources/organisaatiot/1.2.246.562.10.10101010101-hierarkia-v4.json"))
                              [:organisaatiot 0 :children 0 :status]
                              (constantly "PASSIIVINEN")))]
       (is (= oppilaitos-oid (:oid (get-doc oppilaitos/index-name oppilaitos-oid))))
       (is (= oppilaitos-oid (:oid (get-doc oppilaitos-search/index-name oppilaitos-oid))))
       (i/index-oppilaitokset [oppilaitos-oid])
       (is (= nil (:oid (get-doc oppilaitos/index-name oppilaitos-oid))))
       (is (= nil (:oid (get-doc oppilaitos-search/index-name oppilaitos-oid))))))))

(deftest index-all-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index all"
     (check-all-nil)
     (i/index-all-kouta)
     (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
     (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
     (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
     (is (= koulutus-oid (:oid (get-doc koulutus/index-name koulutus-oid))))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (= oppilaitos-oid (:oid (get-doc oppilaitos-search/index-name oppilaitos-oid))))
     (is (= valintaperuste-id (:id (get-doc valintaperuste/index-name valintaperuste-id)))))))

(deftest index-changes-oids-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index changed oids"
     (check-all-nil)
     (i/index-oids {:hakukohteet [hakukohde-oid]})
     (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
     (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
     (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
     (is (nil? (:oid (get-doc koulutus/index-name koulutus-oid))))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (nil? (:oid (get-doc oppilaitos-search/index-name oppilaitos-oid))))
     (is (nil? (:id (get-doc valintaperuste/index-name valintaperuste-id)))))))

(deftest index-changes-oids-test-2
  (fixture/with-mocked-indexing
   (testing "Indexer should index changed oids 2"
     (check-all-nil)
     (i/index-oids {:sorakuvaukset [sorakuvaus-id]})
     (is (nil? (:oid (get-doc haku/index-name haku-oid))))
     (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
     (is (nil? (:oid (get-doc toteutus/index-name toteutus-oid))))
     (is (nil? (:oid (get-doc koulutus/index-name koulutus-oid))))
     (is (nil? (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (nil? (:oid (get-doc oppilaitos-search/index-name oppilaitos-oid))))
     (is (= valintaperuste-id (:id (get-doc valintaperuste/index-name valintaperuste-id)))))))

(deftest index-all-koulutukset-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index all koulutukset"
     (check-all-nil)
     (i/index-all-koulutukset)
     (is (nil? (get-doc haku/index-name haku-oid)))
     (is (nil? (get-doc hakukohde/index-name hakukohde-oid)))
     (is (nil? (get-doc toteutus/index-name toteutus-oid)))
     (is (= koulutus-oid (:oid (get-doc koulutus/index-name koulutus-oid))))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
     (is (nil? (get-doc valintaperuste/index-name valintaperuste-id))))))

(deftest index-all-toteutukset-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index all toteutukset"
     (check-all-nil)
     (i/index-all-toteutukset)
     (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
     (is (nil? (get-doc hakukohde/index-name hakukohde-oid)))
     (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
     (is (= koulutus-oid (:oid (get-doc koulutus/index-name koulutus-oid))))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (= mocks/Oppilaitos1 (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
     (is (nil? (get-doc valintaperuste/index-name valintaperuste-id))))))

(deftest index-all-hakukohteet-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index all hakukohteet"
     (check-all-nil)
     (i/index-all-hakukohteet)
     (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
     (is (= hakukohde-oid (:oid (get-doc hakukohde/index-name hakukohde-oid))))
     (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
     (is (nil? (get-doc koulutus/index-name koulutus-oid)))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (nil? (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
     (is (nil? (get-doc valintaperuste/index-name valintaperuste-id))))))

(deftest index-all-haut-test
  (fixture/with-mocked-indexing
   (testing "Indexer should index all haut"
     (check-all-nil)
     (i/index-all-haut)
     (is (= haku-oid (:oid (get-doc haku/index-name haku-oid))))
     (is (nil? (get-doc hakukohde/index-name hakukohde-oid)))
     (is (= toteutus-oid (:oid (get-doc toteutus/index-name toteutus-oid))))
     (is (nil? (get-doc koulutus/index-name koulutus-oid)))
     (is (= koulutus-oid (:oid (get-doc koulutus-search/index-name koulutus-oid))))
     (is (nil? (:oid (get-doc oppilaitos-search/index-name mocks/Oppilaitos1))))
     (is (nil? (get-doc valintaperuste/index-name valintaperuste-id))))))

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