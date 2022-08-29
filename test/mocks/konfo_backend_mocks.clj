(ns mocks.konfo-backend-mocks
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

;Ajamalla tämän namespacen testin voi generoida elastic-testidataa konfo-backend-palvelua varten
;lein test :only mocks.kouta-index-mocks

(defonce koulutusOid1             "1.2.246.562.13.000001")
(defonce koulutusOid2             "1.2.246.562.13.000002")
(defonce koulutusOid3             "1.2.246.562.13.000003")
(defonce koulutusOid4             "1.2.246.562.13.000004")
(defonce koulutusOid5             "1.2.246.562.13.000005")
(defonce koulutusOid6             "1.2.246.562.13.000006")

(defonce lukio-oid                "1.2.246.562.13.000008")
(defonce kk-koulutus-oid          "1.2.246.562.13.000009")

(defonce traktoriala-oid            "1.2.246.562.13.000010")
(defonce hevosala-oid               "1.2.246.562.13.000011")
(defonce traktoriala-oid2           "1.2.246.562.13.000012")
(defonce hevostutkinnon-osa-oid     "1.2.246.562.13.000013")
(defonce hevososaamisala-oid        "1.2.246.562.13.000014")
(defonce yo-koulutus-oid            "1.2.246.562.13.000015")
(defonce amk-oid                    "1.2.246.562.13.000016")
(defonce lukio-oid2                 "1.2.246.562.13.000017")
(defonce amm-muu-oid                "1.2.246.562.13.000018")

(defonce aakkostus-koulutus-oid1  "1.2.246.562.13.000020")
(defonce aakkostus-koulutus-oid2  "1.2.246.562.13.000021")
(defonce aakkostus-koulutus-oid3  "1.2.246.562.13.000022")
(defonce aakkostus-koulutus-oid4  "1.2.246.562.13.000023")
(defonce aakkostus-koulutus-oid5  "1.2.246.562.13.000024")

(defonce keyword-koulutus-oid1    "1.2.246.562.13.000030")
(defonce keyword-koulutus-oid2    "1.2.246.562.13.000031")
(defonce keyword-koulutus-oid3    "1.2.246.562.13.000032")
(defonce keyword-koulutus-oid4    "1.2.246.562.13.000033")
(defonce keyword-koulutus-oid5    "1.2.246.562.13.000034")
(defonce keyword-koulutus-oid6    "1.2.246.562.13.000035")
(defonce keyword-koulutus-oid7    "1.2.246.562.13.000036")
(defonce keyword-koulutus-oid8    "1.2.246.562.13.000037")
(defonce keyword-koulutus-oid9    "1.2.246.562.13.000038")
(defonce keyword-koulutus-oid10   "1.2.246.562.13.000039")
(defonce keyword-koulutus-oid11   "1.2.246.562.13.000040")
(defonce keyword-koulutus-oid12   "1.2.246.562.13.000041")
(defonce keyword-koulutus-oid13   "1.2.246.562.13.000042")
(defonce keyword-koulutus-oid14   "1.2.246.562.13.000043")
(defonce keyword-koulutus-oid15   "1.2.246.562.13.000044")
(defonce keyword-koulutus-oid16   "1.2.246.562.13.000045")
(defonce keyword-koulutus-oid17   "1.2.246.562.13.000046")
(defonce keyword-koulutus-oid18   "1.2.246.562.13.000047")


(defonce sorakuvausId "2ff6700d-087f-4dbf-9e42-7f38948f227a")


(defonce hakuOid1    "1.2.246.562.29.0000001")
(defonce hakuOid2    "1.2.246.562.29.0000002")
(defonce hakuOid3    "1.2.246.562.29.0000003")
(defonce hakuOid4    "1.2.246.562.29.0000004")
(defonce hakuOid5    "1.2.246.562.29.0000005")
(defonce kk-haku-oid "1.2.246.562.29.0000006")


(defonce toteutusOid1           "1.2.246.562.17.000001")
(defonce toteutusOid2           "1.2.246.562.17.000002")
(defonce toteutusOid3           "1.2.246.562.17.000003")
(defonce toteutusOid4           "1.2.246.562.17.000004")
(defonce toteutusOid5           "1.2.246.562.17.000005")
(defonce toteutusOid6           "1.2.246.562.17.000006")

(defonce lukio-toteutus-oid     "1.2.246.562.17.000007")
(defonce kk-toteutus-oid        "1.2.246.562.17.000008")
(defonce ponikoulu-oid          "1.2.246.562.17.000010")
(defonce valtrakoulu-oid        "1.2.246.562.17.000011")
(defonce massikkakoulu-oid      "1.2.246.562.17.000012")
(defonce zetorkoulu-oid         "1.2.246.562.17.000013")
(defonce poniosatoteutus-oid    "1.2.246.562.17.000014")

(defonce aakkostus-toteutus-oid1  "1.2.246.562.17.000015")
(defonce aakkostus-toteutus-oid2  "1.2.246.562.17.000016")
(defonce aakkostus-toteutus-oid3  "1.2.246.562.17.000017")
(defonce aakkostus-toteutus-oid4  "1.2.246.562.17.000018")
(defonce aakkostus-toteutus-oid5  "1.2.246.562.17.000019")

(defonce keyword-toteutus-oid1  "1.2.246.562.17.000020")
(defonce keyword-toteutus-oid2  "1.2.246.562.17.000021")
(defonce keyword-toteutus-oid3  "1.2.246.562.17.000022")
(defonce keyword-toteutus-oid4  "1.2.246.562.17.000023")
(defonce keyword-toteutus-oid5  "1.2.246.562.17.000024")
(defonce keyword-toteutus-oid6  "1.2.246.562.17.000025")
(defonce keyword-toteutus-oid7  "1.2.246.562.17.000026")
(defonce keyword-toteutus-oid8  "1.2.246.562.17.000027")
(defonce keyword-toteutus-oid9  "1.2.246.562.17.000028")



(defonce hakukohdeOid1     "1.2.246.562.20.0000001")
(defonce hakukohdeOid2     "1.2.246.562.20.0000002")
(defonce hakukohdeOid3     "1.2.246.562.20.0000003")
(defonce hakukohdeOid4     "1.2.246.562.20.0000004")
(defonce hakukohdeOid5     "1.2.246.562.20.0000005")
(defonce hakukohdeOid6     "1.2.246.562.20.0000006")
(defonce hakukohdeOid7     "1.2.246.562.20.0000007")
(defonce hakukohdeOid8     "1.2.246.562.20.0000008")
(defonce hakukohdeOid9     "1.2.246.562.20.0000009")
(defonce hakukohdeOid10    "1.2.246.562.20.0000010")
(defonce kk-hakukohde-oid  "1.2.246.562.20.0000011")

(defonce valintaperusteId1 "31972648-ebb7-4185-ac64-31fa6b841e34")
(defonce valintaperusteId2 "31972648-ebb7-4185-ac64-31fa6b841e35")
(defonce valintaperusteId3 "31972648-ebb7-4185-ac64-31fa6b841e36")
(defonce valintaperusteId4 "31972648-ebb7-4185-ac64-31fa6b841e37")
(defonce valintaperusteId5 "31972648-ebb7-4185-ac64-31fa6b841e38")
(defonce valintaperusteId6 "31972648-ebb7-4185-ac64-31fa6b841e39")


(defonce oppilaitosOid1  "1.2.246.562.10.00101010101")
(defonce oppilaitosOid2  "1.2.246.562.10.00101010102")
(defonce oppilaitosOid3  "1.2.246.562.10.00101010103")
(defonce oppilaitosOid4  "1.2.246.562.10.00101010104")
(defonce oppilaitosOid5  "1.2.246.562.10.00101010105")
(defonce oppilaitosOid6  "1.2.246.562.10.00101010106")
(defonce oppilaitosOid7  "1.2.246.562.10.00101010107")

(defonce aakkostus-oppilaitos-oid1  "1.2.246.562.10.0000011")
(defonce aakkostus-oppilaitos-oid2  "1.2.246.562.10.0000012")
(defonce aakkostus-oppilaitos-oid3  "1.2.246.562.10.0000013")
(defonce aakkostus-oppilaitos-oid4  "1.2.246.562.10.0000014")
(defonce aakkostus-oppilaitos-oid5  "1.2.246.562.10.0000015")

(defonce punkaharjun-yliopisto    "1.2.246.562.10.000002")
(defonce punkaharjun-toimipiste-1 "1.2.246.562.10.000003")
(defonce punkaharjun-toimipiste-2 "1.2.246.562.10.000004")
(defonce helsingin-yliopisto      "1.2.246.562.10.000005")
(defonce helsingin-toimipiste     "1.2.246.562.10.000006")


(defonce oppilaitoksenOsaOid1  "1.2.246.562.10.001010101011")
(defonce oppilaitoksenOsaOid2  "1.2.246.562.10.001010101012")
(defonce oppilaitoksenOsaOid3  "1.2.246.562.10.001010101021")
(defonce oppilaitoksenOsaOid4  "1.2.246.562.10.001010101022")
(defonce oppilaitoksenOsaOid5  "1.2.246.562.10.001010101023")

(defonce punkaharju-org
         (mocks/create-organisaatio-hierarkia
          {:oid "1.2.246.562.10.000001"
           :nimi {:fi "Punkaharjun kunta"
                  :sv "Punkaharjun kunta sv"}
           :kotipaikka "kunta_618"
           :kielet ["oppilaitoksenopetuskieli_1#1",
                    "oppilaitoksenopetuskieli_2#1" ]}
          {:oid punkaharjun-yliopisto
           :nimi {:fi "Punkaharjun yliopisto"
                  :sv "Punkaharjun yliopisto sv"}
           :kotipaikka "kunta_618"
           :kielet ["oppilaitoksenopetuskieli_1#1",
                    "oppilaitoksenopetuskieli_2#1" ]}
          [{:oid punkaharjun-toimipiste-1
            :nimi {:fi "Punkaharjun yliopiston toimipiste"
                   :sv "Punkaharjun yliopiston toimipiste sv "}
            :kotipaikka "kunta_618"
            :kielet ["oppilaitoksenopetuskieli_2#1" ]},
           {:oid punkaharjun-toimipiste-2
            :nimi {:fi "Punkaharjun yliopiston Karjaan toimipiste"
                   :sv "Punkaharjun yliopiston Karjaan toimipiste sv "}
            :kotipaikka "kunta_220"
            :kielet ["oppilaitoksenopetuskieli_1#1"]}]))

(defonce helsinki-org
         (mocks/create-organisaatio-hierarkia
          {:oid "1.2.246.562.10.000001"
           :nimi {:fi "Helsingin kunta" :sv "Helsingin kunta sv"}
           :kotipaikka "kunta_091"
           :kielet [ "oppilaitoksenopetuskieli_1#1", "oppilaitoksenopetuskieli_2#1" ]}
          {:oid helsingin-yliopisto
           :nimi {:fi "Helsingin yliopisto" :sv "Helsingin yliopisto sv"}
           :kotipaikka "kunta_091"
           :kielet [ "oppilaitoksenopetuskieli_1#1", "oppilaitoksenopetuskieli_2#1" ]}
          [{:oid helsingin-toimipiste
            :nimi {:fi "Helsingin yliopiston toimipiste" :sv "Helsingin yliopiston toimipiste sv "}
            :kotipaikka "kunta_091"
            :kielet [ "oppilaitoksenopetuskieli_2#1" ]}]))

(defn- orgs
  [x & {:as params}]
  (cond
    (or (= x punkaharjun-yliopisto) (= x punkaharjun-toimipiste-1) (= x punkaharjun-toimipiste-2)) punkaharju-org
    (or (= x helsingin-yliopisto) (= x helsingin-toimipiste)) helsinki-org))

(defonce koulutus-metatieto
          {:tyyppi "amm"

           :koulutusalaKoodiUrit ["kansallinenkoulutusluokitus2016koulutusalataso1_01#1"
                                  "kansallinenkoulutusluokitus2016koulutusalataso1_02#1"]
           :kuvaus  {:fi "kuvaus", :sv "kuvaus sv"}
           :lisatiedot [{:otsikkoKoodiUri "koulutuksenlisatiedot_03#1"
                         :teksti {:fi "Opintojen lisätieto"
                                  :sv "Opintojen lisätieto sv"}}]})

(defonce oletus-toteutus-metatieto
         {:tyyppi           "amm"
           :asiasanat        [{:kieli "fi" :arvo "hevonen"}]
           :ammattinimikkeet [{:kieli "fi" :arvo "ponityttö"}]
           :ammatillinenPerustutkintoErityisopetuksena false
           :opetus {:opetuskieliKoodiUrit ["oppilaitoksenopetuskieli_02"]
                    :opetustapaKoodiUrit ["opetuspaikkakk_02"]
                    :opetusaikaKoodiUrit []}})

(defonce aikuisten-perusopetus-toteutus-metatieto
         {:tyyppi           "aikuisten-perusopetus"
          :asiasanat        [{:kieli "fi" :arvo "hevonen"}]
          :ammattinimikkeet [{:kieli "fi" :arvo "ponityttö"}]
          :ammatillinenPerustutkintoErityisopetuksena false
          :opetus {:opetuskieliKoodiUrit ["oppilaitoksenopetuskieli_02"]
                   :opetustapaKoodiUrit ["opetuspaikkakk_02"]
                   :opetusaikaKoodiUrit []}})


(defonce amm-toteutus-metatieto
         (merge (:metadata fixture/default-toteutus-map)
         {:tyyppi           "amm"
          :asiasanat        [{:kieli "fi" :arvo "traktori"}]
          :ammattinimikkeet [{:kieli "fi" :arvo "korjaaja"}]
          :ammatillinenPerustutkintoErityisopetuksena false
          :kuvaus  {:fi "kuvaus", :sv "kuvaus sv"}
          }))

(defonce amk-toteutus-metatieto
          {:tyyppi           "amk"
           :asiasanat        [{:kieli "fi" :arvo "hevonen"}]
           :ammattinimikkeet [{:kieli "fi" :arvo "ponipoika"}]
           :opetus {:opetuskieliKoodiUrit ["oppilaitoksenopetuskieli_01"]
                    :opetustapaKoodiUrit ["opetuspaikkakk_01"]
                    :koulutuksenTarkkaAlkamisaika true
                    :koulutuksenAlkamisvuosi 2019}})

(defonce amm-osaamisala-toteutus-metatieto
         {:tyyppi "amm-osaamisala"
          :opetus {:opetuskieliKoodiUrit ["oppilaitoksenopetuskieli_1", "oppilaitoksenopetuskieli_2"]
                   :opetustapaKoodiUrit ["opetuspaikkakk_1", "opetuspaikkakk_2"]
                   :opetusaikaKoodiUrit ["opetusaikakk_1"]}})

(defonce yo-toteutus-metatieto
         (merge (dissoc (:metadata fixture/default-toteutus-map) :osaamisalat :ammatillinenPerustutkintoErityisopetuksena)
                {:tyyppi           "yo"
                 :asiasanat        []
                 :ammattinimikkeet []
                 :kuvaus  {:fi "kuvaus", :sv "kuvaus sv"}
                 }))

(defn- export-elastic-data []
  (println "Starting elasticdump...")
  (.mkdirs (io/file "elasticdump/konfo-backend"))
  (let [e-host (string/replace e-utils/elastic-host #"127\.0\.0\.1|localhost" "host.docker.internal")
        pwd (System/getProperty "user.dir")
        p (sh "./dump_elastic_data.sh" (str pwd "/elasticdump/konfo-backend") e-host)]
    (println (:out p))
    (println (:err p))))

(comment
  (deftest -main []
    (fixture/init)
    (fixture/add-sorakuvaus-mock sorakuvausId :tila "julkaistu" :nimi "Kiva SORA-kuvaus")


    (fixture/add-koulutus-mock koulutusOid1 :koulutustyyppi "amm" :tila "julkaistu" :nimi "Traktorialan koulutus" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock koulutusOid2 :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hevosalan koulutus" :modified "2018-05-05T12:02:23" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock koulutusOid3 :koulutustyyppi "amm" :tila "julkaistu" :nimi "ICT esiopinnot" :muokkaaja "1.2.246.562.24.55555555555" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock koulutusOid4 :tila "arkistoitu" :nimi "Tietojenkäsittelytieteen perusopinnot" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock koulutusOid5 :tila "tallennettu" :nimi "Tietojenkäsittelytieteen perusopinnot" :sorakuvausId sorakuvausId :julkinen true :esikatselu true)
    (fixture/add-koulutus-mock koulutusOid6 :tila "tallennettu" :koulutustyyppi "yo" :nimi "Diplomi-insinööri" :sorakuvausId sorakuvausId :metadata fixture/yo-koulutus-metadata :esikatselu false)
    (fixture/add-koulutus-mock lukio-oid :tila "julkaistu" :koulutustyyppi "lk" :nimi "Lukio" :metadata fixture/lukio-koulutus-metadata :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock kk-koulutus-oid :tila "julkaistu" :koulutustyyppi "yo" :nimi "Korkeakoulu" :metadata fixture/yo-koulutus-metadata :sorakuvausId sorakuvausId)

     ;; Punkaharjun ja Helsingin yliopistoihin kiinnitetyt koulutukset
    (fixture/add-koulutus-mock traktoriala-oid :koulutustyyppi "amm" :tila "julkaistu" :nimi "Traktorialan koulutus" :tarjoajat [punkaharjun-yliopisto helsingin-yliopisto] :metadata koulutus-metatieto :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock hevosala-oid :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hevosalan koulutus" :modified "2018-05-05T12:02:23" :tarjoajat [punkaharjun-yliopisto helsingin-yliopisto] :metadata koulutus-metatieto :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock traktoriala-oid2 :koulutustyyppi "amm" :tila "julkaistu" :nimi "Traktorialan koulutus" :metadata koulutus-metatieto :sorakuvausId sorakuvausId :tarjoajat [])
    (fixture/add-koulutus-mock hevostutkinnon-osa-oid :koulutustyyppi "amm-tutkinnon-osa" :koulutuksetKoodiUri nil :ePerusteId nil :tila "julkaistu" :johtaaTutkintoon false :nimi "Hevosalan tutkinnon osa koulutus" :tarjoajat [punkaharjun-yliopisto] :sorakuvausId sorakuvausId :metadata fixture/amm-tutkinnon-osa-koulutus-metadata)
    (fixture/add-koulutus-mock hevososaamisala-oid :koulutustyyppi "amm-osaamisala" :tila "julkaistu" :johtaaTutkintoon false :nimi "Hevosalan osaamisala koulutus" :tarjoajat [punkaharjun-yliopisto] :metadata fixture/amm-osaamisala-koulutus-metadata :sorakuvausId sorakuvausId )
    (fixture/add-koulutus-mock yo-koulutus-oid :tila "julkaistu" :koulutustyyppi "yo" :nimi "Arkkitehti" :tarjoajat [punkaharjun-yliopisto] :metadata fixture/yo-koulutus-metadata :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock amk-oid :tila "julkaistu" :koulutustyyppi "amk" :nimi "Artenomi" :tarjoajat [punkaharjun-yliopisto] :metadata fixture/amk-koulutus-metadata :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock lukio-oid2 :tila "julkaistu" :koulutustyyppi "lk" :nimi "Lukio" :tarjoajat [punkaharjun-yliopisto] :metadata fixture/lukio-koulutus-metadata :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock amm-muu-oid :tila "julkaistu" :koulutustyyppi "amm-muu" :nimi "Muu ammatillinen" :tarjoajat [punkaharjun-yliopisto] :metadata fixture/amm-muu-koulutus-metadata :sorakuvausId sorakuvausId)

    (fixture/add-koulutus-mock aakkostus-koulutus-oid1 :koulutustyyppi "aikuisten-perusopetus" :tila "julkaistu" :nimi "Aakkosissa ensimmäinen"   :tarjoajat aakkostus-oppilaitos-oid1
                               :sorakuvausId sorakuvausId :metadata fixture/aikuisten-perusopetus-koulutus-metadata)
    (fixture/add-koulutus-mock aakkostus-koulutus-oid2 :koulutustyyppi "aikuisten-perusopetus" :tila "julkaistu" :nimi "Aakkosissa toinen"        :tarjoajat aakkostus-oppilaitos-oid2
                               :sorakuvausId sorakuvausId :metadata fixture/aikuisten-perusopetus-koulutus-metadata)
    (fixture/add-koulutus-mock aakkostus-koulutus-oid3 :koulutustyyppi "aikuisten-perusopetus" :tila "julkaistu" :nimi "Aakkosissa vasta kolmas"  :tarjoajat aakkostus-oppilaitos-oid3
                               :sorakuvausId sorakuvausId :metadata fixture/aikuisten-perusopetus-koulutus-metadata)
    (fixture/add-koulutus-mock aakkostus-koulutus-oid4 :koulutustyyppi "aikuisten-perusopetus" :tila "julkaistu" :nimi "Aakkosissa vasta neljäs"  :tarjoajat aakkostus-oppilaitos-oid4
                               :sorakuvausId sorakuvausId :metadata fixture/aikuisten-perusopetus-koulutus-metadata)
    (fixture/add-koulutus-mock aakkostus-koulutus-oid5 :koulutustyyppi "aikuisten-perusopetus" :tila "julkaistu" :nimi "Aakkosissa viidentenä"    :tarjoajat aakkostus-oppilaitos-oid5
                               :sorakuvausId sorakuvausId :metadata fixture/aikuisten-perusopetus-koulutus-metadata)

    (fixture/add-koulutus-mock keyword-koulutus-oid1  :koulutustyyppi "yo"  :tila "julkaistu" :tarjoajat oppilaitosOid1 :nimi "Lääketieteen koulutus" :metadata fixture/yo-koulutus-metadata :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid2  :koulutustyyppi "yo"  :tila "julkaistu" :tarjoajat oppilaitosOid2 :nimi "Humanistinen koulutus" :metadata fixture/yo-koulutus-metadata :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid3  :koulutustyyppi "yo"  :tila "julkaistu" :tarjoajat oppilaitosOid3 :nimi "Tietojenkäsittelytieteen koulutus" :metadata fixture/yo-koulutus-metadata :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid4  :koulutustyyppi "amm" :tila "julkaistu" :tarjoajat oppilaitosOid4 :nimi "Automaatiotekniikka (ylempi AMK :sorakuvausId sorakuvaus-id)" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid5  :koulutustyyppi "amm" :tila "julkaistu" :tarjoajat oppilaitosOid5 :nimi "Muusikon koulutus" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid6  :koulutustyyppi "amm" :tile "julkaistu" :tarjoajat oppilaitosOid6 :nimi "Sosiaali- ja terveysalan perustutkinto" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid7  :koulutustyyppi "amm" :tile "julkaistu" :nimi "Maanmittausalan perustutkinto" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid8  :koulutustyyppi "amm" :tile "julkaistu" :nimi "Pintakäsittelyalan perustutkinto" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid9  :koulutustyyppi "amm" :tile "julkaistu" :nimi "Puhtaus- ja kiinteistöpalvelualan ammattitutkinto" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid10 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Puhevammaisten tulkkauksen erikoisammattitutkinto" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid11 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Hius- ja kauneudenhoitoalan perustutkinto" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid12 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Autoalan perustutkinto" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid13 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Elintarvikealan perustutkinto" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid14 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Eläintenhoidon ammattitutkinto" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid15 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Tieto- ja viestintätekniikan ammattitutkinto" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid16 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Tanssialan perustutkinto" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid17 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Hevostalouden perustutkinto" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid17 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Hevostalouden perustutkinto" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock keyword-koulutus-oid18 :koulutustyyppi "amk" :tile "julkaistu" :nimi "Moottorialan perustutkinto" :sorakuvausId sorakuvausId)


    (fixture/add-toteutus-mock toteutusOid1       koulutusOid3 :tila "julkaistu" :nimi "Traktorialan alkuopinnot" :metadata amm-toteutus-metatieto)
    (fixture/add-toteutus-mock toteutusOid2       koulutusOid3 :tila "julkaistu" :nimi "Pneumatiikan alkuopinnot" :metadata amm-toteutus-metatieto)
    (fixture/add-toteutus-mock toteutusOid3       koulutusOid3 :tila "julkaistu" :nimi "Traktorialan alkuopinnot" :modified "2018-05-05T12:02:23" :muokkaaja "1.2.246.562.24.55555555555" :metadata amm-toteutus-metatieto)
    (fixture/add-toteutus-mock toteutusOid4       koulutusOid3  :tila "arkistoitu"  :nimi "Traktorialan alkuopinnot" :modified "2018-06-05T12:02:23" :esikatselu true)
    (fixture/add-toteutus-mock toteutusOid5       koulutusOid3  :tila "tallennettu" :nimi "Traktorialan alkuopinnot" :modified "2018-06-05T12:02:23" :esikatselu true)
    (fixture/add-toteutus-mock toteutusOid6       koulutusOid3  :tila "tallennettu" :nimi "Traktorialan alkuopinnot" :modified "2018-06-05T12:02:23" :esikatselu false)

    (fixture/add-toteutus-mock lukio-toteutus-oid lukio-oid     :tila "julkaistu" :nimi "Lukio" :modified "2018-06-05T12:02:23" :metadata fixture/lk-toteutus-metadata)
    (fixture/add-toteutus-mock kk-toteutus-oid    kk-koulutus-oid     :tila "julkaistu" :nimi "Korkeakoulu" :modified "2018-06-05T12:02:23" :metadata yo-toteutus-metatieto)

     ;; Punkaharjun ja Helsingin yliopistoihin kiinnitetyt toteutukset
    (fixture/add-toteutus-mock ponikoulu-oid hevosala-oid               :koulutustyyppi "amm"             :tila "julkaistu" :nimi "Ponikoulu" :tarjoajat [punkaharjun-toimipiste-2] :metadata oletus-toteutus-metatieto)
    (fixture/add-toteutus-mock valtrakoulu-oid traktoriala-oid          :koulutustyyppi "amm"             :tila "julkaistu" :nimi "Valtrakoulutus" :tarjoajat [punkaharjun-toimipiste-2] :metadata oletus-toteutus-metatieto)
    (fixture/add-toteutus-mock massikkakoulu-oid traktoriala-oid        :koulutustyyppi "amk"             :tila "julkaistu" :nimi "Massikkakoulutus" :tarjoajat [helsingin-toimipiste] :metadata amk-toteutus-metatieto)
    (fixture/add-toteutus-mock zetorkoulu-oid traktoriala-oid2          :koulutustyyppi "amm"             :tila "julkaistu" :nimi "Zetorkoulutus" :tarjoajat [punkaharjun-toimipiste-2] :metadata oletus-toteutus-metatieto)
    (fixture/add-toteutus-mock poniosatoteutus-oid hevososaamisala-oid  :koulutustyyppi "amm-osaamisala"  :tila "julkaistu" :nimi "Ponikoulu tutkinnon osa" :tarjoajat [punkaharjun-toimipiste-2] :metadata amm-osaamisala-toteutus-metatieto)

    (fixture/add-toteutus-mock aakkostus-toteutus-oid1 aakkostus-koulutus-oid1 :tila "julkaistu" :nimi "Aakkosissa ensimmäinen toteutus" :tarjoajat aakkostus-oppilaitos-oid1 :metadata aikuisten-perusopetus-toteutus-metatieto)
    (fixture/add-toteutus-mock aakkostus-toteutus-oid2 aakkostus-koulutus-oid2 :tila "julkaistu" :nimi "Aakkosissa toinen toteutus" :tarjoajat aakkostus-oppilaitos-oid2 :metadata aikuisten-perusopetus-toteutus-metatieto)
    (fixture/add-toteutus-mock aakkostus-toteutus-oid3 aakkostus-koulutus-oid3 :tila "julkaistu" :nimi "Aakkosissa vasta kolmas toteutus" :tarjoajat aakkostus-oppilaitos-oid3 :metadata aikuisten-perusopetus-toteutus-metatieto)
    (fixture/add-toteutus-mock aakkostus-toteutus-oid4 aakkostus-koulutus-oid4 :tila "julkaistu" :nimi "Aakkosissa vasta neljäs toteutus" :tarjoajat aakkostus-oppilaitos-oid4 :metadata aikuisten-perusopetus-toteutus-metatieto)
    (fixture/add-toteutus-mock aakkostus-toteutus-oid5 aakkostus-koulutus-oid5 :tila "julkaistu" :nimi "Aakkosissa viides toteutus" :tarjoajat aakkostus-oppilaitos-oid5 :metadata aikuisten-perusopetus-toteutus-metatieto)

    (fixture/add-toteutus-mock keyword-toteutus-oid1 keyword-koulutus-oid1  :koulutustyyppi "yo"  :tila "julkaistu" :tarjoajat oppilaitosOid1 :metadata {:tyyppi "yo" :ammattinimikkeet [{:kieli "fi" :arvo "lääkäri"}, {:kieli "fi" :arvo "esimies"}]})
    (fixture/add-toteutus-mock keyword-toteutus-oid2 keyword-koulutus-oid2  :koulutustyyppi "yo"  :tila "julkaistu" :tarjoajat oppilaitosOid2 :metadata {:tyyppi "yo" :ammattinimikkeet [{:kieli "fi" :arvo "psykologi"}] :asiasanat [{:kieli "fi" :arvo "ammattikorkeakoulu"}]})
    (fixture/add-toteutus-mock keyword-toteutus-oid3 keyword-koulutus-oid4  :koulutustyyppi "amm" :tila "julkaistu" :tarjoajat oppilaitosOid4 :metadata {:tyyppi "amm" :ammattinimikkeet [{:kieli "fi" :arvo "automaatioinsinööri"}] :asiasanat [{:kieli "fi" :arvo "ammattioppilaitos"}]})
    (fixture/add-toteutus-mock keyword-toteutus-oid4 keyword-koulutus-oid5  :koulutustyyppi "amm" :tila "julkaistu" :tarjoajat oppilaitosOid5 :metadata {:tyyppi "amm" :asiasanat [{:kieli "fi" :arvo "musiikkioppilaitokset"}]})
    (fixture/add-toteutus-mock keyword-toteutus-oid5 keyword-koulutus-oid8  :koulutustyyppi "amm" :tila "julkaistu" :metadata {:tyyppi "amm" :ammattinimikkeet [{:kieli "fi" :arvo "maalari"}, {:kieli "fi" :arvo "merimies"}]})
    (fixture/add-toteutus-mock keyword-toteutus-oid6 keyword-koulutus-oid12 :koulutustyyppi "amm" :tila "julkaistu" :metadata {:tyyppi "amm" :ammattinimikkeet [{:kieli "fi" :arvo "automaalari"}]})
    (fixture/add-toteutus-mock keyword-toteutus-oid7 keyword-koulutus-oid14 :koulutustyyppi "amm" :tila "julkaistu" :metadata {:tyyppi "amm" :ammattinimikkeet [{:kieli "fi" :arvo "eläintenhoitaja"}]})
    (fixture/add-toteutus-mock keyword-toteutus-oid8 keyword-koulutus-oid17 :koulutustyyppi "amm" :tila "julkaistu" :metadata {:tyyppi "amm" :ammattinimikkeet [{:kieli "fi" :arvo "hevostenhoitaja"}, {:kieli "fi" :arvo "seppä"}]})
    (fixture/add-toteutus-mock keyword-toteutus-oid9 keyword-koulutus-oid18 :koulutustyyppi "amk" :tila "julkaistu" :metadata {:tyyppi "amk" :ammattinimikkeet [{:kieli "fi" :arvo "virittäjä"}]})


    (fixture/add-haku-mock hakuOid1     :tila "julkaistu"   :nimi "Hevoshaku" :muokkaaja "1.2.246.562.24.62301161440" :hakutapaKoodiUri "hakutapa_03#1")
    (fixture/add-haku-mock hakuOid2     :tila "julkaistu"   :nimi "Yhteishaku" :muokkaaja "1.2.246.562.24.62301161440" :hakutapaKoodiUri "hakutapa_01#1")
    (fixture/add-haku-mock hakuOid3     :tila "julkaistu"   :nimi "Jatkuva haku" :modified "2018-05-05T12:02:23" :muokkaaja "1.2.246.562.24.55555555555")
    (fixture/add-haku-mock hakuOid4     :tila "arkistoitu"  :nimi "Jatkuva haku" :modified "2018-06-05T12:02:23")
    (fixture/add-haku-mock hakuOid5     :tila "tallennettu" :nimi "Jatkuva haku" :modified "2018-06-05T12:02:23" :esikatselu false)
    (fixture/add-haku-mock kk-haku-oid  :tila "julkaistu"   :nimi "KK-haku" :muokkaaja "1.2.246.562.24.62301161440" :hakutapaKoodiUri "hakutapa_01#1")


    (fixture/add-valintaperuste-mock valintaperusteId1 :tila "julkaistu" :nimi "Valintaperustekuvaus" :organisaatio mocks/Oppilaitos2)
    (fixture/add-valintaperuste-mock valintaperusteId2 :tila "julkaistu" :nimi "Valintaperuste" :muokkaaja "1.2.246.562.24.62301161440")
    (fixture/add-valintaperuste-mock valintaperusteId3 :tila "julkaistu" :nimi "Kiva valintaperustekuvaus" :modified "2018-05-05T12:02:23" :muokkaaja "1.2.246.562.24.55555555555")
    (fixture/add-valintaperuste-mock valintaperusteId4 :tila "arkistoitu" :nimi "Kiva valintaperustekuvaus" :modified "2018-06-05T12:02:23")
    (fixture/add-valintaperuste-mock valintaperusteId5 :tila "tallennettu" :esikatselu false :nimi "Kiva valintaperustekuvaus" :modified "2018-06-05T12:02:23")
    (fixture/add-valintaperuste-mock valintaperusteId6 :tila "tallennettu" :esikatselu true :nimi "Kiva valintaperustekuvaus" :modified "2018-06-05T12:02:23")


    (fixture/add-hakukohde-mock hakukohdeOid1 toteutusOid1 hakuOid1 :tila "julkaistu" :esitysnimi "Hakukohde" :valintaperuste valintaperusteId1 :organisaatio mocks/Oppilaitos2)
    (fixture/add-hakukohde-mock hakukohdeOid2 toteutusOid4 hakuOid1 :tila "julkaistu" :esitysnimi "Hakukohde" :valintaperuste valintaperusteId5)
    (fixture/add-hakukohde-mock hakukohdeOid3 toteutusOid2 hakuOid1 :tila "julkaistu" :esitysnimi "autoalan hakukohde" :valintaperuste valintaperusteId1 :modified "2018-05-05T12:02:23" :muokkaaja "1.2.246.562.24.55555555555")
    (fixture/add-hakukohde-mock hakukohdeOid4 toteutusOid5 hakuOid1 :tila "arkistoitu" :esitysnimi "Autoalan hakukohde" :valintaperuste valintaperusteId1 :modified "2018-06-05T12:02:23")
    (fixture/add-hakukohde-mock hakukohdeOid5 toteutusOid5 hakuOid1 :tila "tallennettu" :esitysnimi "Autoalan hakukohde" :valintaperuste valintaperusteId6 :modified "2018-06-05T12:02:23" :esikatselu true :hakuaikaPaattyy "2100-04-14T09:58")
    (fixture/add-hakukohde-mock hakukohdeOid6 toteutusOid5 hakuOid1 :tila "tallennettu" :esitysnimi "Autoalan hakukohde" :valintaperuste valintaperusteId6 :modified "2018-06-05T12:02:23" :esikatselu false)
    (fixture/add-hakukohde-mock hakukohdeOid7 toteutusOid1 hakuOid1 :tila "tallennettu" :nimi "Hakukohde" :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId1 :esikatselu false)
    (fixture/add-hakukohde-mock hakukohdeOid8 ponikoulu-oid hakuOid1 :tila "julkaistu"  :nimi "ponikoulun hakukohde" :muokkaaja "1.2.246.562.24.62301161440" :hakuaikaAlkaa "2000-01-01T00:00" :hakuaikaPaattyy "2100-01-01T00:00" :valintaperuste valintaperusteId2)
    (fixture/add-hakukohde-mock hakukohdeOid9 poniosatoteutus-oid hakuOid1 :tila "julkaistu"  :nimi "ponikoulun hakukohde" :muokkaaja "1.2.246.562.24.62301161440" :hakuaikaAlkaa "2000-01-01T00:00" :hakuaikaPaattyy "2000-01-02T00:00" :valintaperuste valintaperusteId2)
    (fixture/add-hakukohde-mock hakukohdeOid10 ponikoulu-oid hakuOid2 :tila "julkaistu"  :nimi "ponikoulun yhteishakukohde" :muokkaaja "1.2.246.562.24.62301161440" :hakuaikaAlkaa "2000-01-01T00:00" :hakuaikaPaattyy "2100-01-01T00:00" :valintaperuste valintaperusteId2)
    (fixture/add-hakukohde-mock kk-hakukohde-oid kk-toteutus-oid kk-haku-oid :tila "julkaistu" :esitysnimi "KK-hakukohde")


    (fixture/add-oppilaitos-mock oppilaitosOid1 :tila "julkaistu" :organisaatio oppilaitosOid1)
    (fixture/add-oppilaitos-mock oppilaitosOid2 :tila "julkaistu" :organisaatio oppilaitosOid2)
    (fixture/add-oppilaitos-mock oppilaitosOid3 :tila "tallennettu" :esikatselu false :organisaatio oppilaitosOid2)
    (fixture/add-oppilaitos-mock oppilaitosOid4 :tila "tallennettu" :esikatselu true :organisaatio oppilaitosOid4)
    (fixture/add-oppilaitos-mock oppilaitosOid7 :tila "julkaistu" :organisaatio oppilaitosOid7)


    (fixture/add-oppilaitoksen-osa-mock oppilaitoksenOsaOid1 oppilaitosOid1 :tila "julkaistu" :organisaatio oppilaitoksenOsaOid1)
    (fixture/add-oppilaitoksen-osa-mock oppilaitoksenOsaOid2 oppilaitosOid1 :tila "arkistoitu" :organisaatio oppilaitoksenOsaOid2)
    (fixture/add-oppilaitoksen-osa-mock oppilaitoksenOsaOid3 oppilaitosOid2 :tila "julkaistu" :organisaatio oppilaitoksenOsaOid3)
    (fixture/add-oppilaitoksen-osa-mock oppilaitoksenOsaOid4 oppilaitosOid2 :tila "tallennettu" :esikatselu true :organisaatio oppilaitoksenOsaOid4)
    (fixture/add-oppilaitoksen-osa-mock oppilaitoksenOsaOid5 oppilaitosOid2 :tila "tallennettu" :esikatselu false :organisaatio oppilaitoksenOsaOid5)

                 (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5 koulutusOid6 lukio-oid kk-koulutus-oid
                                                                            aakkostus-koulutus-oid1 aakkostus-koulutus-oid2 aakkostus-koulutus-oid3 aakkostus-koulutus-oid3 aakkostus-koulutus-oid4 aakkostus-koulutus-oid5
                                                                            keyword-koulutus-oid1 keyword-koulutus-oid2 keyword-koulutus-oid3 keyword-koulutus-oid4 keyword-koulutus-oid5 keyword-koulutus-oid6 keyword-koulutus-oid7
                                                                            keyword-koulutus-oid8 keyword-koulutus-oid9 keyword-koulutus-oid10 keyword-koulutus-oid11 keyword-koulutus-oid12 keyword-koulutus-oid13
                                                                            keyword-koulutus-oid14 keyword-koulutus-oid15 keyword-koulutus-oid16 keyword-koulutus-oid17 keyword-koulutus-oid18]
                                     :toteutukset [toteutusOid1 toteutusOid2 toteutusOid3 toteutusOid4 toteutusOid5 toteutusOid6 lukio-toteutus-oid kk-toteutus-oid]
                                     :haut [hakuOid1 hakuOid2 hakuOid3 hakuOid4 hakuOid5 kk-haku-oid]
                                     :hakukohteet [hakukohdeOid1 hakukohdeOid2 hakukohdeOid3 hakukohdeOid4 hakukohdeOid5 hakukohdeOid6 hakukohdeOid7 kk-hakukohde-oid]
                                     :valintaperusteet [valintaperusteId1 valintaperusteId2 valintaperusteId3 valintaperusteId4 valintaperusteId5 valintaperusteId6]
                                     :oppilaitokset [oppilaitosOid1 oppilaitosOid2 oppilaitosOid3 oppilaitosOid4 oppilaitosOid5 oppilaitosOid6 oppilaitosOid7
                                                     aakkostus-oppilaitos-oid1 aakkostus-oppilaitos-oid2 aakkostus-oppilaitos-oid3 aakkostus-oppilaitos-oid4 aakkostus-oppilaitos-oid5]})

                 ;; Punkaharjun ja Helsingin yliopistoihin kiinnitetyt koulutukset
                 (fixture/index-oids-without-related-indices {:koulutukset [traktoriala-oid hevosala-oid traktoriala-oid2 hevostutkinnon-osa-oid hevososaamisala-oid yo-koulutus-oid amk-oid lukio-oid2 amm-muu-oid]
                                                              :oppilaitokset [punkaharjun-yliopisto helsingin-yliopisto]} orgs)
    (export-elastic-data))
)
