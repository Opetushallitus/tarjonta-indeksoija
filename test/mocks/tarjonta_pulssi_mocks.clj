(ns mocks.tarjonta-pulssi-mocks
  (:require
   [clj-log.access-log]
   [clojure.string :as str]
   [mocks.export-elastic-data :refer [export-elastic-data]]
   [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
   [clj-test-utils.elasticsearch-docker-utils :as ed-utils]))

(defonce koulutus-oid-template "1.2.246.562.13.00000000000000000000")
(defonce toteutus-oid-template "1.2.246.562.17.00000000000000000000")
(defonce haku-oid-template "1.2.246.562.29.00000000000000000000")
(defonce hakukohde-oid-template "1.2.246.562.20.00000000000000000000")

(defn- oid
  [template oidNbr]
  (let [lastPart (last (str/split template #"\."))
        padWithLeadingZerosFormat (str "%0" (count lastPart) "d")
        newLastPart (format padWithLeadingZerosFormat oidNbr)]
    (str/replace template lastPart newLastPart)))

(defn- add-entities
  ([oidCounter nbr koulutustyyppi tila]
   (add-entities oidCounter nbr koulutustyyppi tila nil {:tyyppi koulutustyyppi}))

  ([oidCounter nbr koulutustyyppi tila koodiUri]
   (add-entities oidCounter nbr koulutustyyppi tila koodiUri {:tyyppi koulutustyyppi}))

  ([oidCounter nbr koulutustyyppi tila koodiUri metadata]
  (dotimes [i nbr]
    (let [oidVal (+ oidCounter i)
          koulutusOid (oid koulutus-oid-template oidVal)
          toteutusOid (oid toteutus-oid-template oidVal)
          hakukohdeOid (oid hakukohde-oid-template oidVal)]
    (if koodiUri
      (fixture/add-koulutus-mock koulutusOid
                                 :koulutustyyppi koulutustyyppi :tila tila :koulutuksetKoodiUri koodiUri
                                 :metadata metadata)
      (fixture/add-koulutus-mock koulutusOid
                                 :koulutustyyppi koulutustyyppi :tila tila :metadata metadata))
    (fixture/add-toteutus-mock toteutusOid koulutusOid :tila tila)
    (fixture/add-hakukohde-mock hakukohdeOid toteutusOid "1.2.246.562.29.00000000000000000001" :tila tila)))
  (+ oidCounter nbr)))

(defn- add-haut
  [oidCounter nbr hakutapa tila]
  (dotimes [i nbr]
    (fixture/add-haku-mock (oid haku-oid-template (+ oidCounter i)) :tila tila :hakutapaKoodiUri hakutapa))
  (+ oidCounter nbr))

(defn -main []
  (ed-utils/start-elasticsearch)
  (fixture/init)
  (let [lastHakuOidNumber (-> 1
                              (add-haut 1 "hakutapa_01" "julkaistu")
                              (add-haut 1 "hakutapa_01" "arkistoitu")
                              (add-haut 2 "hakutapa_02" "julkaistu")
                              (add-haut 1 "hakutapa_02" "arkistoitu")
                              (add-haut 3 "hakutapa_03" "julkaistu")
                              (add-haut 2 "hakutapa_03" "arkistoitu")
                              (add-haut 4 "hakutapa_04" "julkaistu")
                              (add-haut 3 "hakutapa_04" "arkistoitu")
                              (add-haut 5 "hakutapa_05" "julkaistu")
                              (add-haut 4 "hakutapa_05" "arkistoitu")
                              (add-haut 6 "hakutapa_06" "julkaistu")
                              (add-haut 5 "hakutapa_06" "arkistoitu"))
        lastNumber (-> 1
                       (add-entities 1 "amm" "julkaistu" "koulutustyyppi_1")
                       (add-entities 1 "amm" "arkistoitu" "koulutustyyppi_26")
                       (add-entities 2 "amm" "julkaistu" "koulutustyyppi_11")
                       (add-entities 1 "amm" "arkistoitu" "koulutustyyppi_11")
                       (add-entities 3 "amm" "julkaistu" "koulutustyyppi_12")
                       (add-entities 1 "amm" "arkistoitu" "koulutustyyppi_12")
                       (add-entities 4 "amm" "julkaistu")
                       (add-entities 1 "amm" "arkistoitu")
                       (add-entities 5 "aikuisten-perusopetus" "julkaistu")
                       (add-entities 2 "aikuisten-perusopetus" "arkistoitu")
                       (add-entities 6 "telma" "julkaistu")
                       (add-entities 3 "telma" "arkistoitu")
                       (add-entities 7 "amm-osaamisala" "julkaistu")
                       (add-entities 4 "amm-osaamisala" "arkistoitu")
                       (add-entities 8 "amm-tutkinnon-osa" "julkaistu")
                       (add-entities 5 "amm-tutkinnon-osa" "arkistoitu")
                       (add-entities 9 "amm-muu" "julkaistu")
                       (add-entities 6 "amm-muu" "arkistoitu")
                       (add-entities 10 "amk" "julkaistu")
                       (add-entities 7 "amk" "arkistoitu")
                       (add-entities 11 "yo" "julkaistu")
                       (add-entities 8 "yo" "arkistoitu")
                       (add-entities 12 "amm-ope-erityisope-ja-opo" "julkaistu")
                       (add-entities 9 "amm-ope-erityisope-ja-opo" "arkistoitu")
                       (add-entities 13 "kk-opintojakso" "julkaistu")
                       (add-entities 10 "kk-opintojakso" "arkistoitu")
                       (add-entities 14 "kk-opintojakso" "julkaistu" nil {:tyyppi "opintojakso" :isAvoinKorkeakoulutus true})
                       (add-entities 11 "kk-opintojakso" "arkistoitu" nil {:tyyppi "opintojakso" :isAvoinKorkeakoulutus true})
                       (add-entities 15 "kk-opintokokonaisuus" "julkaistu")
                       (add-entities 12 "kk-opintokokonaisuus" "arkistoitu")
                       (add-entities 16 "kk-opintokokonaisuus" "julkaistu" nil {:tyyppi "opintojakso" :isAvoinKorkeakoulutus true})
                       (add-entities 13 "kk-opintokokonaisuus" "arkistoitu" nil {:tyyppi "opintojakso" :isAvoinKorkeakoulutus true})
                       (add-entities 17 "erikoislaakari" "julkaistu")
                       (add-entities 14 "erikoislaakari" "arkistoitu")
                       (add-entities 18 "erikoistumiskoulutus" "julkaistu")
                       (add-entities 15 "erikoistumiskoulutus" "arkistoitu")
                       (add-entities 19 "ope-pedag-opinnot" "julkaistu")
                       (add-entities 16 "ope-pedag-opinnot" "arkistoitu")
                       (add-entities 20 "lk" "julkaistu")
                       (add-entities 17 "lk" "arkistoitu")
                       (add-entities 21 "vapaa-sivistystyo-opistovuosi" "julkaistu")
                       (add-entities 18 "vapaa-sivistystyo-opistovuosi" "arkistoitu")
                       (add-entities 22 "tuva" "julkaistu")
                       (add-entities 19 "tuva" "arkistoitu")
                       (add-entities 23 "taiteen-perusopetus" "julkaistu")
                       (add-entities 20 "taiteen-perusopetus" "arkistoitu"))
        koulutusOids (vec (map #(oid koulutus-oid-template (+ % 1)) (range (- lastNumber 1))))
        toteutusOids (vec (map #(oid toteutus-oid-template (+ % 1)) (range (- lastNumber 1))))
        hakukohdeOids (vec (map #(oid hakukohde-oid-template (+ % 1)) (range (- lastNumber 1))))
        hakuOids (vec (map #(oid haku-oid-template (+ % 1)) (range (- lastHakuOidNumber 1))))]
    (fixture/index-oids-without-related-indices {:koulutukset koulutusOids
                                                 :toteutukset toteutusOids
                                                 :haut hakuOids
                                                 :hakukohteet hakukohdeOids}))
  (export-elastic-data "tarjonta-pulssi")
  (ed-utils/stop-elasticsearch)
  )



