(ns konfo-indeksoija-service.do-index-test
  (:require [midje.sweet :refer :all]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [clj-test-utils.s3-mock-utils :refer :all]
            [konfo-indeksoija-service.test-tools :as tools :refer [reset-test-data block-until-indexed]]
            [konfo-indeksoija-service.elastic.elastic-client :as e]
            [konfo-indeksoija-service.indexer.index :as i]
            [mocks.externals-mock :as mock]
            [clj-s3.s3-connect :as s3]
            [konfo-indeksoija-service.rest.organisaatio :as o]
            [konfo-indeksoija-service.rest.tarjonta :as t]
            [konfo-indeksoija-service.util.conf :refer [env]]))

(defn setup-queue [type oid]
             (e/upsert-indexdata [{:type type :oid oid}])
             (block-until-indexed 5000)
             (e/get-queue))

(defn empty-s3 []
  (let [keys (s3/list-keys)]
    (if (not (empty? keys)) (s3/delete keys))))



(against-background
  [(before :contents (do (init-elastic-test) (init-s3-mock)))
   (after :facts (do (reset-test-data) (empty-s3)))
   (after :contents (do (stop-elastic-test) (stop-s3-mock)))]

  (with-redefs [env {:s3-dev-disabled "false"}
                o/get-doc mock/get-doc
                t/get-doc mock/get-doc
                t/get-pic mock/get-pic]
    (fact "index organisaatio"
      (let [oid "1.2.246.562.10.39920288212"]
        (setup-queue "organisaatio" oid)
        (i/do-index)
        (let [indexed-org (e/get-organisaatio oid)
              picture-list (s3/list-keys)]
          (count picture-list) => 1
          (first picture-list) => "organisaatio/1.2.246.562.10.39920288212/1.2.246.562.10.39920288212.jpg"
          (get-in indexed-org [:searchData :tyyppi]) => "muu")))

    (fact "index haku"
       (let [oid "1.2.246.562.29.86197271827"]
         (setup-queue "haku" oid)
         (i/do-index)
         (let [indexed-haku (e/get-haku oid)
               picture-list (s3/list-keys)]
           (count picture-list) => 0
           (get indexed-haku :oid) => "1.2.246.562.29.86197271827")))

    (fact "index hakukohde"
      (let [oid "1.2.246.562.20.17663370199"]
        (setup-queue "hakukohde" oid)
        (i/do-index)
        (let [indexed-hakukohde (e/get-hakukohde oid)
              picture-list (s3/list-keys)]
          (count picture-list) => 0
          (get (first (get indexed-hakukohde :koulutusmoduuliToteutusTarjoajatiedot)) :koulutus) => "1.2.246.562.17.53874141319")))

    (fact "index koulutusmoduuli"
      (let [oid "1.2.246.562.13.39326629852"]
        (setup-queue "koulutusmoduuli" oid)
        (i/do-index)
        (let [indexed-koulutusmoduuli (e/get-koulutusmoduuli oid)
              picture-list (s3/list-keys)]
          (count picture-list) => 0
          (get-in indexed-koulutusmoduuli [:searchData :tyyppi]) => "kk")))

    (fact "index koulutus"
      (let [oid "1.2.246.562.17.53874141319"]
        (setup-queue "koulutus" oid)
        (i/do-index)
        (let [indexed-koulutus (e/get-koulutus-with-searh-data oid)
              picture-list (s3/list-keys)]
          (count picture-list) => 3
          (get-in indexed-koulutus [:searchData :tyyppi]) => "kk")))))

