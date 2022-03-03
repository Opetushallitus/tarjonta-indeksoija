(ns mocks.kouta-internal-mocks
  (:require
   [clj-log.access-log]
   [clojure.test :refer :all]
   [clojure.string :as string]
   [clojure.java.shell :refer [sh]]
   [clojure.java.io :as io]
   [clj-elasticsearch.elastic-utils :as e-utils]
   [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
   [kouta-indeksoija-service.fixture.external-services :as mocks]))

(intern 'clj-log.access-log 'service "kouta-indeksoija")

(defn- export-elastic-data []
  (println "Starting elasticdump...")
  (.mkdirs (io/file "elasticdump/kouta-internal"))
  (let [e-host (string/replace e-utils/elastic-host #"127\.0\.0\.1|localhost" "host.docker.internal")
        pwd (System/getProperty "user.dir")
        p (sh "./dump_elastic_data.sh" (str pwd "/elasticdump/kouta-internal") e-host)]
    (println (:out p))
    (println (:err p))))

(defonce OphOid             "1.2.246.562.10.00000000001")
(defonce ParentOid          "1.2.246.562.10.594252633210")
(defonce ChildOid           "1.2.246.562.10.81934895871")
(defonce EvilChild          "1.2.246.562.10.66634895871")
(defonce GrandChildOid      "1.2.246.562.10.67603619189")
(defonce EvilGrandChildOid  "1.2.246.562.10.66603619189")
(defonce EvilCousin         "1.2.246.562.10.66634895666")

(defonce sorakuvausId      "9267884f-fba1-4b85-8bb3-3eb77440c197")

(defonce ammKoulutusOid     "1.2.246.562.13.00000000000000000001")
(defonce ammTukinnonosaOid  "1.2.246.562.13.00000000000000000002")
(defonce ammOsaamisalaOid   "1.2.246.562.13.00000000000000000003")

(defonce ammToteutusOid             "1.2.246.562.17.00000000000000000001")
(defonce ammTukinnonosaToteutusOid  "1.2.246.562.17.00000000000000000002")
(defonce ammOsaamisalaToteutusOid   "1.2.246.562.17.00000000000000000003")

(defonce ataruId1          "dcd38a87-912e-4e91-8840-99c7e242dd53")
(defonce ataruId2          "dcd38a87-912e-4e91-8840-99c7e242dd54")
(defonce ataruId3          "dcd38a87-912e-4e91-8840-99c7e242dd55")

(defonce hakuOid1          "1.2.246.562.29.00000000000000000001")
(defonce hakuOid2          "1.2.246.562.29.00000000000000000002")
(defonce hakuOid3          "1.2.246.562.29.00000000000000000003")
(defonce hakuOid4          "1.2.246.562.29.00000000000000000004")
(defonce hakuOid5          "1.2.246.562.29.00000000000000000005")
(defonce hakuOid6          "1.2.246.562.29.00000000000000000006")

(defonce hakukohdeOid1     "1.2.246.562.20.00000000000000000001")
(defonce hakukohdeOid2     "1.2.246.562.20.00000000000000000002")

(defonce valintaPerusteId1  "fa7fcb96-3f80-4162-8d19-5b74731cf90c")

(defonce child-org
         (mocks/create-organisaatio-hierarkia
          {:oid ParentOid
           :nimi {:fi "Koulutuskeskus Salpaus -kuntayhtymä"
                  :sv "Koulutuskeskus Salpaus -kuntayhtymä sv"}
           :kotipaikka "kunta_398"
           :kielet ["oppilaitoksenopetuskieli_1#1"]}
          {:oid ChildOid
           :nimi {:fi "Koulutuskeskus Salpaus"
                  :sv "Koulutuskeskus Salpaus sv"}
           :kotipaikka "kunta_398"
           :kielet ["oppilaitoksenopetuskieli_1#1"]}
          [{:oid GrandChildOid
            :nimi {:fi "Koulutuskeskus Salpaus, Lahti, Jokimaa"
                   :sv "Koulutuskeskus Salpaus, Lahti, Jokimaa sv "}
            :kotipaikka "kunta_398"
            :kielet ["oppilaitoksenopetuskieli_1#1" ]},
           {:oid EvilGrandChildOid
            :nimi {:fi "Koulutuskeskus Salpaus, Lahti, Pahamaa"
                   :sv "Koulutuskeskus Salpaus, Lahti, Pahamaa sv "}
            :kotipaikka "kunta_398"
            :kielet ["oppilaitoksenopetuskieli_1#1"]}]))

(defonce evil-org
         (mocks/create-organisaatio-hierarkia
          {:oid ParentOid
           :nimi {:fi "Koulutuskeskus Salpaus -kuntayhtymä"
                  :sv "Koulutuskeskus Salpaus -kuntayhtymä sv"}
           :kotipaikka "kunta_398"
           :kielet ["oppilaitoksenopetuskieli_1#1"]}
          {:oid EvilChild
           :nimi {:fi "Evil child"
                  :sv "Evil child sv"}
           :kotipaikka "kunta_618"
           :kielet ["oppilaitoksenopetuskieli_1#1"]}
          [{:oid EvilCousin
            :nimi {:fi "Evil cousin"
                   :sv "Evil cousin sv "}
            :kotipaikka "kunta_618"
            :kielet ["oppilaitoksenopetuskieli_1#1" ]}]))

(defn- orgs
  [x & {:as params}]
  (cond
    (or (= x ParentOid) (= x ChildOid) (= x GrandChildOid) (= x EvilGrandChildOid)) child-org
    (or (= x EvilChild) (= x EvilCousin)) evil-org))

(comment
 (deftest -main []
                (fixture/init)
                (fixture/add-sorakuvaus-mock sorakuvausId :organisaatioOid ChildOid)

                (fixture/add-koulutus-mock ammKoulutusOid :koulutustyyppi "amm" :tila "julkaistu" :organisaatioOid ChildOid :sorakuvausId sorakuvausId)
                (fixture/add-koulutus-mock ammTukinnonosaOid :koulutustyyppi "amm-tutkinnon-osa" :tila "julkaistu" :organisaatioOid ChildOid :sorakuvausId sorakuvausId
                                           :johtaaTutkintoon false :koulutuksetKoodiUri nil :ePerusteId nil :metadata fixture/amm-tutkinnon-osa-koulutus-metadata)
                (fixture/add-koulutus-mock ammOsaamisalaOid :koulutustyyppi "amm-osaamisala" :tila "julkaistu" :organisaatioOid ChildOid
                                           :johtaaTutkintoon false :sorakuvausId sorakuvausId :metadata fixture/amm-osaamisala-koulutus-metadata)

                (fixture/add-toteutus-mock ammToteutusOid ammKoulutusOid :tila "julkaistu" :organisaatioOid ChildOid :tarjoajat ChildOid
                                           :metadata {:tyyppi "amm"})
                (fixture/add-toteutus-mock ammTukinnonosaToteutusOid ammTukinnonosaOid :tila "julkaistu" :organisaatioOid ChildOid :tarjoajat ChildOid
                                           :metadata {:tyyppi "amm-tutkinnon-osa"})
                (fixture/add-toteutus-mock ammOsaamisalaToteutusOid ammOsaamisalaOid :tila "julkaistu" :organisaatioOid ChildOid :tarjoajat ChildOid
                                           :metadata {:tyyppi "amm-osaamisala"})

                (fixture/add-haku-mock hakuOid1 :tila "julkaistu" :organisaatioOid ChildOid :hakulomaketyyppi "ataru" :hakulomakeAtaruId ataruId1)
                (fixture/add-haku-mock hakuOid2 :tila "julkaistu" :organisaatioOid ChildOid :hakulomaketyyppi "ataru" :hakulomakeAtaruId ataruId1)
                (fixture/add-haku-mock hakuOid3 :tila "julkaistu" :organisaatioOid ChildOid :hakulomaketyyppi "ataru" :hakulomakeAtaruId ataruId2)
                (fixture/add-haku-mock hakuOid4 :tila "julkaistu" :organisaatioOid ChildOid :hakulomaketyyppi "ataru" :hakulomakeAtaruId ataruId2)
                (fixture/add-haku-mock hakuOid5 :tila "julkaistu" :organisaatioOid ParentOid :hakulomaketyyppi "ataru" :hakulomakeAtaruId ataruId1)
                (fixture/add-haku-mock hakuOid6 :tila "julkaistu" :organisaatioOid EvilChild :hakulomaketyyppi "ataru" :hakulomakeAtaruId ataruId1)

                (fixture/add-valintaperuste-mock valintaPerusteId1 :organisaatioOid ChildOid)
                (fixture/add-hakukohde-mock hakukohdeOid1 ammToteutusOid hakuOid1 :valintaperuste valintaPerusteId1 :organisaatioOid ChildOid :jarjestyspaikkaOid ChildOid)
                (fixture/add-hakukohde-mock hakukohdeOid2 ammToteutusOid hakuOid1 :valintaperuste valintaPerusteId1 :organisaatioOid GrandChildOid :jarjestyspaikkaOid OphOid)
                (fixture/index-oids-without-related-indices {:koulutukset [ammKoulutusOid ammTukinnonosaOid ammOsaamisalaOid]
                                                             :toteutukset [ammToteutusOid ammTukinnonosaToteutusOid ammOsaamisalaToteutusOid]
                                                             :haut [hakuOid1 hakuOid2 hakuOid3 hakuOid4 hakuOid5 hakuOid6]
                                                             :valintaperusteet [valintaPerusteId1]
                                                             :hakukohteet [hakukohdeOid1 hakukohdeOid2]
                                                             :oppilaitokset [child-org evil-org]} orgs)
                (export-elastic-data))
 )
