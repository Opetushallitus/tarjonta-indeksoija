(ns mocks.kouta-index-mocks
  (:require
   [clj-log.access-log]
   [clojure.test :refer :all]
   [clojure.string :as string]
   [clojure.java.shell :refer [sh]]
   [clojure.java.io :as io]
   [clj-elasticsearch.elastic-utils :as e-utils]
   [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
   [kouta-indeksoija-service.fixture.external-services :as mocks]))

(intern 'clj-log.access-log 'service "kouta-indeksoija")

;Ajamalla tämän namespacen testin voi generoida elastic-testidataa kouta-index-palvelua varten
;lein test :only mocks.kouta-index-mocks

(defonce koulutusOid1 "1.2.246.562.13.000001")
(defonce koulutusOid2 "1.2.246.562.13.000002")
(defonce koulutusOid3 "1.2.246.562.13.000003")
(defonce koulutusOid4 "1.2.246.562.13.000004")
(defonce koulutusOid5 "1.2.246.562.13.000005")
(defonce yoKoulutusOid1 "1.2.246.562.13.000006")

(defonce sorakuvausId "31972648-ebb7-4185-ac64-31fa6b841e39")

(defonce hakuOid1    "1.2.246.562.29.0000001")
(defonce hakuOid2    "1.2.246.562.29.0000002")
(defonce hakuOid3    "1.2.246.562.29.0000003")
(defonce hakuOid4    "1.2.246.562.29.0000004")
(defonce hakuOid5    "1.2.246.562.29.0000005")

(defonce toteutusOid1  "1.2.246.562.17.000001")
(defonce toteutusOid2  "1.2.246.562.17.000002")
(defonce toteutusOid3  "1.2.246.562.17.000003")
(defonce toteutusOid4  "1.2.246.562.17.000004")
(defonce toteutusOid5  "1.2.246.562.17.000005")

(defonce hakukohdeOid1     "1.2.246.562.20.0000001")
(defonce hakukohdeOid2     "1.2.246.562.20.0000002")
(defonce hakukohdeOid3     "1.2.246.562.20.0000003")
(defonce hakukohdeOid4     "1.2.246.562.20.0000004")
(defonce hakukohdeOid5     "1.2.246.562.20.0000005")
(defonce hakukohdeOid6     "1.2.246.562.20.0000006")

(defonce valintaperusteId1 "31972648-ebb7-4185-ac64-31fa6b841e34")
(defonce valintaperusteId2 "31972648-ebb7-4185-ac64-31fa6b841e35")
(defonce valintaperusteId3 "31972648-ebb7-4185-ac64-31fa6b841e36")
(defonce valintaperusteId4 "31972648-ebb7-4185-ac64-31fa6b841e37")
(defonce valintaperusteId5 "31972648-ebb7-4185-ac64-31fa6b841e38")

(defn- export-elastic-data []
  (println "Starting elasticdump...")
  (.mkdirs (io/file "elasticdump/kouta-index"))
  (let [e-host (string/replace e-utils/elastic-host #"127\.0\.0\.1|localhost" "host.docker.internal")
        pwd (System/getProperty "user.dir")
        p (sh "./dump_elastic_data.sh" (str pwd "/elasticdump/kouta-index") e-host)]
    (println (:out p))
    (println (:err p))))

(comment
  (deftest -main []
    (fixture/init)
    (fixture/add-sorakuvaus-mock sorakuvausId :tila "julkaistu" :nimi "Kiva SORA-kuvaus")

    (fixture/add-koulutus-mock koulutusOid1 :tila "julkaistu" :nimi "Hauska koulutus" :organisaatio mocks/Oppilaitos2 :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock koulutusOid2 :tila "julkaistu" :nimi "Tietojenkäsittelytieteen perusopinnot" :modified "2018-05-05T12:02:23" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock koulutusOid3 :tila "julkaistu" :nimi "Tietotekniikan perusopinnot" :muokkaaja "1.2.246.562.24.55555555555" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock koulutusOid4 :tila "arkistoitu" :nimi "Tietojenkäsittelytieteen perusopinnot" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock koulutusOid5 :tila "tallennettu" :nimi "Tietojenkäsittelytieteen perusopinnot" :sorakuvausId sorakuvausId :julkinen "true")
    (fixture/add-koulutus-mock yoKoulutusOid1 :tila "tallennettu" :koulutustyyppi "yo" :nimi "Diplomi-insinööri" :sorakuvausId sorakuvausId :metadata fixture/yo-koulutus-metadata)

    (fixture/add-toteutus-mock toteutusOid1 koulutusOid1 :tila "julkaistu"   :nimi "Automaatioalan perusopinnot" :organisaatio mocks/Oppilaitos1)
    (fixture/add-toteutus-mock toteutusOid2 koulutusOid1 :tila "julkaistu"   :nimi "Automatiikan perusopinnot")
    (fixture/add-toteutus-mock toteutusOid3 koulutusOid1 :tila "julkaistu"   :nimi "Autoalan perusopinnot" :modified "2018-05-05T12:02:23" :muokkaaja "1.2.246.562.24.55555555555")
    (fixture/add-toteutus-mock toteutusOid4 koulutusOid1 :tila "arkistoitu"  :nimi "Autoalan perusopinnot" :modified "2018-06-05T12:02:23")
    (fixture/add-toteutus-mock toteutusOid5 koulutusOid1 :tila "tallennettu" :nimi "Autoalan perusopinnot" :modified "2018-06-05T12:02:23")

    (fixture/add-haku-mock hakuOid1 :tila "julkaistu"   :nimi "Yhteishaku" :organisaatio mocks/Oppilaitos2)
    (fixture/add-haku-mock hakuOid2 :tila "julkaistu"   :nimi "Yhteishaku" :hakutapaKoodiUri "hakutapa_01")
    (fixture/update-haku-mock hakuOid2 :metadata fixture/haku-metadata)
    (fixture/add-haku-mock hakuOid3 :tila "julkaistu"   :nimi "Jatkuva haku" :modified "2018-05-05T12:02:23" :muokkaaja "1.2.246.562.24.55555555555")
    (fixture/add-haku-mock hakuOid4 :tila "arkistoitu"  :nimi "Erillishaku" :modified "2018-06-05T12:02:23" :hakutapaKoodiUri "hakutapa_02")
    (fixture/add-haku-mock hakuOid5 :tila "tallennettu" :nimi "Jatkuva haku" :modified "2018-06-05T12:02:23")

    (fixture/add-hakukohde-mock hakukohdeOid1 toteutusOid1 hakuOid1 :tila "julkaistu" :esitysnimi "Hakukohde" :valintaperuste valintaperusteId1 :organisaatio mocks/Oppilaitos2)
    (fixture/add-hakukohde-mock hakukohdeOid2 toteutusOid4 hakuOid1 :tila "julkaistu" :esitysnimi "Hakukohde" :valintaperuste valintaperusteId1)
    (fixture/add-hakukohde-mock hakukohdeOid3 toteutusOid2 hakuOid1 :tila "julkaistu" :esitysnimi "autoalan hakukohde" :valintaperuste valintaperusteId1 :modified "2018-05-05T12:02:23" :muokkaaja "1.2.246.562.24.55555555555")
    (fixture/add-hakukohde-mock hakukohdeOid4 toteutusOid5 hakuOid1 :tila "arkistoitu" :esitysnimi "Autoalan hakukohde" :valintaperuste valintaperusteId1 :modified "2018-06-05T12:02:23")
    (fixture/add-hakukohde-mock hakukohdeOid5 toteutusOid5 hakuOid1 :tila "tallennettu" :esitysnimi "Autoalan hakukohde" :valintaperuste valintaperusteId1 :modified "2018-06-05T12:02:23")
    (fixture/add-hakukohde-mock hakukohdeOid6 toteutusOid1 hakuOid1 :tila "tallennettu" :nimi "Hakukohde" :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId1)

    (fixture/add-valintaperuste-mock valintaperusteId1 :tila "julkaistu" :nimi "Valintaperustekuvaus" :organisaatio mocks/Oppilaitos2)
    (fixture/add-valintaperuste-mock valintaperusteId2 :tila "julkaistu" :nimi "Valintaperustekuvaus" :julkinen true)
    (fixture/add-valintaperuste-mock valintaperusteId3 :tila "julkaistu" :nimi "Kiva valintaperustekuvaus" :modified "2018-05-05T12:02:23" :muokkaaja "1.2.246.562.24.55555555555")
    (fixture/add-valintaperuste-mock valintaperusteId4 :tila "arkistoitu" :nimi "Kiva valintaperustekuvaus" :modified "2018-06-05T12:02:23")
    (fixture/add-valintaperuste-mock valintaperusteId5 :tila "tallennettu" :nimi "Kiva valintaperustekuvaus" :modified "2018-06-05T12:02:23")

    (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5 yoKoulutusOid1]
                                                 :toteutukset [toteutusOid1 toteutusOid2 toteutusOid3 toteutusOid4 toteutusOid5]
                                                 :haut [hakuOid1 hakuOid2 hakuOid3 hakuOid4 hakuOid5]
                                                 :hakukohteet [hakukohdeOid1 hakukohdeOid2 hakukohdeOid3 hakukohdeOid4 hakukohdeOid5]
                                                 :valintaperusteet [valintaperusteId1 valintaperusteId2 valintaperusteId3 valintaperusteId4 valintaperusteId5]})
    (export-elastic-data)))
