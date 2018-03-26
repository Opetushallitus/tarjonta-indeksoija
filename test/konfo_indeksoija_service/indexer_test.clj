(ns konfo-indeksoija-service.indexer-test
  (:require [konfo-indeksoija-service.indexer :as indexer]
            [konfo-indeksoija-service.elastic-client :as elastic-client]
            [konfo-indeksoija-service.test-tools :as tools :refer [reset-test-data]]
            [mocks.externals-mock :refer [with-externals-mock]]
            [midje.sweet :refer :all]))

(against-background
  [(after :facts (reset-test-data))
   (after :contents (reset-test-data))]

  (fact "Indexer should save hakukohde"
    (let [oid "1.2.246.562.20.99178639649"]
      (with-externals-mock
        (indexer/index-objects [(indexer/get-coverted-doc {:oid oid :type "hakukohde"})]))
      (elastic-client/get-hakukohde oid) => (contains {:oid oid})))

  (fact "Indexer should save koulutus"
    (let [oid "1.2.246.562.17.81687174185"]
      (with-externals-mock
        (indexer/index-objects [(indexer/get-coverted-doc {:oid oid :type "koulutus"})]))
      (let [indexed-koulutus (elastic-client/get-koulutus oid)]
        indexed-koulutus => (contains {:oid oid})
        (get-in indexed-koulutus [:koulutuskoodi :uri]) => "koulutus_371101"
        (get-in indexed-koulutus [:valmistavaKoulutus :kuvaus :SISALTO :kieli_fi])
        => "<p><a href=\"http://www.hyria.fi/koulutukset/aikuiskoulutukset/koulutushaku?e=3807&amp;i=3061\">Tutustu tästä tarkemmin koulutuksen sisältöön.</a></p> <p> </p> <p>Oppisopimuskoulutus mahdollinen</p>")))

  (fact "Indexer should save organisaatio"
    (let [oid "1.2.246.562.10.39920288212"]
      (with-externals-mock
        (indexer/index-objects [(indexer/get-coverted-doc {:oid oid :type "organisaatio"})]))
      (elastic-client/get-organisaatio oid) => (contains {:oid oid})))

  (fact "Indexer should start scheduled indexing and index objects"
    (let [hk1-oid "1.2.246.562.20.99178639649"
          hk2-oid "1.2.246.562.20.28810946823"
          k1-oid "1.2.246.562.17.81687174185"
          oids [hk1-oid hk2-oid k1-oid]]
      (with-externals-mock
        (indexer/start-indexer-job)
        (map #(elastic-client/get-hakukohde %) [hk1-oid hk2-oid]) => [nil nil]
        (elastic-client/get-koulutus k1-oid) => nil

        (elastic-client/upsert-indexdata [{:oid hk1-oid :type "hakukohde"}
                                          {:oid hk2-oid :type "hakukohde"}
                                          {:oid k1-oid :type "koulutus"}])
        (tools/block-until-indexed 10000)
        (tools/refresh-and-wait "hakukohde" 1000)
        (let [hk1-res (elastic-client/get-hakukohde hk1-oid)
              hk2-res (elastic-client/get-hakukohde hk2-oid)
              k1-res (elastic-client/get-koulutus k1-oid)]
          hk1-res => (contains {:oid hk1-oid})
          hk2-res => (contains {:oid hk2-oid})
          k1-res => (contains {:oid k1-oid})

          (elastic-client/upsert-indexdata [{:oid hk1-oid :type "hakukohde"}
                                            {:oid hk2-oid :type "hakukohde"}
                                            {:oid k1-oid :type "koulutus"}])
          (tools/block-until-indexed 10000)
          (tools/refresh-and-wait "hakukohde" 1000)
          (< (:timestamp hk1-res) (:timestamp (elastic-client/get-hakukohde hk1-oid))) => true
          (< (:timestamp hk2-res) (:timestamp (elastic-client/get-hakukohde hk2-oid))) => true
          (< (:timestamp k1-res) (:timestamp (elastic-client/get-koulutus k1-oid))) => true))))

  (fact "should index from tarjonta latest"
    (reset-test-data)
    (let [hk1-oid "1.2.246.562.20.99178639649"
          k1-oid "1.2.246.562.17.81687174185"]
      (with-externals-mock
        ;; Do this to activate mock!
        ;; TODO: could this be done in a smarter way?
        (elastic-client/set-last-index-time 0)
        (indexer/start-indexer-job)
        (tools/block-until-latest-in-queue 10000)
        (tools/block-until-indexed 10000)
        (tools/refresh-and-wait "hakukohde" 1000)
        (let [hk1-res (elastic-client/get-hakukohde hk1-oid)
              k1-res (elastic-client/get-koulutus k1-oid)]
          hk1-res => (contains {:oid hk1-oid})
          k1-res => (contains {:oid k1-oid})
          (< 0 (elastic-client/get-last-index-time)) => true)))))