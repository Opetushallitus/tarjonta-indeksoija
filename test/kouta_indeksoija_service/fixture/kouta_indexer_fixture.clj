(ns kouta-indeksoija-service.fixture.kouta-indexer-fixture
  (:require [kouta-indeksoija-service.elastic.admin :as admin]
            [kouta-indeksoija-service.indexer.indexer :as indexer]
            [kouta-indeksoija-service.elastic.tools :as tools]
            [kouta-indeksoija-service.fixture.external-services :refer :all]
            [mocks.notifier-target-mock]
            [clojure.test :refer :all]
            [cheshire.core :refer [parse-string]]
            [clojure.walk :refer [keywordize-keys stringify-keys]])
  (:import (fi.oph.kouta.external KoutaFixtureTool$)))

(defonce KoutaFixture KoutaFixtureTool$/MODULE$)

(defn ->keywordized-json
  [string]
  (keywordize-keys (parse-string string)))

(defn ->java-map
  [clj-map]
  (java.util.HashMap. (stringify-keys clj-map)))

(defn ->clj-map
  [java-map]
  (keywordize-keys (merge {} java-map)))

(defonce default-koulutus-map (->clj-map (.DefaultKoulutus KoutaFixture)))
(defonce default-toteutus-map (->clj-map (.DefaultToteutus KoutaFixture)))
(defonce default-haku-map (->clj-map (.DefaultHaku KoutaFixture)))
(defonce default-hakukohde-map (->clj-map (.DefaultHakukohde KoutaFixture)))
(defonce default-valintaperuste-map (->clj-map (.DefaultValintaperuste KoutaFixture)))
(defonce default-sorakuvaus-map (->clj-map (.DefaultSorakuvaus KoutaFixture)))
(defonce default-oppilaitos-map (->clj-map (.DefaultOppilaitos KoutaFixture)))
(defonce default-oppilaitoksen-osa-map (->clj-map (.DefaultOppilaitoksenOsa KoutaFixture)))

(defn add-koulutus-mock
  [oid & {:as params}]
  (let [koulutus (merge default-koulutus-map {:organisaatio Oppilaitos1} params)]
    (.addKoulutus KoutaFixture oid (->java-map koulutus))))

(defn update-koulutus-mock
  [oid & {:as params}]
  (.updateKoulutus KoutaFixture oid (->java-map params)))

(defn mock-get-koulutus
  [oid]
  (locking KoutaFixture
    (->keywordized-json (.getKoulutus KoutaFixture oid))))

(defn add-toteutus-mock
  [oid koulutusOid & {:as params}]
  (let [toteutus (merge default-toteutus-map {:organisaatio Oppilaitos1} params {:koulutusOid koulutusOid})]
    (.addToteutus KoutaFixture oid (->java-map toteutus))))

(defn update-toteutus-mock
  [oid & {:as params}]
  (.updateToteutus KoutaFixture oid (->java-map params)))

(defn mock-get-toteutus
  [oid]
  (locking KoutaFixture
    (->keywordized-json (.getToteutus KoutaFixture oid))))

(defn mock-get-toteutukset
  ([koulutusOid vainJulkaistut]
   (comment let [pred (fn [e] (and (= (:koulutusOid (val e)) koulutusOid) (or (not vainJulkaistut) (= (:tila (val e)) "julkaistu"))))
         mapper (fn [e] (->toteutus-mock-response (name (key e)) koulutusOid (val e)))]
     (map mapper (find-from-atom toteutukset pred)))
   (locking KoutaFixture
     (->keywordized-json (.getToteutuksetByKoulutus KoutaFixture koulutusOid vainJulkaistut))))
  ([koulutusOid]
   (mock-get-toteutukset koulutusOid false)))

(defn add-haku-mock
  [oid & {:as params}]
  (let [haku (merge default-haku-map {:organisaatio Oppilaitos1} params)]
    (.addHaku KoutaFixture oid (->java-map haku))))

(defn update-haku-mock
  [oid & {:as params}]
  (.updateHaku KoutaFixture oid (->java-map params)))

(defn mock-get-haku
  [oid]
  (locking KoutaFixture
    (->keywordized-json (.getHaku KoutaFixture oid))))

(defn add-hakukohde-mock
  [oid toteutusOid hakuOid & {:as params}]
  (let [hakukohde (merge default-hakukohde-map {:organisaatio Oppilaitos1} params {:hakuOid hakuOid :toteutusOid toteutusOid})]
    (.addHakukohde KoutaFixture oid (->java-map hakukohde))))

(defn update-hakukohde-mock
  [oid & {:as params}]
  (.updateHakukohde KoutaFixture oid (->java-map params)))

(defn mock-get-hakukohde
  [oid]
  (locking KoutaFixture
    (->keywordized-json (.getHakukohde KoutaFixture oid))))

(defn add-valintaperuste-mock
  [id & {:as params}]
  (let [valintaperuste (merge default-valintaperuste-map {:organisaatio Oppilaitos1} params)]
    (.addValintaperuste KoutaFixture id (->java-map valintaperuste))))

(defn update-valintaperuste-mock
  [id & {:as params}]
  (.updateValintaperuste KoutaFixture id (->java-map params)))

(defn mock-get-valintaperuste
  [id]
  (locking KoutaFixture
    (->keywordized-json (.getValintaperuste KoutaFixture id))))

(defn add-sorakuvaus-mock
  [id & {:as params}]
  (let [sorakuvaus (merge default-sorakuvaus-map {:organisaatio Oppilaitos1} params)]
    (.addSorakuvaus KoutaFixture id (->java-map sorakuvaus))))

(defn update-sorakuvaus-mock
  [id & {:as params}]
  (.updateSorakuvaus KoutaFixture id (->java-map params)))

(defn add-oppilaitos-mock
  [oid & {:as params}]
  (let [oppilaitos (merge default-oppilaitos-map {:organisaatio Oppilaitos1} params)]
    (.addOppilaitos KoutaFixture oid (->java-map oppilaitos))))

(defn update-oppilaitos-mock
  [oid & {:as params}]
  (.updateOppilaitos KoutaFixture oid (->java-map params)))

(defn mock-get-oppilaitos
  [oid]
  (locking KoutaFixture
    (->keywordized-json (.getOppilaitos KoutaFixture oid))))

(defn add-oppilaitoksen-osa-mock
  [oid oppilaitosOid & {:as params}]
  (let [oppilaitosen-osa (merge default-oppilaitoksen-osa-map {:organisaatio Oppilaitos1} params {:oppilaitosOid oppilaitosOid})]
    (.addOppilaitoksenOsa KoutaFixture oid (->java-map oppilaitosen-osa))))

(defn update-oppilaitoksen-osa-mock
  [oid & {:as params}]
  (.updateOppilaitoksenOsa KoutaFixture oid (->java-map params)))

(defn mock-get-oppilaitoksen-osa
  [oid]
  (locking KoutaFixture
    (->keywordized-json (.getOppilaitoksenOsa KoutaFixture oid))))

(defn mock-get-sorakuvaus
  [id]
  (locking KoutaFixture
    (->keywordized-json (.getSorakuvaus KoutaFixture id))))

(defn mock-get-hakukohteet-by-haku
  [hakuOid]
  (locking KoutaFixture
    (->keywordized-json (.listHakukohteetByHaku KoutaFixture hakuOid))))

(defn mock-list-haut-by-toteutus
  [toteutusOid]
  (locking KoutaFixture
    (->keywordized-json (.listHautByToteutus KoutaFixture toteutusOid))))

(defn mock-list-hakukohteet-by-toteutus
  [toteutusOid]
  (locking KoutaFixture
    (->keywordized-json (.listHakukohteetByToteutus KoutaFixture toteutusOid))))

(defn mock-list-hakukohteet-by-valintaperuste
  [valintaperusteId]
  (locking KoutaFixture
    (->keywordized-json (.listHakukohteetByValintaperuste KoutaFixture valintaperusteId))))

(defn mock-list-koulutukset-by-haku
  [hakuOid]
  (locking KoutaFixture
    (->keywordized-json (.listKoulutuksetByHaku KoutaFixture hakuOid))))

(defn mock-list-toteutukset-by-haku
  [hakuOid]
  (locking KoutaFixture
    (->keywordized-json (.listToteutuksetByHaku KoutaFixture hakuOid))))

(defn mock-get-hakutiedot-for-koulutus
  [oid]
  (locking KoutaFixture
    (->keywordized-json (.getHakutiedotByKoulutus KoutaFixture oid))))

(defn mock-list-valintaperusteet-by-sorakuvaus
  [sorakuvausId]
  (locking KoutaFixture
    (->keywordized-json (.listValintaperusteetBySorakuvaus KoutaFixture sorakuvausId))))

(defn mock-get-oppilaitoksen-osat-by-oppilaitos
  [oppilaitosOid]
  (locking KoutaFixture
    (->keywordized-json (.getOppilaitostenOsatByOppilaitos KoutaFixture oppilaitosOid))))

(defn mock-get-last-modified
  [since]
  (locking KoutaFixture
    (->keywordized-json (.getLastModified KoutaFixture since))))

(defn reset-indices
  []
  (tools/delete-index kouta-indeksoija-service.indexer.kouta.koulutus/index-name)
  (tools/delete-index kouta-indeksoija-service.indexer.kouta.toteutus/index-name)
  (tools/delete-index kouta-indeksoija-service.indexer.kouta.haku/index-name)
  (tools/delete-index kouta-indeksoija-service.indexer.kouta.hakukohde/index-name)
  (tools/delete-index kouta-indeksoija-service.indexer.kouta.valintaperuste/index-name)
  (tools/delete-index kouta-indeksoija-service.indexer.kouta.koulutus-search/index-name)
  (tools/delete-index kouta-indeksoija-service.indexer.kouta.oppilaitos/index-name))

(defn refresh-indices
  []
  (tools/refresh-index kouta-indeksoija-service.indexer.kouta.koulutus/index-name)
  (tools/refresh-index kouta-indeksoija-service.indexer.kouta.toteutus/index-name)
  (tools/refresh-index kouta-indeksoija-service.indexer.kouta.haku/index-name)
  (tools/refresh-index kouta-indeksoija-service.indexer.kouta.hakukohde/index-name)
  (tools/refresh-index kouta-indeksoija-service.indexer.kouta.valintaperuste/index-name)
  (tools/refresh-index kouta-indeksoija-service.indexer.kouta.koulutus-search/index-name)
  (tools/refresh-index kouta-indeksoija-service.indexer.kouta.oppilaitos/index-name))

(defn reset-mocks
  []
  (.reset KoutaFixture))

(defn init
  []
  (admin/initialize-indices))

(defn teardown
  []
  (reset-mocks)
  (reset-indices))

(defn mock-indexing-fixture [test]
  (init)
  (test)
  (teardown))

(defmacro with-mocked-indexing
  [& body]
  ;TODO: with-redefs is not thread safe and may cause unexpected behaviour.
  ;It can be temporarily fixed by using locked in mocking functions, but better solution would be superb!
  `(with-redefs [kouta-indeksoija-service.rest.kouta/get-koulutus
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-get-koulutus

                 kouta-indeksoija-service.rest.kouta/get-toteutus-list-for-koulutus
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-get-toteutukset

                 kouta-indeksoija-service.rest.kouta/get-toteutus
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-get-toteutus

                 kouta-indeksoija-service.rest.kouta/get-haku
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-get-haku

                 kouta-indeksoija-service.rest.kouta/list-hakukohteet-by-haku
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-get-hakukohteet-by-haku

                 kouta-indeksoija-service.rest.kouta/get-hakukohde
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-get-hakukohde

                 kouta-indeksoija-service.rest.kouta/get-valintaperuste
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-get-valintaperuste

                 kouta-indeksoija-service.rest.kouta/get-sorakuvaus
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-get-sorakuvaus

                 kouta-indeksoija-service.rest.kouta/get-oppilaitos
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-get-oppilaitos

                 kouta-indeksoija-service.rest.kouta/get-hakutiedot-for-koulutus
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-get-hakutiedot-for-koulutus

                 kouta-indeksoija-service.rest.kouta/list-haut-by-toteutus
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-list-haut-by-toteutus

                 kouta-indeksoija-service.rest.kouta/list-hakukohteet-by-toteutus
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-list-hakukohteet-by-toteutus

                 kouta-indeksoija-service.rest.kouta/list-hakukohteet-by-valintaperuste
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-list-hakukohteet-by-valintaperuste

                 kouta-indeksoija-service.rest.kouta/list-koulutukset-by-haku
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-list-koulutukset-by-haku

                 kouta-indeksoija-service.rest.kouta/list-toteutukset-by-haku
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-list-toteutukset-by-haku

                 kouta-indeksoija-service.rest.kouta/list-valintaperusteet-by-sorakuvaus
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-list-valintaperusteet-by-sorakuvaus

                 kouta-indeksoija-service.rest.kouta/get-oppilaitoksen-osat
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-get-oppilaitoksen-osat-by-oppilaitos

                 kouta-indeksoija-service.rest.kouta/get-last-modified
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-get-last-modified

                 kouta-indeksoija-service.rest.organisaatio/get-by-oid
                 kouta-indeksoija-service.fixture.external-services/mock-organisaatio

                 kouta-indeksoija-service.rest.organisaatio/get-hierarkia-v4
                 kouta-indeksoija-service.fixture.external-services/mock-organisaatio-hierarkia

                 kouta-indeksoija-service.rest.koodisto/get-koodi-nimi-with-cache
                 kouta-indeksoija-service.fixture.external-services/mock-koodisto

                 kouta-indeksoija-service.indexer.kouta.common/muokkaaja
                 kouta-indeksoija-service.fixture.external-services/mock-muokkaaja

                 kouta-indeksoija-service.notifier.notifier/send-notifications
                 mocks.notifier-target-mock/add]
     (do ~@body)))

(defn index-oppilaitokset
  [oids]
  (with-mocked-indexing
   (indexer/index-oppilaitokset oids))
  (refresh-indices))

(defn index-oids-with-related-indices
  [oids]
  (with-mocked-indexing
   (indexer/index-oids oids))
  (refresh-indices))

(defn index-oids-without-related-indices
  [oids]
  (with-mocked-indexing
    (with-redefs [kouta-indeksoija-service.rest.kouta/get-last-modified (fn [x] oids)]
      (indexer/index-all-kouta)))
  (refresh-indices))

(defn index-all
  []
  (with-mocked-indexing
   (indexer/index-all-kouta))
  (refresh-indices))
