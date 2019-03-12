(ns konfo-indeksoija-service.kouta-indexer-test
  (:require [konfo-indeksoija-service.kouta.indexer :as i]
            [konfo-indeksoija-service.kouta.koulutus :as koulutus]
            [konfo-indeksoija-service.kouta.koulutus-search :as search]
            [konfo-indeksoija-service.kouta.toteutus :as toteutus]
            [konfo-indeksoija-service.kouta.haku :as haku]
            [konfo-indeksoija-service.kouta.valintaperuste :as valintaperuste]
            [konfo-indeksoija-service.kouta.hakukohde :as hakukohde]
            [konfo-indeksoija-service.elastic.tools :refer [get-by-id]]
            [konfo-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [konfo-indeksoija-service.fixture.external-services :as mocks]
            [cheshire.core :as cheshire]
            [konfo-indeksoija-service.test-tools :as tools]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [mocks.externals-mock :refer [with-externals-mock]]
            [midje.sweet :refer :all]))

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

(against-background
  [(before :contents (do (init-elastic-test) (fixture/init)))
   (after :facts (fixture/reset-indices))
   (after :contents (stop-elastic-test))]

  (fixture/with-mocked-indexing

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
        (read search/index-name koulutus-oid) => nil
        (read koulutus/index-name koulutus-oid) => nil
        (read toteutus/index-name toteutus-oid) => nil
        (read haku/index-name haku-oid) => nil
        (read hakukohde/index-name hakukohde-oid) => nil
        (read valintaperuste/index-name valintaperuste-id) => nil)

      (fact "Indexer should index tallennettu koulutus only to koulutus index"
            (fixture/update-koulutus-mock koulutus-oid :tila "tallennettu")
            (check-all-nil)
            (i/index-koulutus koulutus-oid)
            (read search/index-name koulutus-oid) => nil
            (no-timestamp (read koulutus/index-name koulutus-oid)) => (no-timestamp (merge (json "kouta-koulutus-result") {:tila "tallennettu"}))
            (fixture/update-koulutus-mock koulutus-oid :tila "julkaistu"))

      (fact "Indexer should index julkaistu koulutus also to search index"
            (check-all-nil)
            (i/index-koulutus koulutus-oid)
            (no-timestamp (read search/index-name koulutus-oid)) => (no-timestamp (json "kouta-koulutus-search-result"))
            (no-timestamp (read koulutus/index-name koulutus-oid)) => (no-timestamp (merge (json "kouta-koulutus-result") {:tila "julkaistu"})))

      (fact "Indexer should index toteutus to toteutus index and update related indexes"
            (check-all-nil)(i/index-toteutus toteutus-oid)
            (no-timestamp (read toteutus/index-name toteutus-oid)) => (no-timestamp (json "kouta-toteutus-result"))
            (:oid (read search/index-name koulutus-oid)) => koulutus-oid
            (:oid (read koulutus/index-name koulutus-oid)) => koulutus-oid)

      (fact "Indexer should index haku to haku index and update related indexes"
            (check-all-nil)(i/index-haku haku-oid)
            (no-timestamp (read haku/index-name haku-oid)) => (no-timestamp (json "kouta-haku-result"))
            (:oid (read toteutus/index-name toteutus-oid)) => toteutus-oid
            (:oid (read search/index-name koulutus-oid)) => koulutus-oid
            (read koulutus/index-name koulutus-oid) => nil)

      (fact "Indexer should index hakukohde to hakukohde index and update related indexes"
            (check-all-nil)
            (i/index-hakukohde hakukohde-oid)
            (no-timestamp (read hakukohde/index-name hakukohde-oid)) => (no-timestamp (json "kouta-hakukohde-result"))
            (:oid (read haku/index-name haku-oid)) => haku-oid
            (:oid (read toteutus/index-name toteutus-oid)) => nil
            (:oid (read search/index-name koulutus-oid)) => koulutus-oid
            (read koulutus/index-name koulutus-oid) => nil)

      (fact "Indexer should index valintaperuste to valintaperuste index"
            (check-all-nil)
            (i/index-valintaperuste valintaperuste-id)
            (no-timestamp (read valintaperuste/index-name valintaperuste-id)) => (no-timestamp (json "kouta-valintaperuste-result")))

      (fact "Indexer should index all"
            (check-all-nil)
            (i/index-all)
            (:oid (read haku/index-name haku-oid)) => haku-oid
            (:oid (read hakukohde/index-name hakukohde-oid)) => hakukohde-oid
            (:oid (read toteutus/index-name toteutus-oid)) => toteutus-oid
            (:oid (read koulutus/index-name koulutus-oid)) => koulutus-oid
            (:oid (read search/index-name koulutus-oid)) => koulutus-oid
            (:id (read valintaperuste/index-name valintaperuste-id)) => valintaperuste-id)

      (fact "Indexer should index changed oids"
            (check-all-nil)
            (i/index-oids {:hakukohteet [hakukohde-oid]})
            (:oid (read haku/index-name haku-oid)) => haku-oid
            (:oid (read hakukohde/index-name hakukohde-oid)) => hakukohde-oid
            (:oid (read toteutus/index-name toteutus-oid)) => nil
            (:oid (read koulutus/index-name koulutus-oid)) => nil
            (:oid (read search/index-name koulutus-oid)) => koulutus-oid
            (:id (read valintaperuste/index-name valintaperuste-id)) => nil))))