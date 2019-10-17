(ns kouta-indeksoija-service.fixture.common-indexer-fixture
  (:require
   [cheshire.core :as cheshire]
   [clojure.test :refer :all]
   [kouta-indeksoija-service.elastic.tools :refer [get-by-id get-doc]]
   [kouta-indeksoija-service.fixture.external-services :as mocks]
   [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
   [kouta-indeksoija-service.indexer.kouta.haku :as haku]
   [kouta-indeksoija-service.indexer.kouta.hakukohde :as hakukohde]
   [kouta-indeksoija-service.indexer.kouta.koulutus :as koulutus]
   [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus-search]
   [kouta-indeksoija-service.indexer.kouta.oppilaitos :as oppilaitos]
   [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as oppilaitos-search]
   [kouta-indeksoija-service.indexer.kouta.toteutus :as toteutus]
   [kouta-indeksoija-service.indexer.kouta.valintaperuste :as valintaperuste]))

(def koulutus-oid "1.2.246.562.13.00000000000000000001")
(def toteutus-oid "1.2.246.562.17.00000000000000000001")
(def haku-oid "1.2.246.562.29.00000000000000000001")
(def hakukohde-oid "1.2.246.562.20.00000000000000000001")
(def valintaperuste-id "a5e88367-555b-4d9e-aa43-0904e5ea0a13")
(def sorakuvaus-id "ffa8c6cf-a962-4bb2-bf61-fe8fc741fabd")
(def oppilaitos-oid "1.2.246.562.10.10101010101")
(def oppilaitoksen-osa-oid "1.2.246.562.10.10101010102")

(defn no-timestamp
  [json]
  (dissoc json :timestamp))

(defn json
  [name]
  (cheshire/parse-string (slurp (str "test/resources/kouta/" name ".json")) true))

(defn check-all-nil
  []
  (is (nil? (read koulutus-search/index-name koulutus-oid)))
  (is (nil? (read koulutus/index-name koulutus-oid)))
  (is (nil? (read toteutus/index-name toteutus-oid)))
  (is (nil? (read haku/index-name haku-oid)))
  (is (nil? (read hakukohde/index-name hakukohde-oid)))
  (is (nil? (read valintaperuste/index-name valintaperuste-id)))
  (is (nil? (read oppilaitos/index-name oppilaitos-oid)))
  (is (nil? (read oppilaitos/index-name mocks/Oppilaitos1)))
  (is (nil? (read oppilaitos/index-name mocks/Oppilaitos2)))
  (is (nil? (read oppilaitos-search/index-name oppilaitos-oid)))
  (is (nil? (read oppilaitos-search/index-name mocks/Oppilaitos1)))
  (is (nil? (read oppilaitos-search/index-name mocks/Oppilaitos2))))

(defn common-indexer-fixture
  [tests]
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

  (fixture/add-sorakuvaus-mock sorakuvaus-id
                               :tila "arkistoitu"
                               :nimi "Sorakuvaus 0"
                               :muokkaaja "1.2.246.562.24.62301161440"
                               :modified "2019-02-05T09:49")

  (fixture/add-valintaperuste-mock valintaperuste-id
                                   :tila "arkistoitu"
                                   :nimi "Valintaperuste 0"
                                   :sorakuvaus sorakuvaus-id
                                   :muokkaaja "1.2.246.562.24.62301161440"
                                   :modified "2019-02-05T09:49")

  (fixture/add-oppilaitos-mock oppilaitos-oid
                               :tila "julkaistu"
                               :muokkaaja "1.2.246.562.24.62301161440"
                               :modified "2019-02-05T09:49")

  (fixture/add-oppilaitoksen-osa-mock "1.2.246.562.10.10101010102"
                                      oppilaitos-oid
                                      :tila "julkaistu"
                                      :muokkaaja "1.2.246.562.24.62301161440"
                                      :modified "2019-02-05T09:49")

  (tests))
