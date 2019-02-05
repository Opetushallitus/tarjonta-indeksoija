(ns konfo-indeksoija-service.kouta-indexer-test
  (:require [konfo-indeksoija-service.kouta.indexer :as i]
            [konfo-indeksoija-service.kouta.koulutus :as koulutus]
            [konfo-indeksoija-service.kouta.koulutus-search :as search]
            [konfo-indeksoija-service.kouta.toteutus :as toteutus]
            [konfo-indeksoija-service.kouta.haku :as haku]
            [konfo-indeksoija-service.kouta.valintaperuste :as valintaperuste]
            [konfo-indeksoija-service.kouta.hakukohde :as hakukohde]
            [konfo-indeksoija-service.rest.kouta :as kouta-backend]
            [konfo-indeksoija-service.kouta.common :as common]
            [konfo-indeksoija-service.rest.koodisto :as koodisto-service]
            [konfo-indeksoija-service.elastic.tools :refer [get-by-id]]
            [cheshire.core :as cheshire]
            [konfo-indeksoija-service.test-tools :as tools]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [mocks.externals-mock :refer [with-externals-mock]]
            [midje.sweet :refer :all]))

(defn json
  [name]
  (tools/parse (str "test/resources/kouta/" name ".json")))

(defn mock-koodisto
  ([koodisto koodi-uri]
   (if koodi-uri
     { :koodiUri koodi-uri :nimi {:fi (str koodi-uri " nimi fi") :sv (str koodi-uri " nimi sv")}}))
  ([koodi-uri]
   (if koodi-uri
     (mock-koodisto (subs koodi-uri 0 (clojure.string/index-of koodi-uri "_")) koodi-uri))))

(defn mock-toteutukset
  ([oid]
   (json (str oid "-toteutukset")))
  ([oid vainJulkaistut]
   (if vainJulkaistut
     (filter #(= (:tila %) "julkaistu") (mock-toteutukset oid))
     (mock-toteutukset oid))))

(defn no-timestamp
  [json]
  (dissoc json :timestamp))

(defn read
  [index id]
  (get-by-id index index id))

(defn debug-pretty
  [json]
  (println (cheshire/generate-string json {:pretty true})))

(against-background
  [(before :contents (init-elastic-test))
   (after :facts (tools/reset-kouta-test-data))
   (after :contents (stop-elastic-test))]

  (with-redefs [common/muokkaaja (fn [x] {:nimi "Kalle Ankka"})
                koodisto-service/get-koodi-nimi-with-cache mock-koodisto
                kouta-backend/get-toteutus-list-for-koulutus mock-toteutukset]
    (let [koulutus-oid "1.2.246.562.13.00000000000000000001"
          toteutus-oid "1.2.246.562.17.00000000000000000001"
          haku-oid "1.2.246.562.29.00000000000000000001"
          hakukohde-oid "1.2.246.562.20.00000000000000000001"
          valintaperuste-id "a5e88367-555b-4d9e-aa43-0904e5ea0a13"
          oids {:koulutukset [koulutus-oid]
                :toteutukset [toteutus-oid]
                :haut [haku-oid]
                :hakukohteet [hakukohde-oid]
                :valintaperusteet [valintaperuste-id]}]

      (fact "Indexer should index tallennettu koulutus only to koulutus index"
        (with-redefs [kouta-backend/get-koulutus (fn [oid] (merge (json oid) {:tila "tallennettu"}))]
            (i/index-koulutus koulutus-oid)
            (read search/index-name koulutus-oid) => nil
            (no-timestamp (read koulutus/index-name koulutus-oid)) => (no-timestamp (merge (json "kouta-koulutus-result") {:tila "tallennettu"}))))

      (with-redefs [kouta-backend/list-koulutukset-by-haku (fn [oid] (json (str oid "-koulutukset")))
                    kouta-backend/get-hakukohde (fn [oid] (json oid))
                    kouta-backend/get-valintaperuste (fn [oid] (json oid))
                    kouta-backend/get-haku (fn [oid] (json oid))
                    kouta-backend/list-hakukohteet-by-haku (fn [oid] (json (str oid "-hakukohteet")))
                    kouta-backend/get-toteutus (fn [oid] (json oid))
                    kouta-backend/list-haut-by-toteutus (fn [oid] (json (str oid "-haut")))
                    kouta-backend/get-koulutus (fn [oid] (merge (json oid) {:tila "julkaistu"}))
                    kouta-backend/get-hakutiedot-for-koulutus (fn [oid] (json (str oid "-hakutiedot")))
                    kouta-backend/get-last-modified (fn [s] oids)]

      (fact "Indexer should index julkaistu koulutus also to search index"
            (i/index-koulutus koulutus-oid)
            (no-timestamp (read search/index-name koulutus-oid)) => (no-timestamp (json "kouta-koulutus-search-result"))
            (no-timestamp (read koulutus/index-name koulutus-oid)) => (no-timestamp (merge (json "kouta-koulutus-result") {:tila "julkaistu"})))

      (fact "Indexer should index toteutus to toteutus index and update related indexes"
          (i/index-toteutus toteutus-oid)
          (no-timestamp (read toteutus/index-name toteutus-oid)) => (no-timestamp (json "kouta-toteutus-result"))
          (:oid (read search/index-name koulutus-oid)) => koulutus-oid
          (:oid (read koulutus/index-name koulutus-oid)) => koulutus-oid)

      (fact "Indexer should index haku to haku index and update related indexes"
          (i/index-haku haku-oid)
          (no-timestamp (read haku/index-name haku-oid)) => (no-timestamp (json "kouta-haku-result"))
          (:oid (read toteutus/index-name toteutus-oid)) => toteutus-oid
          (:oid (read search/index-name koulutus-oid)) => koulutus-oid
          (read koulutus/index-name koulutus-oid) => nil)

      (fact "Indexer should index hakukohde to hakukohde index and update related indexes"
          (i/index-hakukohde hakukohde-oid)
          (no-timestamp (read hakukohde/index-name hakukohde-oid)) => (no-timestamp (json "kouta-hakukohde-result"))
          (:oid (read haku/index-name haku-oid)) => haku-oid
          (:oid (read toteutus/index-name toteutus-oid)) => nil
          (:oid (read search/index-name koulutus-oid)) => koulutus-oid
          (read koulutus/index-name koulutus-oid) => nil)

      (fact "Indexer should index valintaperuste to valintaperuste index"
        (i/index-valintaperuste valintaperuste-id)
        (no-timestamp (read valintaperuste/index-name valintaperuste-id)) => (no-timestamp (json "kouta-valintaperuste-result")))

      (fact "Indexer should index all"
        (i/index-all)
        (:oid (read haku/index-name haku-oid)) => haku-oid
        (:oid (read hakukohde/index-name hakukohde-oid)) => hakukohde-oid
        (:oid (read toteutus/index-name toteutus-oid)) => toteutus-oid
        (:oid (read koulutus/index-name koulutus-oid)) => koulutus-oid
        (:oid (read search/index-name koulutus-oid)) => koulutus-oid
        (:id (read valintaperuste/index-name valintaperuste-id)) => valintaperuste-id)

      (fact "Indexer should index changed oids"
        (i/index-oids {:hakukohteet [hakukohde-oid]})
        (:oid (read haku/index-name haku-oid)) => haku-oid
        (:oid (read hakukohde/index-name hakukohde-oid)) => hakukohde-oid
        (:oid (read toteutus/index-name toteutus-oid)) => nil
        (:oid (read koulutus/index-name koulutus-oid)) => nil
        (:oid (read search/index-name koulutus-oid)) => koulutus-oid
        (:id (read valintaperuste/index-name valintaperuste-id)) => nil)))))