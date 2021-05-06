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
(def elainlaaketietieen-kandi-koulutuskoodi "koulutus_672301#1")
(def liikuntakasvatuksen-kandi-koulutuskoodi "koulutus_682251#1")
(def arkkitehti-koulutuskoodi "koulutus_754101#1")
(def fil-maist-kemia-koulutuskoodi "koulutus_742401#1")
(def farmasian-tohtori-koulutuskoodi "koulutus_875401#1")
(def fil-tohtori-englannin-kieli-koulutuskoodi "koulutus_826103#1")
(def kandi-ja-maisteri-koulutuskoodi (str "koulutus_754101#1" "," "koulutus_672301#1" ))
(def kandi-maisteri-tohtori-koulutuskoodi (str "koulutus_754101#1" "," "koulutus_672301#1" "," "koulutus_875401#1"))

(def ylempi-amk-tutkintotyyppi {:koodiUri "tutkintotyyppi_12"
                                :nimi {:fi "Ylempi ammattikorkeakoulututkinto" :sv "Högre yrkeshögskoleexaman"}})
(def amk-tutkintotyyppi {:koodiUri "tutkintotyyppi_06"
                         :nimi {:fi "Ammattikorkeakoulutus" :sv "Yrkeshögskoleutbildning"}})
(def kandi-tutkintotyyppi {:koodiUri "tutkintotyyppi_13"
                           :nimi {:fi "Alempi korkeakoulututkinto" :sv "Lägre högskoleexamen"}})
(def maisteri-tutkintotyyppi {:koodiUri "tutkintotyyppi_14"
                              :nimi {:fi "Ylempi korkeakoulututkinto" :sv "Högre högskoleexamen"}})
(def tohtori-tutkintotyyppi {:koodiUri "tutkintotyyppi_16"
                             :nimi {:fi "Tohtorin tutkinto" :sv "Doktorsexamen"}})

(defn- mock-tutkintotyyppi
  [koulutus-koodi-uri]
  (cond
    (= koulutus-koodi-uri agrologi-koulutuskoodi) [ylempi-amk-tutkintotyyppi]
    (= koulutus-koodi-uri fysioterapeutti-koulutuskoodi) [amk-tutkintotyyppi]
    (= koulutus-koodi-uri elainlaaketietieen-kandi-koulutuskoodi) [kandi-tutkintotyyppi]
    (= koulutus-koodi-uri liikuntakasvatuksen-kandi-koulutuskoodi) [kandi-tutkintotyyppi]
    (= koulutus-koodi-uri arkkitehti-koulutuskoodi) [maisteri-tutkintotyyppi]
    (= koulutus-koodi-uri fil-maist-kemia-koulutuskoodi) [maisteri-tutkintotyyppi]
    (= koulutus-koodi-uri farmasian-tohtori-koulutuskoodi) [tohtori-tutkintotyyppi]
    (= koulutus-koodi-uri fil-tohtori-englannin-kieli-koulutuskoodi) [tohtori-tutkintotyyppi]))

(defn- get-koulutustyypit
  [koulutus]
  (-> koulutus
      :hits
      (first) ;;Korkeakoulu koulutustyyppi päätellään ainoastaan koulutuksen perusteella joten kaikilla toteutuksilla on sama arvo ja voidaan ottaa first
      :koulutustyypit))

(deftest adds-amk-ylempi-koulutustyyppi
  (fixture/with-mocked-indexing
   (testing "Indexer should add amk-ylempi koulutustyyppi when tutkintotyyppi is ylempi ammattikorkeakoulu"
     (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/tutkintotyypit mock-tutkintotyyppi]
       (fixture/update-koulutus-mock koulutus-oid :koulutuksetKoodiUri agrologi-koulutuskoodi :koulutustyyppi "amk" :metadata fixture/amk-koulutus-metadata)
       (check-all-nil)
       (koulutus-search/do-index [koulutus-oid])
       (let [koulutus (get-doc koulutus-search/index-name koulutus-oid)
             koulutustyypit (get-koulutustyypit koulutus)]
         (is (= koulutustyypit ["amk" "amk-ylempi" "korkeakoulutus"])))))))

(deftest adds-amk-alempi-koulutustyyppi
  (fixture/with-mocked-indexing
   (testing "Indexer should add amk-alempi koulutustyyppi when tutkintotyyppi is ammattikorkeakoulu"
     (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/tutkintotyypit mock-tutkintotyyppi]
       (fixture/update-koulutus-mock koulutus-oid :koulutuksetKoodiUri fysioterapeutti-koulutuskoodi :koulutustyyppi "amk" :metadata fixture/amk-koulutus-metadata)
       (check-all-nil)
       (koulutus-search/do-index [koulutus-oid])
       (let [koulutus (get-doc koulutus-search/index-name koulutus-oid)
             koulutustyypit (get-koulutustyypit koulutus)]
         (is (= koulutustyypit ["amk" "amk-alempi" "korkeakoulutus"])))))))

(deftest adds-kandi-koulutustyyppi
  (fixture/with-mocked-indexing
   (testing "Indexer should add kandi koulutustyyppi when tutkintotyyppi is alempi korkeakoulututkinto"
     (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/tutkintotyypit mock-tutkintotyyppi]
       (fixture/update-koulutus-mock koulutus-oid :koulutuksetKoodiUri elainlaaketietieen-kandi-koulutuskoodi :koulutustyyppi "yo" :metadata fixture/yo-koulutus-metadata)
       (check-all-nil)
       (koulutus-search/do-index [koulutus-oid])
       (let [koulutus (get-doc koulutus-search/index-name koulutus-oid)
             koulutustyypit (get-koulutustyypit koulutus)]
         (is (= koulutustyypit ["yo" "kandi" "korkeakoulutus"])))))))

(deftest adds-maisteri-koulutustyyppi
  (fixture/with-mocked-indexing
   (testing "Indexer should add maisteri koulutustyyppi when tutkintotyyppi is ylempi korkeakoulututkinto"
     (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/tutkintotyypit mock-tutkintotyyppi]
       (fixture/update-koulutus-mock koulutus-oid :koulutuksetKoodiUri arkkitehti-koulutuskoodi :koulutustyyppi "yo" :metadata fixture/yo-koulutus-metadata)
       (check-all-nil)
       (koulutus-search/do-index [koulutus-oid])
       (let [koulutus (get-doc koulutus-search/index-name koulutus-oid)
             koulutustyypit (get-koulutustyypit koulutus)]
         (is (= koulutustyypit ["yo" "maisteri" "korkeakoulutus"])))))))

(deftest adds-tohtori-koulutustyyppi
  (fixture/with-mocked-indexing
   (testing "Indexer should add tohtori koulutustyyppi when tutkintotyyppi is tohtorin tutkinto"
     (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/tutkintotyypit mock-tutkintotyyppi]
       (fixture/update-koulutus-mock koulutus-oid :koulutuksetKoodiUri farmasian-tohtori-koulutuskoodi :koulutustyyppi "yo" :metadata fixture/yo-koulutus-metadata)
       (check-all-nil)
       (koulutus-search/do-index [koulutus-oid])
       (let [koulutus (get-doc koulutus-search/index-name koulutus-oid)
             koulutustyypit (get-koulutustyypit koulutus)]
         (is (= koulutustyypit ["yo" "tohtori" "korkeakoulutus"])))))))

(deftest adds-kandi-ja-maisteri-koulutustyyppi
  (fixture/with-mocked-indexing
   (testing "Indexer should add kandi-ja-maisteri koulutustyyppi when tutkintotyyppi is alempi korkeakoulututkinto + ylempi korkeakoulututkinto"
     (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/tutkintotyypit mock-tutkintotyyppi]
       (fixture/update-koulutus-mock koulutus-oid :koulutuksetKoodiUri kandi-ja-maisteri-koulutuskoodi :koulutustyyppi "yo" :metadata fixture/yo-koulutus-metadata)
       (check-all-nil)
       (koulutus-search/do-index [koulutus-oid])
       (let [koulutus (get-doc koulutus-search/index-name koulutus-oid)
             koulutustyypit (get-koulutustyypit koulutus)]
         (is (= koulutustyypit ["yo" "kandi-ja-maisteri" "korkeakoulutus"])))))))

(deftest adds-kandi-ja-maisteri-if-tukintotyypit-contains-those
  (fixture/with-mocked-indexing
   (testing "Indexer should add kandi-ja-maisteri koulutustyyppi if list of tutkintotyypit contains kandi + maisteri"
     (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/tutkintotyypit mock-tutkintotyyppi]
       (fixture/update-koulutus-mock koulutus-oid :koulutuksetKoodiUri kandi-maisteri-tohtori-koulutuskoodi :koulutustyyppi "yo" :metadata fixture/yo-koulutus-metadata)
       (check-all-nil)
       (koulutus-search/do-index [koulutus-oid])
       (let [koulutus (get-doc koulutus-search/index-name koulutus-oid)
             koulutustyypit (get-koulutustyypit koulutus)]
         (is (= koulutustyypit ["yo" "kandi-ja-maisteri" "korkeakoulutus"])))))))

(deftest adds-kandi-if-multiple-kandi-koulutuskoodi
  (fixture/with-mocked-indexing
   (testing "Indexer should add kandi koulutustyyppi even if koulutus has many kandi koulutuskoodis"
     (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/tutkintotyypit mock-tutkintotyyppi]
       (fixture/update-koulutus-mock koulutus-oid :koulutuksetKoodiUri (str elainlaaketietieen-kandi-koulutuskoodi "," liikuntakasvatuksen-kandi-koulutuskoodi) :koulutustyyppi "yo" :metadata fixture/yo-koulutus-metadata)
       (check-all-nil)
       (koulutus-search/do-index [koulutus-oid])
       (let [koulutus (get-doc koulutus-search/index-name koulutus-oid)
             koulutustyypit (get-koulutustyypit koulutus)]
         (is (= koulutustyypit ["yo" "kandi" "korkeakoulutus"])))))))

(deftest adds-maisteri-if-multiple-maisteri-koulutuskoodi
  (fixture/with-mocked-indexing
   (testing "Indexer should add maisteri koulutustyyppi even if koulutus has many maisteri koulutuskoodis"
     (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/tutkintotyypit mock-tutkintotyyppi]
       (fixture/update-koulutus-mock koulutus-oid :koulutuksetKoodiUri (str arkkitehti-koulutuskoodi "," fil-maist-kemia-koulutuskoodi) :koulutustyyppi "yo" :metadata fixture/yo-koulutus-metadata)
       (check-all-nil)
       (koulutus-search/do-index [koulutus-oid])
       (let [koulutus (get-doc koulutus-search/index-name koulutus-oid)
             koulutustyypit (get-koulutustyypit koulutus)]
         (is (= koulutustyypit ["yo" "maisteri" "korkeakoulutus"])))))))

(deftest adds-tohtori-if-multiple-tohtori-koulutuskoodi
  (fixture/with-mocked-indexing
   (testing "Indexer should add tohtori koulutustyyppi even if koulutus has many tohtori koulutuskoodis"
     (with-redefs [kouta-indeksoija-service.indexer.tools.koodisto/tutkintotyypit mock-tutkintotyyppi]
       (fixture/update-koulutus-mock koulutus-oid :koulutuksetKoodiUri (str farmasian-tohtori-koulutuskoodi "," fil-tohtori-englannin-kieli-koulutuskoodi) :koulutustyyppi "yo" :metadata fixture/yo-koulutus-metadata)
       (check-all-nil)
       (koulutus-search/do-index [koulutus-oid])
       (let [koulutus (get-doc koulutus-search/index-name koulutus-oid)
             koulutustyypit (get-koulutustyypit koulutus)]
         (is (= koulutustyypit ["yo" "tohtori" "korkeakoulutus"])))))))