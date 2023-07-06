(ns kouta-indeksoija-service.cache.hierarkia-test-fast
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.test-tools :as test-tools]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as tools]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as hierarkia]))

(defn mock-get-all-organisaatiot []
  (test-tools/parse (str "test/resources/organisaatiot/hierarkia.json")))

(deftest hierarkia-test-fast
  (testing "Toimipiste parents set correctly"
    (let [cache-atom (atom {"1.2.246.562.10.1" {:organisaatiotyypit ["organisaatiotyyppi_02"]},
                            "1.2.246.562.10.11" {:organisaatiotyypit ["organisaatiotyyppi_03"], :parentOid "1.2.246.562.10.1"},
                            "1.2.246.562.10.2" {:organisaatiotyypit ["organisaatiotyyppi_02"]},
                            "1.2.246.562.10.22" {:organisaatiotyypit ["organisaatiotyyppi_03"], :parentOid "1.2.246.562.10.2"},
                            "1.2.246.562.10.222" {:organisaatiotyypit ["organisaatiotyyppi_03"], :parentOid "1.2.246.562.10.22"},
                            "1.2.246.562.10.3" {:organisaatiotyypit ["organisaatiotyyppi_02"]},
                            "1.2.246.562.10.33" {:organisaatiotyypit ["organisaatiotyyppi_03"], :parentOid "1.2.246.562.10.3"},
                            "1.2.246.562.10.333" {:organisaatiotyypit ["organisaatiotyyppi_03"], :parentOid "1.2.246.562.10.33"},
                            "1.2.246.562.10.3333" {:organisaatiotyypit ["organisaatiotyyppi_03"], :parentOid "1.2.246.562.10.333"},
                            "1.2.246.562.10.44" {:organisaatiotyypit ["organisaatiotyyppi_03"]},
                            "1.2.246.562.10.444" {:organisaatiotyypit ["organisaatiotyyppi_03"], :parentOid "1.2.246.562.10.44"}})]
      (hierarkia/fix-toimipiste-parents cache-atom)
      (is (= "1.2.246.562.10.1" (:parentOid (get @cache-atom "1.2.246.562.10.11"))))
      (is (= "1.2.246.562.10.2" (:parentOid (get @cache-atom "1.2.246.562.10.222"))))
      (is (= "1.2.246.562.10.3" (:parentOid (get @cache-atom "1.2.246.562.10.3333"))))
      (is (= false (contains? (get @cache-atom "1.2.246.562.10.444") :parentOid)))))

  (testing "All indexable oppilaitos oids"
    (let [cache-atom (atom {"1.2.246.562.10.1" {:oid "1.2.246.562.10.1" :organisaatiotyypit ["organisaatiotyyppi_02"] :status "AKTIIVINEN"},
                            "1.2.246.562.10.2" {:oid "1.2.246.562.10.2" :organisaatiotyypit ["organisaatiotyyppi_03"] :status "AKTIIVINEN"},
                            "1.2.246.562.10.3" {:oid "1.2.246.562.10.3" :organisaatiotyypit ["organisaatiotyyppi_02"] :status "PASSIIVINEN"},
                            "1.2.246.562.10.4" {:oid "1.2.246.562.10.4" :organisaatiotyypit ["organisaatiotyyppi_02"] :status "AKTIIVINEN"}})]
      (with-redefs [kouta-indeksoija-service.indexer.cache.hierarkia/get-hierarkia-cached (fn [] cache-atom)]
        (is (= ["1.2.246.562.10.1" "1.2.246.562.10.2" "1.2.246.562.10.4"] (hierarkia/get-all-indexable-oppilaitos-oids)))))))

(deftest organisaatio-tool-test-fast
  (with-redefs [kouta-indeksoija-service.rest.organisaatio/get-all-organisaatiot mock-get-all-organisaatiot]
    (let [assert-toimipiste (fn [toimipiste] (and
                                              (= (keys toimipiste) [:oid :status :organisaatiotyypit :parentOid :nimi
                                                                    :kotipaikkaUri :kieletUris])
                                              (= (:parentOid toimipiste) "1.2.246.562.10.54453921329")
                                              (= (:organisaatiotyypit toimipiste) ["organisaatiotyyppi_03"])))]
      (testing "Oppilaitos hierarkia fetched correctly from cache"
        (let [res (tools/find-oppilaitos-hierarkia-from-cache (hierarkia/get-hierarkia-cached) "1.2.246.562.10.54453921329")]
          (is (= [:oid :status :organisaatiotyypit :nimi :kotipaikkaUri :children] (keys res)))
          (is (= "1.2.246.562.10.2014041511401945349694" (:oid res)))
          (is (= ["organisaatiotyyppi_01"] (:organisaatiotyypit res)))
          (is (= "AKTIIVINEN" (:status res)))
          (is (= 1 (count (:children res))))
          (let [oppilaitos (first (:children res))]
            (is (= [:children :kieletUris :organisaatiotyypit :parentOid
                    :nimi :oid :oppilaitostyyppi :status :kotipaikkaUri]
                   (keys oppilaitos)))
            (is (= "1.2.246.562.10.54453921329"
                   (:oid oppilaitos)))
            (is (= "1.2.246.562.10.2014041511401945349694" (:parentOid oppilaitos)))
            (is (= ["organisaatiotyyppi_02"] (:organisaatiotyypit oppilaitos)))
            (is (= 5 (count (:children oppilaitos))))
            (let [toimipisteet (:children oppilaitos)]
              (is (assert-toimipiste (first (filter #(= (:oid %) "1.2.246.562.10.41117090922") toimipisteet))))
              (is (assert-toimipiste (first (filter #(= (:oid %) "1.2.246.562.10.42160341923") toimipisteet))))
              (is (assert-toimipiste (first (filter #(= (:oid %) "1.2.246.562.10.82061758015") toimipisteet))))
              (is (assert-toimipiste (first (filter #(= (:oid %) "1.2.246.562.10.83918870482") toimipisteet))))
              (is (assert-toimipiste (first (filter #(= (:oid %) "1.2.246.562.10.52750255714") toimipisteet))))))))

      (testing "Oppilaitos fetched correctly from cache with member toimipiste oid"
        (let [oppilaitos (hierarkia/find-oppilaitos-by-own-or-child-oid "1.2.246.562.10.83918870482")]
          (is (= [:children :kieletUris :organisaatiotyypit :parentOid
                  :nimi :oid :oppilaitostyyppi :status :kotipaikkaUri]
                 (keys oppilaitos)))
          (is (= "1.2.246.562.10.54453921329" (:oid oppilaitos)))
          (is (= "1.2.246.562.10.2014041511401945349694" (:parentOid oppilaitos)))
          (let [toimipisteet (:children oppilaitos)]
            (is (assert-toimipiste (first (filter #(= (:oid %) "1.2.246.562.10.41117090922") toimipisteet))))
            (is (assert-toimipiste (first (filter #(= (:oid %) "1.2.246.562.10.42160341923") toimipisteet))))
            (is (assert-toimipiste (first (filter #(= (:oid %) "1.2.246.562.10.82061758015") toimipisteet))))
            (is (assert-toimipiste (first (filter #(= (:oid %) "1.2.246.562.10.83918870482") toimipisteet))))
            (is (assert-toimipiste (first (filter #(= (:oid %) "1.2.246.562.10.52750255714") toimipisteet)))))))
      (testing "Oppilaitos without toimipisteet fetched correctly from cache"
        (let [oppilaitos (hierarkia/find-oppilaitos-by-own-or-child-oid "1.2.246.562.10.53670619591")]
          (is (= [:children :kieletUris :organisaatiotyypit :parentOid
                  :nimi :oid :oppilaitostyyppi :status :kotipaikkaUri]
                 (keys oppilaitos)))
          (is (= "1.2.246.562.10.53670619591" (:oid oppilaitos)))
          (is (= "1.2.246.562.10.73481747332" (:parentOid oppilaitos)))
          (is (= (:children oppilaitos) []))))

      (testing "Parent search returns oppilaitos itself when koulutustoimija not found"
        (let [oppilaitos {:parentOid "1.2.246.562.10.1", :organisaatiotyypit ["organisaatiotyyppi_02"]}
              cache-atom (atom {"1.2.246.562.10.11" oppilaitos})
              res (tools/attach-parent-to-oppilaitos-from-cache cache-atom oppilaitos)]
          (is (= ["organisaatiotyyppi_02"]
                 (:organisaatiotyypit res)))))

      (testing "Resolve indexable oppilaitos- or toimipiste-oids from cache for given oids, i.e. oid itself or children of koulutustoimija"
        (let [cache-atom (hierarkia/get-hierarkia-cached)
              koulutustoimija-oid "1.2.246.562.10.53814745062"
              oppilaitos-oid      "1.2.246.562.10.197113642410"
              toimipiste-oid      "1.2.246.562.10.80117936338"
              oids-to-index (tools/resolve-organisaatio-oids-to-index cache-atom [koulutustoimija-oid oppilaitos-oid toimipiste-oid])]
          (is (= ["1.2.246.562.10.112212847610" ;;koulutustoimijan lapsi
                  "1.2.246.562.10.81927839589" ;;koulutustoimijan lapsi
                  "1.2.246.562.10.39218317368" ;;koulutustoimijan lapsi
                  "1.2.246.562.10.197113642410" ;;oppilaitos
                  "1.2.246.562.10.32506551657" ;;toimipisteen parent (oppilaitos)
                  "1.2.246.562.10.80117936338" ;;toimipiste
                  ]
                 oids-to-index)))))))

