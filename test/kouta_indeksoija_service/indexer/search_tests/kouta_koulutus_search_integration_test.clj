(ns kouta-indeksoija-service.indexer.search-tests.kouta-koulutus-search-integration-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.common-indexer-fixture :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.elastic.tools :refer [get-doc]]
            [kouta-indeksoija-service.indexer.kouta.koulutus-search :as koulutus-search]))

(use-fixtures :each fixture/indices-fixture)
(use-fixtures :each common-indexer-fixture)

(def agrologi-koulutuskoodi "koulutus_761101#1")
(def fysioterapeutti-koulutuskoodi "koulutus_671112#1")

(defn- mock-tutkintotyyppi
  [koulutus-koodi-uri]
  (cond
    (= koulutus-koodi-uri agrologi-koulutuskoodi) {:koodiUri "tutkintotyyppi_12"
                                                    :nimi {:fi "Ylempi ammattikorkeakoulututkinto" :sv "Högre yrkeshögskoleexaman"}}
    (= koulutus-koodi-uri fysioterapeutti-koulutuskoodi) {:koodiUri "tutkintotyyppi_06"
                                                           :nimi {:fi "Ammattikorkeakoulutus" :sv "Yrkeshögskoleutbildning"}}))

(defn- get-koulutustyypit
  [koulutus]
  (-> koulutus
      :hits
      (first) ;;Korkeakoulu koulutustyyppi päätellään ainoastaan koulutuksen perusteella joten kaikilla toteutuksilla on sama arvo ja voidaan ottaa first
      :koulutustyypit))

(deftest adds-ylempi-amk-koulutustyyppi
  (fixture/with-mocked-indexing
   (testing "Indexer should add ylempi-amk koulutustyyppi when tutkintotyyppi is ylempi ammattikorkeakoulu"
     (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/tutkintotyypit mock-tutkintotyyppi]
       (fixture/update-koulutus-mock koulutus-oid :koulutuksetKoodiUri agrologi-koulutuskoodi :koulutustyyppi "amk" :metadata fixture/amk-koulutus-metadata)
       (check-all-nil)
       (koulutus-search/do-index [koulutus-oid])
       (let [koulutus (get-doc koulutus-search/index-name koulutus-oid)
             koulutustyypit (get-koulutustyypit koulutus)]
         (is (= koulutustyypit ["amk" "ylempi-amk"])))))))

(deftest adds-alempi-amk-koulutustyyppi
  (fixture/with-mocked-indexing
   (testing "Indexer should add alempi-amk koulutustyyppi when tutkintotyyppi is ammattikorkeakoulu"
     (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/tutkintotyypit mock-tutkintotyyppi]
       (fixture/update-koulutus-mock koulutus-oid :koulutuksetKoodiUri fysioterapeutti-koulutuskoodi :koulutustyyppi "amk" :metadata fixture/amk-koulutus-metadata)
       (check-all-nil)
       (koulutus-search/do-index [koulutus-oid])
       (let [koulutus (get-doc koulutus-search/index-name koulutus-oid)
             koulutustyypit (get-koulutustyypit koulutus)]
         (is (= koulutustyypit ["amk" "alempi-amk"])))))))
