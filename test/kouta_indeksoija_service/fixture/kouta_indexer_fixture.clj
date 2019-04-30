(ns kouta-indeksoija-service.fixture.kouta-indexer-fixture
  (:require [kouta-indeksoija-service.elastic.admin :as admin]
            [kouta-indeksoija-service.kouta.indexer :as indexer]
            [kouta-indeksoija-service.elastic.tools :as tools]
            [kouta-indeksoija-service.fixture.external-services :refer :all]
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

(defn add-koulutus-mock
  [oid & {:as params}]
  (let [koulutus (merge default-koulutus-map {:organisaatio Oppilaitos1} params)]
    (.addKoulutus KoutaFixture oid (->java-map koulutus))))

(defn update-koulutus-mock
  [oid & {:as params}]
  (.updateKoulutus KoutaFixture oid (->java-map params)))

(defn mock-get-koulutus
  [oid]
  (->keywordized-json (.getKoulutus KoutaFixture oid)))

(defn add-toteutus-mock
  [oid koulutusOid & {:as params}]
  (let [toteutus (merge default-toteutus-map {:organisaatio Oppilaitos1} params {:koulutusOid koulutusOid})]
    (.addToteutus KoutaFixture oid (->java-map toteutus))))

(defn update-toteutus-mock
  [oid & {:as params}]
  (.updateToteutus KoutaFixture oid (->java-map params)))

(defn mock-get-toteutus
  [oid]
  (->keywordized-json (.getToteutus KoutaFixture oid)))

(defn mock-get-toteutukset
  ([koulutusOid vainJulkaistut]
   (comment let [pred (fn [e] (and (= (:koulutusOid (val e)) koulutusOid) (or (not vainJulkaistut) (= (:tila (val e)) "julkaistu"))))
         mapper (fn [e] (->toteutus-mock-response (name (key e)) koulutusOid (val e)))]
     (map mapper (find-from-atom toteutukset pred)))
   (->keywordized-json (.getToteutuksetByKoulutus KoutaFixture koulutusOid vainJulkaistut)))
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
  (->keywordized-json (.getHaku KoutaFixture oid)))

(defn add-hakukohde-mock
  [oid toteutusOid hakuOid & {:as params}]
  (let [hakukohde (merge default-hakukohde-map {:organisaatio Oppilaitos1} params {:hakuOid hakuOid :toteutusOid toteutusOid})]
    (.addHakukohde KoutaFixture oid (->java-map hakukohde))))

(defn update-hakukohde-mock
  [oid & {:as params}]
  (.updateHakukohde KoutaFixture oid (->java-map params)))

(defn mock-get-hakukohde
  [oid]
  (->keywordized-json (.getHakukohde KoutaFixture oid)))

(defn add-valintaperuste-mock
  [id & {:as params}]
  (let [valintaperuste (merge default-valintaperuste-map {:organisaatio Oppilaitos1} params)]
    (.addValintaperuste KoutaFixture id (->java-map valintaperuste))))

(defn update-valintaperuste-mock
  [id & {:as params}]
  (.updateValintaperuste KoutaFixture id (->java-map params)))

(defn mock-get-valintaperuste
  [id]
  (->keywordized-json (.getValintaperuste KoutaFixture id)))

(defn mock-get-hakukohteet-by-haku
  [hakuOid]
  (->keywordized-json (.listHakukohteetByHaku KoutaFixture hakuOid)))

(defn mock-list-haut-by-toteutus
  [toteutusOid]
  (->keywordized-json (.listHautByToteutus KoutaFixture toteutusOid)))

(defn mock-list-hakukohteet-by-toteutus
  [toteutusOid]
  (->keywordized-json (.listHakukohteetByToteutus KoutaFixture toteutusOid)))

(defn mock-list-koulutukset-by-haku
  [hakuOid]
  (->keywordized-json (.listKoulutuksetByHaku KoutaFixture hakuOid)))

(defn mock-get-hakutiedot-for-koulutus
  [oid]
  (->keywordized-json (.getHakutiedotByKoulutus KoutaFixture oid)))

(defn mock-get-last-modified
  [since]
  (->keywordized-json (.getLastModified KoutaFixture since)))

(defn reset-indices
  []
  (tools/delete-index kouta-indeksoija-service.kouta.koulutus/index-name)
  (tools/delete-index kouta-indeksoija-service.kouta.toteutus/index-name)
  (tools/delete-index kouta-indeksoija-service.kouta.haku/index-name)
  (tools/delete-index kouta-indeksoija-service.kouta.hakukohde/index-name)
  (tools/delete-index kouta-indeksoija-service.kouta.valintaperuste/index-name)
  (tools/delete-index kouta-indeksoija-service.kouta.koulutus-search/index-name))

(defn refresh-indices
  []
  (tools/refresh-index kouta-indeksoija-service.kouta.koulutus/index-name)
  (tools/refresh-index kouta-indeksoija-service.kouta.toteutus/index-name)
  (tools/refresh-index kouta-indeksoija-service.kouta.haku/index-name)
  (tools/refresh-index kouta-indeksoija-service.kouta.hakukohde/index-name)
  (tools/refresh-index kouta-indeksoija-service.kouta.valintaperuste/index-name)
  (tools/refresh-index kouta-indeksoija-service.kouta.koulutus-search/index-name))

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

                 kouta-indeksoija-service.rest.kouta/get-hakutiedot-for-koulutus
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-get-hakutiedot-for-koulutus

                 kouta-indeksoija-service.rest.kouta/list-haut-by-toteutus
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-list-haut-by-toteutus

                 kouta-indeksoija-service.rest.kouta/list-hakukohteet-by-toteutus
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-list-hakukohteet-by-toteutus

                 kouta-indeksoija-service.rest.kouta/list-koulutukset-by-haku
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-list-koulutukset-by-haku

                 kouta-indeksoija-service.rest.kouta/get-last-modified
                 kouta-indeksoija-service.fixture.kouta-indexer-fixture/mock-get-last-modified

                 kouta-indeksoija-service.rest.organisaatio/get-by-oid
                 kouta-indeksoija-service.fixture.external-services/mock-organisaatio

                 kouta-indeksoija-service.rest.koodisto/get-koodi-nimi-with-cache
                 kouta-indeksoija-service.fixture.external-services/mock-koodisto

                 kouta-indeksoija-service.kouta.common/muokkaaja
                 kouta-indeksoija-service.fixture.external-services/mock-muokkaaja]
     (do ~@body)))

(defn index-oids-with-related-indices
  [oids]
  (with-mocked-indexing
   (indexer/index-oids oids))
  (refresh-indices))

(defn index-oids-without-related-indices
  [oids]
  (with-mocked-indexing
    (with-redefs [kouta-indeksoija-service.rest.kouta/get-last-modified (fn [x] oids)]
      (indexer/index-all)))
  (refresh-indices))

(defn index-all
  []
  (with-mocked-indexing
   (indexer/index-all))
  (refresh-indices))
