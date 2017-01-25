(ns tarjonta-indeksoija-service.indexer-test
  (:require [tarjonta-indeksoija-service.indexer :as indexer]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [mocks.tarjonta-mock :as mock]
            [midje.sweet :refer :all]))

(def index-identifier (.getTime (java.util.Date.)))

(defn index-name
  [base-name]
  (str base-name "_" index-identifier))

(def hakukohde-index (index-name "hakukohde_test"))
(def koulutus-index (index-name "koulutus_test"))
(def indexdata (index-name "index-data"))

(against-background
  [(after :facts [(elastic-client/delete-index hakukohde-index)
                  (elastic-client/delete-index koulutus-index)])
   (after :contents [(indexer/reset-jobs)])]

  (fact "Indexer should save hakukohde"
    (let [oid "1.2.246.562.20.99178639649"]
      (mock/with-tarjonta-mock
        (indexer/index-object {:oid oid :type hakukohde-index}))
      (elastic-client/get-by-id hakukohde-index hakukohde-index oid) => (contains {:oid oid})))

  (fact "Indexer should save koulutus"
    (let [oid "1.2.246.562.17.81687174185"]
      (mock/with-tarjonta-mock
        (indexer/index-object {:oid oid :type koulutus-index}))
      (let [indexed-koulutus (elastic-client/get-by-id koulutus-index koulutus-index oid)]
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
        (map #(elastic-client/get-by-id hakukohde-index hakukohde-index %) [hk1-oid hk2-oid]) => [nil nil]
        (elastic-client/get-by-id koulutus-index koulutus-index k1-oid) => nil

        ;; TODO: Make bulk upsert use indexdata_test index!!
        (elastic-client/bulk-upsert "indexdata" "indexdata" [{:oid hk1-oid :type hakukohde-index}
                                                             {:oid hk2-oid :type hakukohde-index}
                                                             {:oid k1-oid :type koulutus-index}])
        (Thread/sleep 5000) ;; TODO: Be smarter about this also...
        (let [hk1-res (elastic-client/get-by-id hakukohde-index hakukohde-index hk1-oid)
              hk2-res (elastic-client/get-by-id hakukohde-index hakukohde-index hk2-oid)
              k1-res (elastic-client/get-by-id koulutus-index koulutus-index k1-oid)]
          hk1-res => (contains {:oid hk1-oid})
          hk2-res => (contains {:oid hk2-oid})
          k1-res => (contains {:oid k1-oid})

          (elastic-client/bulk-upsert "indexdata" "indexdata" [{:oid hk1-oid :type hakukohde-index}
                                                               {:oid hk2-oid :type hakukohde-index}
                                                               {:oid k1-oid :type koulutus-index}])
          (Thread/sleep 5000) ;; TODO: Be smarter about this also...
          (< (:timestamp hk1-res) (:timestamp (elastic-client/get-by-id hakukohde-index hakukohde-index hk1-oid))) => true
          (< (:timestamp hk2-res) (:timestamp (elastic-client/get-by-id hakukohde-index hakukohde-index hk2-oid))) => true
          (< (:timestamp k1-res) (:timestamp (elastic-client/get-by-id koulutus-index koulutus-index k1-oid))) => true)))))