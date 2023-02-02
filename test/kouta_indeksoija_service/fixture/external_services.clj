(ns kouta-indeksoija-service.fixture.external-services
  (:require [clojure.string :as string]
            [kouta-indeksoija-service.fixture.common-oids :refer :all]))

(def Oppilaitos2 "1.2.246.562.10.55555555555")

(defn mock-koodisto
  ([koodisto koodi-uri]
   (locking koodi-uri
     (if koodi-uri
       {:koodiUri koodi-uri :nimi {:fi (str koodi-uri " nimi fi") :sv (str koodi-uri " nimi sv")}})))
  ([koodi-uri]
   (locking koodi-uri
     (if koodi-uri
       (mock-koodisto (subs koodi-uri 0 (string/index-of koodi-uri "_")) koodi-uri)))))

(defn mock-alakoodit
  [koodi-uri alakoodi-uri]
  (cond
    (= "koulutus_222336#1" koodi-uri)
    (vector {:koodiUri "koulutustyyppiabc_01" :nimi {:fi (str koodi-uri "_01" " nimi fi") :sv (str koodi-uri "_01" " nimi sv")}})
    (= "koulutus_222337#1" koodi-uri)
    (vector {:koodiUri "koulutustyyppiabc_01" :nimi {:fi (str koodi-uri "_01" " nimi fi") :sv (str koodi-uri "_01" " nimi sv")} :tila "PASSIIVINEN"})
    :else (vector
            {:koodiUri (str alakoodi-uri "_01") :nimi {:fi (str alakoodi-uri "_01" " nimi fi") :sv (str alakoodi-uri "_01" " nimi sv")}}
            {:koodiUri (str alakoodi-uri "_02") :nimi {:fi (str alakoodi-uri "_02" " nimi fi") :sv (str alakoodi-uri "_02" " nimi sv")}})))

(defn mock-get-henkilo-nimi-with-cache
  [oid]
  (locking oid "Kalle Ankka"))

(defn mock-get-eperuste
  [id]
  {:id id
   :diaarinumero "1111-OPH-2021"
   :voimassaoloLoppuu 1514757600000
   :koulutukset [{:nimi {:fi (str "koulutus " id " nimi fi")
                         :sv (str "koulutus " id " nimi sv")}
                  :koulutuskoodiUri "koulutus_354345"}]
   :tutkintonimikkeet [{:tutkintonimikeUri "tutkintonimikkeet_01" :nimi {:fi "tutkintonimikkeet_01 nimi fi" :sv "tutkintonimikkeet_01 nimi sv"}}
                       {:tutkintonimikeUri "tutkintonimikkeet_02" :nimi {:fi "tutkintonimikkeet_02 nimi fi" :sv "tutkintonimikkeet_02 nimi sv"}}]
   :suoritustavat [{:laajuusYksikko "OSAAMISPISTE"
                    :rakenne {:muodostumisSaanto {:laajuus {:minimi 150 :maksimi 150}}
                              :osat [{:tunniste "osaamisala tunniste 1"
                                      :osaamisala {:nimi {:fi "Osaamisala 01 fi" :sv "Osaamisala 01 sv"}
                                                   :osaamisalakoodiArvo "01"
                                                   :osaamisalakoodiUri "osaamisala_01"}
                                      :muodostumisSaanto {:laajuus {:minimi 30 :maksimi 30}}}]}
                    :tutkinnonOsaViitteet [{:id 122, :laajuus 50, :jarjestys 1, :_tutkinnonOsa 1234}]}]
   :tutkinnonOsat [{:id 1234 :koodi {:nimi {:fi "tutkinnon osa 123 fi" :sv "tutkinnon osa 123 sv"}
                                     :uri "tutkinnonosat_12345"}}]
   :lops2019 {:oppiaineet [{:id 6835372 :moduulit [{:koodi {:uri "moduulikoodistolops2021_kald3"}
                                                    :tavoitteet {:kohde {:fi "Tavoitteet kohde fi" :sv "Tavoitteet kohde sv" :en "Tavoitteet kohde en"}
                                                                 :tavoitteet [{:fi "Tavoite 1 fi" :sv "Tavoite 1 sv"} {:fi "Tavoite 2 fi" :sv "Tavoite 2 sv"}]}
                                                    :sisallot [{:sisallot [{:fi "Sisalto 1 fi" :sv "Sisalto 1 sv"} {:fi "Sisalto 2 fi" :sv "Sisalto 2 sv"}]}]}]}]}})

(defn create-organisaatio-hierarkia
  [koulutustoimija oppilaitos oppilaitoksen-osat]
  {:numHits (+ 2 (count oppilaitoksen-osat))
   :organisaatiot [{:oid (:oid koulutustoimija)
                    :alkuPvm 313106400000
                    :parentOid "1.2.246.562.10.00000000001"
                    :parentOidPath (str (:oid koulutustoimija)  "/1.2.246.562.10.10101010100")
                    :nimi (or (:nimi koulutustoimija) {:fi (str "Koulutustoimija fi " (:oid koulutustoimija)), :sv (str "Koulutustoimija sv " (:oid koulutustoimija))})
                    :kieletUris (or (:kielet koulutustoimija) ["oppilaitoksenopetuskieli_1#1"])
                    :kotipaikkaUri (or (:kotipaikka koulutustoimija) "kunta_091")
                    :organisaatiotyypit ["organisaatiotyyppi_01"]
                    :status "AKTIIVINEN"
                    :children [{:oid (:oid oppilaitos)
                                :alkuPvm 725839200000
                                :parentOid (:oid koulutustoimija)
                                :parentOidPath (str (:oid oppilaitos) "/"  (:oid koulutustoimija)  "/1.2.246.562.10.10101010100")
                                :oppilaitosKoodi "00000"
                                :oppilaitostyyppi "oppilaitostyyppi_42#1"
                                :toimipistekoodi "00000"
                                :nimi (or (:nimi oppilaitos) {:fi (str "Oppilaitos fi " (:oid oppilaitos)), :sv (str "Oppilaitos sv " (:oid oppilaitos))})
                                :kieletUris (or (:kielet oppilaitos) ["oppilaitoksenopetuskieli_1#1", "oppilaitoksenopetuskieli_2#1"])
                                :kotipaikkaUri (or (:kotipaikka oppilaitos) "kunta_091")
                                :aliOrganisaatioMaara (count oppilaitoksen-osat)
                                :organisaatiotyypit ["organisaatiotyyppi_02"]
                                :status "AKTIIVINEN"
                                :children (vec (map #(let [toimipiste %] {:oid (:oid toimipiste)
                                                                          :alkuPvm 725839200000
                                                                          :parentOid (:oid oppilaitos)
                                                                          :parentOidPath (str (:oid toimipiste) "/" (:oid oppilaitos) "/"  (:oid koulutustoimija)  "/1.2.246.562.10.10101010100")
                                                                          :toimipistekoodi "00000"
                                                                          :nimi (or (:nimi toimipiste) {:fi (str "Toimipiste fi " (:oid toimipiste)), :sv (str "Toimipiste sv " (:oid toimipiste))})
                                                                          :kieletUris (or (:kielet toimipiste) ["oppilaitoksenopetuskieli_1#1", "oppilaitoksenopetuskieli_2#1"])
                                                                          :kotipaikkaUri (or (:kotipaikka toimipiste) "kunta_091")
                                                                          :aliOrganisaatioMaara 0
                                                                          :organisaatiotyypit ["organisaatiotyyppi_03"]
                                                                          :status "AKTIIVINEN"
                                                                          :children []}) oppilaitoksen-osat))}]}]})
(defn mock-get-osaamisalakuvaukset
  [eperuste-id eperuste-tila]
  [{
    :id 5697410
    :nimi {
           :fi "Osaamisala"
           :sv "Kompetensområdet"}
    :tila "valmis"
    :teksti {
             :fi "<p>Kuvaus suomeksi</p>"
             :sv "<p>Kuvaus ruotsiksi</p<"}
    :osaamisala {
                 :nimi {
                        :fi "Osaamisala"
                        :sv "Kompetensområdet"}
                 :uri "osaamisala_3157"
                 :koodisto "osaamisala"}}])
