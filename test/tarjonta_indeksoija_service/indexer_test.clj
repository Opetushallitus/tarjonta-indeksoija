(ns tarjonta-indeksoija-service.indexer-test
  (:require [tarjonta-indeksoija-service.indexer :as indexer]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [mocks.tarjonta-mock :as mock]
            [midje.sweet :refer :all]))

(defn block-until-indexed
  []
  (elastic-client/refresh-index "indexdata_test")
  (while (not (empty? (elastic-client/get-queue)))
    (Thread/sleep 1000)))

(against-background
  [(after :facts [(elastic-client/delete-index "hakukohde_test")
                  (elastic-client/delete-index "koulutus_test")
                  (elastic-client/delete-index "indexdata_test")])
   (after :contents [(indexer/reset-jobs)])]

  (fact "Indexer should save hakukohde"
    (let [oid "1.2.246.562.20.99178639649"]
      (mock/with-tarjonta-mock
        (indexer/index-object {:oid oid :type "hakukohde_test"}))
      (elastic-client/get-by-id "hakukohde_test" "hakukohde_test" oid) => (contains {:oid oid})))

  (fact "Indexer should save koulutus"
    (let [oid "1.2.246.562.17.81687174185"]
      (mock/with-tarjonta-mock
        (indexer/index-object {:oid oid :type "koulutus_test"}))
      (let [indexed-koulutus (elastic-client/get-by-id "koulutus_test" "koulutus_test" oid)]
        indexed-koulutus => (contains {:oid oid})
        (get-in indexed-koulutus [:koulutuskoodi :uri]) => "koulutus_371101"
        (get-in indexed-koulutus [:valmistavaKoulutus :kuvaus :SISALTO :kieli_fi])
          => "<p><a href=\"http://www.hyria.fi/koulutukset/aikuiskoulutukset/koulutushaku?e=3807&amp;i=3061\">Tutustu tästä tarkemmin koulutuksen sisältöön.</a></p> <p> </p> <p>Oppisopimuskoulutus mahdollinen</p>")))

  (fact "Indexer should start scheduled indexing and index objects"
    (let [hk1-oid "1.2.246.562.20.99178639649"
          hk2-oid "1.2.246.562.20.28810946823"
          k1-oid "1.2.246.562.17.81687174185"
          oids [hk1-oid hk2-oid k1-oid]]
      (mock/with-tarjonta-mock
        (indexer/start-indexer-job)
        (map #(elastic-client/get-by-id "hakukohde_test" "hakukohde_test" %) [hk1-oid hk2-oid]) => [nil nil]
        (elastic-client/get-by-id "koulutus_test" "koulutus_test" k1-oid) => nil

        ;; TODO: Make bulk upsert use indexdata_test_test index!!
        (elastic-client/bulk-upsert "indexdata_test" "indexdata_test" [{:oid hk1-oid :type "hakukohde_test"}
                                                             {:oid hk2-oid :type "hakukohde_test"}
                                                             {:oid k1-oid :type "koulutus_test"}])
        (block-until-indexed)

        (let [hk1-res (elastic-client/get-by-id "hakukohde_test" "hakukohde_test" hk1-oid)
              hk2-res (elastic-client/get-by-id "hakukohde_test" "hakukohde_test" hk2-oid)
              k1-res (elastic-client/get-by-id "koulutus_test" "koulutus_test" k1-oid)]
          hk1-res => (contains {:oid hk1-oid})
          hk2-res => (contains {:oid hk2-oid})
          k1-res => (contains {:oid k1-oid})

          (elastic-client/bulk-upsert "indexdata_test" "indexdata_test" [{:oid hk1-oid :type "hakukohde_test"}
                                                               {:oid hk2-oid :type "hakukohde_test"}
                                                               {:oid k1-oid :type "koulutus_test"}])

          (block-until-indexed)

          (< (:timestamp hk1-res) (:timestamp (elastic-client/get-by-id "hakukohde_test" "hakukohde_test" hk1-oid))) => true
          (< (:timestamp hk2-res) (:timestamp (elastic-client/get-by-id "hakukohde_test" "hakukohde_test" hk2-oid))) => true
          (< (:timestamp k1-res) (:timestamp (elastic-client/get-by-id "koulutus_test" "koulutus_test" k1-oid))) => true)))))