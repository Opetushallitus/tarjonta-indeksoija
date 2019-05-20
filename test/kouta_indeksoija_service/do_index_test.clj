(ns kouta-indeksoija-service.do-index-test
  (:require [midje.sweet :refer :all]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [clj-test-utils.s3-mock-utils :refer :all]
            [kouta-indeksoija-service.test-tools :as tools :refer [reset-test-data block-until-indexed]]
            [kouta-indeksoija-service.elastic.queue :as q]
            [kouta-indeksoija-service.elastic.docs :as d]
            [kouta-indeksoija-service.indexer.index :as i]
            [mocks.externals-mock :as mock]
            [clj-s3.s3-connect :as s3]
            [kouta-indeksoija-service.rest.organisaatio :as o]
            [kouta-indeksoija-service.rest.eperuste :as e]
            [kouta-indeksoija-service.rest.tarjonta :as t]
            [kouta-indeksoija-service.util.conf :refer [env]]))

(defn setup-queue [type oid]
             (q/upsert-to-queue [{:type type :oid oid}])
             (block-until-indexed 5000)
             (q/get-queue))

(defn empty-s3 []
  (let [keys (s3/list-keys)]
    (if (not (empty? keys)) (s3/delete keys))))

(against-background
  [(before :contents (do (init-elastic-test) (init-s3-mock)))
   (after :facts (do (reset-test-data) (empty-s3)))
   (after :contents (do (stop-elastic-test) (stop-s3-mock)))]

  (with-redefs [env {:s3-dev-disabled "false"}
                o/get-doc mock/get-doc
                e/get-doc mock/get-doc
                t/get-doc mock/get-doc
                t/get-pic mock/get-pic]
    (fact "index organisaatio"
      (let [oid "1.2.246.562.10.39920288212"]
        (setup-queue "organisaatio" oid)
        (i/do-index)
        (let [indexed-org (d/get-organisaatio oid)
              picture-list (s3/list-keys)]
          (count picture-list) => 1
          (first picture-list) => "organisaatio/1.2.246.562.10.39920288212/1.2.246.562.10.39920288212.jpg"
          (get-in indexed-org [:searchData :tyyppi]) => "muu")))

    (comment fact "index haku"
       (let [oid "1.2.246.562.29.86197271827"]
         (setup-queue "haku" oid)
         (i/do-index)
         (let [indexed-haku (d/get-haku oid)
               picture-list (s3/list-keys)]
           (count picture-list) => 0
           (get indexed-haku :oid) => "1.2.246.562.29.86197271827")))

    (comment fact "index hakukohde"
      (let [oid "1.2.246.562.20.17663370199"]
        (setup-queue "hakukohde" oid)
        (i/do-index)
        (let [indexed-hakukohde (d/get-hakukohde oid)
              picture-list (s3/list-keys)]
          (count picture-list) => 0
          (get (first (get indexed-hakukohde :koulutusmoduuliToteutusTarjoajatiedot)) :koulutus) => "1.2.246.562.17.53874141319")))

    (comment fact "index koulutusmoduuli"
      (let [oid "1.2.246.562.13.39326629852"]
        (setup-queue "koulutusmoduuli" oid)
        (i/do-index)
        (let [indexed-koulutusmoduuli (d/get-koulutusmoduuli oid)
              picture-list (s3/list-keys)]
          (count picture-list) => 0
          (get-in indexed-koulutusmoduuli [:searchData :tyyppi]) => "kk")))

    (comment fact "index koulutus"
      (let [oid "1.2.246.562.17.53874141319"]
        (setup-queue "koulutus" oid)
        (i/do-index)
        (let [indexed-koulutus (d/get-koulutus-with-searh-data oid)
              picture-list (s3/list-keys)]
          (count picture-list) => 3
          (get-in indexed-koulutus [:searchData :tyyppi]) => "kk")))

    (fact "index eperuste"
      (let [oid "3397334"]
        (setup-queue "eperuste" oid)
        (i/do-index)
        (let [indexed-eperuste (d/get-eperuste oid)
              picture-list (s3/list-keys)]
          (count picture-list) => 0
          (get indexed-eperuste :koulutustyyppi) => "koulutustyyppi_12")))))

