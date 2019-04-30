(ns kouta-indeksoija-service.kouta-koulutus-search-test
  (:require [midje.sweet :refer :all]
            [kouta-indeksoija-service.kouta.tools.hakuaika :refer [->real-hakuajat]]))

  (facts "Hakuajat"

    (let [hakuaika1     {:alkaa "2031-04-02T12:00" :paattyy "2031-05-02T12:00"}
          hakuaika2     {:alkaa "2032-04-02T12:00" :paattyy "2032-05-02T12:00"}
          hakuaika3     {:alkaa "2033-04-02T12:00" :paattyy "2033-05-02T12:00"}
          hakuaika4     {:alkaa "2034-04-02T12:00" :paattyy "2034-05-02T12:00"}

          expected1     {:gte "2031-04-02T12:00" :lt "2031-05-02T12:00"}
          expected2     {:gte "2032-04-02T12:00" :lt "2032-05-02T12:00"}
          expected3     {:gte "2033-04-02T12:00" :lt "2033-05-02T12:00"}
          expected4     {:gte "2034-04-02T12:00" :lt "2034-05-02T12:00"}

          haku1         {:hakuajat [hakuaika1, hakuaika2] :hakukohteet []}
          haku2         {:hakuajat [hakuaika3, hakuaika4] :hakukohteet []}
          haku          {:hakuajat []                     :hakukohteet []}
          hakukohde1    {:hakuajat [hakuaika1, hakuaika2] :kaytetaanHaunAikataulua false}
          hakukohde2    {:hakuajat [hakuaika3]            :kaytetaanHaunAikataulua false}
          hakukohde3    {:hakuajat [hakuaika4]            :kaytetaanHaunAikataulua false}]

      (fact "should contain hakujen hakuajat"
        (->real-hakuajat {:haut [haku1, haku2]})
          => (just [expected1, expected2, expected3, expected4] :in-any-order))

      (fact "should contain hakukohteiden hakuajat"
        (->real-hakuajat {:haut [(merge haku {:hakukohteet [hakukohde1, hakukohde2]}),
                                 (merge haku {:hakukohteet [hakukohde3]})]})
          => (just [expected1, expected2, expected3, expected4] :in-any-order))

      (fact "should ignore haun hakuajat if not used"
        (->real-hakuajat {:haut [(merge haku1 {:hakukohteet [hakukohde2]})]})
          => (just [expected3] :in-any-order))

      (fact "should ignore hakukohteen hakuajat if not used"
        (->real-hakuajat {:haut [(merge haku1 {:hakukohteet [(merge hakukohde2 {:kaytetaanHaunAikataulua true})]})]})
          => (just [expected1, expected2] :in-any-order))

      (fact "should remove duplicates"
        (->real-hakuajat {:haut [haku1 (merge haku (:hakukohteet hakukohde1)) haku1]})
          => (just [expected1, expected2] :in-any-order))))
