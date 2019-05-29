(ns kouta-indeksoija-service.kouta.kouta-indexer-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.kouta.indexer :as i]
            [kouta-indeksoija-service.kouta.koulutus :as koulutus]
            [kouta-indeksoija-service.kouta.koulutus-search :as search]
            [kouta-indeksoija-service.kouta.toteutus :as toteutus]
            [kouta-indeksoija-service.kouta.haku :as haku]
            [kouta-indeksoija-service.kouta.valintaperuste :as valintaperuste]
            [kouta-indeksoija-service.kouta.hakukohde :as hakukohde]
            [kouta-indeksoija-service.elastic.tools :refer [get-by-id]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [cheshire.core :as cheshire]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [mocks.externals-mock :refer [with-externals-mock]]))

(defn no-timestamp
 [json]
 (dissoc json :timestamp))

(defn json
 [name]
 (cheshire/parse-string (slurp (str "test/resources/kouta/" name ".json")) true))

(defn read
 [index id]
 (get-by-id index index id))

(defn debug-pretty
 [json]
 (println (cheshire/generate-string json {:pretty true})))

(use-fixtures :each (fn [test] (do (test) (fixture/reset-indices))))

(let [koulutus-oid "1.2.246.562.13.00000000000000000001"
      toteutus-oid "1.2.246.562.17.00000000000000000001"
      haku-oid "1.2.246.562.29.00000000000000000001"
      hakukohde-oid "1.2.246.562.20.00000000000000000001"
      valintaperuste-id "a5e88367-555b-4d9e-aa43-0904e5ea0a13"]

  (fixture/add-koulutus-mock koulutus-oid
                             :tila "julkaistu"
                             :nimi "Autoalan perustutkinto 0"
                             :muokkaaja "1.2.246.562.24.62301161440"
                             :julkinen "true"
                             :modified "2019-01-31T09:11"
                             :tarjoajat "1.2.246.562.10.54545454545")

  (fixture/add-toteutus-mock "1.2.246.562.17.00000000000000000001"
                             koulutus-oid
                             :tila "arkistoitu"
                             :nimi "Koulutuksen 0 toteutus 0"
                             :muokkaaja "1.2.246.562.24.62301161440"
                             :modified "2019-02-01T13:16"
                             :tarjoajat (str mocks/Toimipiste1OfOppilaitos1 "," mocks/Toimipiste2OfOppilaitos1)
                             :metadata (slurp (str "test/resources/kouta/toteutus-metadata.json")))

  (fixture/add-toteutus-mock "1.2.246.562.17.00000000000000000002"
                             koulutus-oid
                             :tila "julkaistu"
                             :nimi "Koulutuksen 0 toteutus 1"
                             :muokkaaja "1.2.246.562.24.62301161440"
                             :modified "2019-02-01T13:16"
                             :tarjoajat mocks/Toimipiste1OfOppilaitos1
                             :metadata (slurp (str "test/resources/kouta/toteutus-metadata.json")))

  (fixture/add-toteutus-mock "1.2.246.562.17.00000000000000000003"
                             koulutus-oid
                             :tila "julkaistu"
                             :nimi "Koulutuksen 0 toteutus 2"
                             :muokkaaja "1.2.246.562.24.62301161440"
                             :modified "2019-02-01T13:16"
                             :tarjoajat mocks/Toimipiste2OfOppilaitos1
                             :metadata (slurp (str "test/resources/kouta/toteutus-metadata.json")))

  (fixture/add-haku-mock haku-oid
                         :tila "julkaistu"
                         :nimi "Haku 0"
                         :muokkaaja "1.2.246.562.24.62301161440"
                         :modified "2019-02-05T09:49")

  (fixture/add-hakukohde-mock hakukohde-oid
                              toteutus-oid
                              haku-oid
                              :tila "arkistoitu"
                              :valintaperuste valintaperuste-id
                              :nimi "Koulutuksen 0 toteutuksen 0 hakukohde 0"
                              :muokkaaja "1.2.246.562.24.62301161440"
                              :modified "2019-02-05T09:49")

  (fixture/add-hakukohde-mock "1.2.246.562.20.00000000000000000002"
                              "1.2.246.562.17.00000000000000000003"
                              haku-oid
                              :tila "julkaistu"
                              :valintaperuste valintaperuste-id
                              :nimi "Koulutuksen 0 toteutuksen 2 hakukohde 0"
                              :muokkaaja "1.2.246.562.24.62301161440"
                              :hakuaikaAlkaa "2018-10-10T12:00"
                              :hakuaikaPaattyy "2030-11-10T12:00"
                              :modified "2019-02-05T09:49")

  (fixture/add-valintaperuste-mock valintaperuste-id
                                   :tila "arkistoitu"
                                   :nimi "Valintaperuste 0"
                                   :muokkaaja "1.2.246.562.24.62301161440"
                                   :modified "2019-02-05T09:49")

  (defn check-all-nil
    []
    (is (= nil (read search/index-name koulutus-oid)))
    (is (= nil (read koulutus/index-name koulutus-oid)))
    (is (= nil (read toteutus/index-name toteutus-oid)))
    (is (= nil (read haku/index-name haku-oid)))
    (is (= nil (read hakukohde/index-name hakukohde-oid)))
    (is (= nil (read valintaperuste/index-name valintaperuste-id))))

  (deftest index-tallennettu-koulutus-test
    (fixture/with-mocked-indexing
      (testing "Indexer should index tallennettu koulutus only to koulutus index"
        (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu")
        (check-all-nil)
        (i/index-koulutus koulutus-oid)
        (is (= nil (read search/index-name koulutus-oid)))
        (is (= (no-timestamp (merge (json "kouta-koulutus-result") {:tila "tallennettu"}))
               (no-timestamp (read koulutus/index-name koulutus-oid))))
        (fixture/update-koulutus-mock koulutus-oid :tila "julkaistu"))))

  (deftest index-julkaistu-koulutus-test
    (fixture/with-mocked-indexing
      (testing "Indexer should index julkaistu koulutus also to search index"
        (check-all-nil)
        (i/index-koulutus koulutus-oid)
        (is (= (no-timestamp (json "kouta-koulutus-search-result"))
               (no-timestamp (read search/index-name koulutus-oid))))
        (is (= (no-timestamp (merge (json "kouta-koulutus-result") {:tila "julkaistu"}))
               (no-timestamp (read koulutus/index-name koulutus-oid)))))))

  (deftest index-toteutus-test
    (fixture/with-mocked-indexing
      (testing "Indexer should index toteutus to toteutus index and update related indexes"
        (check-all-nil)(i/index-toteutus toteutus-oid)
        (is (= (no-timestamp (json "kouta-toteutus-result"))
               (no-timestamp (read toteutus/index-name toteutus-oid))))
        (is (= koulutus-oid (:oid (read search/index-name koulutus-oid))))
        (is (= koulutus-oid (:oid (read koulutus/index-name koulutus-oid)))))))

  (deftest index-haku-test
    (fixture/with-mocked-indexing
      (testing "Indexer should index haku to haku index and update related indexes"
        (check-all-nil)(i/index-haku haku-oid)
        (is (= (no-timestamp (json "kouta-haku-result"))
               (no-timestamp (read haku/index-name haku-oid))))
        (is (= nil (:oid (read toteutus/index-name toteutus-oid))))
        (is (= koulutus-oid (:oid (read search/index-name koulutus-oid))))
        (is (= nil (read koulutus/index-name koulutus-oid))))))

  (deftest index-hakukohde-test
    (fixture/with-mocked-indexing
      (testing "Indexer should index hakukohde to hakukohde index and update related indexes"
        (check-all-nil)
        (i/index-hakukohde hakukohde-oid)
        (is (= (no-timestamp (json "kouta-hakukohde-result"))
               (no-timestamp (read hakukohde/index-name hakukohde-oid))))
        (is (= haku-oid (:oid (read haku/index-name haku-oid))))
        (is (= toteutus-oid (:oid (read toteutus/index-name toteutus-oid))))
        (is (= koulutus-oid (:oid (read search/index-name koulutus-oid))))
        (is (= nil (read koulutus/index-name koulutus-oid))))))

  (deftest index-valintaperuste-test
    (fixture/with-mocked-indexing
      (testing "Indexer should index valintaperuste to valintaperuste index"
       (check-all-nil)
       (i/index-valintaperuste valintaperuste-id)
       (is (= (no-timestamp (json "kouta-valintaperuste-result"))
              (no-timestamp (read valintaperuste/index-name valintaperuste-id)))))))

  (deftest index-all-test
    (fixture/with-mocked-indexing
      (testing "Indexer should index all"
        (check-all-nil)
        (i/index-all)
        (is (= haku-oid (:oid (read haku/index-name haku-oid))))
        (is (= hakukohde-oid (:oid (read hakukohde/index-name hakukohde-oid))))
        (is (= toteutus-oid (:oid (read toteutus/index-name toteutus-oid))))
        (is (= koulutus-oid (:oid (read koulutus/index-name koulutus-oid))))
        (is (= koulutus-oid (:oid (read search/index-name koulutus-oid))))
        (is (= valintaperuste-id (:id (read valintaperuste/index-name valintaperuste-id)))))))

  (deftest index-changes-oids-test
    (fixture/with-mocked-indexing
      (testing "Indexer should index changed oids"
        (check-all-nil)
        (i/index-oids {:hakukohteet [hakukohde-oid]})
        (is (= haku-oid (:oid (read haku/index-name haku-oid))))
        (is (= hakukohde-oid (:oid (read hakukohde/index-name hakukohde-oid))))
        (is (= toteutus-oid (:oid (read toteutus/index-name toteutus-oid))))
        (is (= nil (:oid (read koulutus/index-name koulutus-oid))))
        (is (= koulutus-oid (:oid (read search/index-name koulutus-oid))))
        (is (= nil (:id (read valintaperuste/index-name valintaperuste-id)))))))

  (deftest index-all-koulutukset-test
    (fixture/with-mocked-indexing
     (testing "Indexer should index all koulutukset"
       (check-all-nil)
       (i/index-all-koulutukset)
       (is (= nil (read haku/index-name haku-oid)))
       (is (= nil (read hakukohde/index-name hakukohde-oid)))
       (is (= nil (read toteutus/index-name toteutus-oid)))
       (is (= koulutus-oid (:oid (read koulutus/index-name koulutus-oid))))
       (is (= koulutus-oid (:oid (read search/index-name koulutus-oid))))
       (is (= nil (read valintaperuste/index-name valintaperuste-id))))))

  (deftest index-all-toteutukset-test
    (fixture/with-mocked-indexing
     (testing "Indexer should index all toteutukset"
       (check-all-nil)
       (i/index-all-toteutukset)
       (is (= nil (read haku/index-name haku-oid)))
       (is (= nil (read hakukohde/index-name hakukohde-oid)))
       (is (= toteutus-oid (:oid (read toteutus/index-name toteutus-oid))))
       (is (= koulutus-oid (:oid (read koulutus/index-name koulutus-oid))))
       (is (= koulutus-oid (:oid (read search/index-name koulutus-oid))))
       (is (= nil (read valintaperuste/index-name valintaperuste-id))))))

  (deftest index-all-hakukohteet-test
    (fixture/with-mocked-indexing
     (testing "Indexer should index all hakukohteet"
       (check-all-nil)
       (i/index-all-hakukohteet)
       (is (= haku-oid (:oid (read haku/index-name haku-oid))))
       (is (= hakukohde-oid (:oid (read hakukohde/index-name hakukohde-oid))))
       (is (= toteutus-oid (:oid (read toteutus/index-name toteutus-oid))))
       (is (= nil (read koulutus/index-name koulutus-oid)))
       (is (= koulutus-oid (:oid (read search/index-name koulutus-oid))))
       (is (= nil (read valintaperuste/index-name valintaperuste-id))))))

  (deftest index-all-haut-test
    (fixture/with-mocked-indexing
     (testing "Indexer should index all haut"
       (check-all-nil)
       (i/index-all-haut)
       (is (= haku-oid (:oid (read haku/index-name haku-oid))))
       (is (= nil (read hakukohde/index-name hakukohde-oid)))
       (is (= nil (read toteutus/index-name toteutus-oid)))
       (is (= nil (read koulutus/index-name koulutus-oid)))
       (is (= koulutus-oid (:oid (read search/index-name koulutus-oid))))
       (is (= nil (read valintaperuste/index-name valintaperuste-id))))))

  (deftest index-all-valintaperusteet-test
    (fixture/with-mocked-indexing
     (testing "Indexer should index all valintaperusteet"
       (check-all-nil)
       (i/index-all-valintaperusteet)
       (is (= nil (read haku/index-name haku-oid)))
       (is (= nil (read hakukohde/index-name hakukohde-oid)))
       (is (= nil (read toteutus/index-name toteutus-oid)))
       (is (= nil (read koulutus/index-name koulutus-oid)))
       (is (= nil (read search/index-name koulutus-oid)))
       (is (= valintaperuste-id (:id (read valintaperuste/index-name valintaperuste-id))))))))