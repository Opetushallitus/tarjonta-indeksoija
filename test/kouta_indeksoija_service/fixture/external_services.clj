(ns kouta-indeksoija-service.fixture.external-services)

(def Koulutustoimija "1.2.246.562.10.11111111111")
(def Oppilaitos1 "1.2.246.562.10.54545454545")
(def Toimipiste1OfOppilaitos1 "1.2.246.562.10.54545454511")
(def Toimipiste2OfOppilaitos1 "1.2.246.562.10.54545454522")
(def Oppilaitos2 "1.2.246.562.10.55555555555")
(def Toimipiste1OfOppilaitos2 "1.2.246.562.10.55555555511")

(defn mock-organisaatio
  [oid]
  (locking oid
    (condp = oid
      Oppilaitos1 { :nimi { :fi "Kiva ammattikorkeakoulu" :sv "Kiva ammattikorkeakoulu sv"} :oid oid :kotipaikkaUri "kunta_091"}
      Toimipiste1OfOppilaitos1 { :nimi { :fi "Kiva ammattikorkeakoulu, Helsingin toimipiste" :sv "Kiva ammattikorkeakoulu, Helsingin toimipiste sv"} :oid oid :kotipaikkaUri "kunta_091" }
      Toimipiste2OfOppilaitos1 { :nimi { :fi "Kiva ammattikorkeakoulu, Kuopion toimipiste" :sv "Kiva ammattikorkeakoulu, Kuopion toimipiste sv"} :oid oid :kotipaikkaUri "kunta_297" }
      Oppilaitos2 { :nimi { :fi "Toinen kiva ammattikorkeakoulu"} :oid oid :kotipaikkaUri "kunta_532" }
      { :nimi { :fi (str "Nimi " oid " fi") :en (str "Nimi " oid " en")} :oid oid :kotipaikkaUri "kunta_091" } )))

(defn mock-koodisto
  ([koodisto koodi-uri]
   (locking koodi-uri
    (if koodi-uri
      { :koodiUri koodi-uri :nimi {:fi (str koodi-uri " nimi fi") :sv (str koodi-uri " nimi sv")}})))
  ([koodi-uri]
   (locking koodi-uri
     (if koodi-uri
       (mock-koodisto (subs koodi-uri 0 (clojure.string/index-of koodi-uri "_")) koodi-uri)))))

(defn mock-alakoodit
  [koodi-uri alakoodi-uri]
  (vector
   { :koodiUri (str alakoodi-uri "_01") :nimi {:fi (str alakoodi-uri "_01" " nimi fi") :sv (str alakoodi-uri "_01" " nimi sv")}}
   { :koodiUri (str alakoodi-uri "_02") :nimi {:fi (str alakoodi-uri "_02" " nimi fi") :sv (str alakoodi-uri "_02" " nimi sv")}}))

(defn mock-get-henkilo-nimi-with-cache
  [oid]
  (locking oid "Kalle Ankka"))

(defn mock-get-eperuste
  [id]
  {:id id
   :koulutukset[ {:nimi {:fi (str "koulutus " id " nimi fi")
                         :sv (str "koulutus " id " nimi sv")} ,
                  :koulutuskoodiUri "koulutus_354345"}]
   :tutkintonimikkeet [{:tutkintonimikeUri "tutkintonimikkeet_01" :nimi {:fi "tutkintonimikkeet_01 nimi fi" :sv "tutkintonimikkeet_01 nimi sv"}}
                       {:tutkintonimikeUri "tutkintonimikkeet_02" :nimi {:fi "tutkintonimikkeet_02 nimi fi" :sv "tutkintonimikkeet_02 nimi sv"}}],
   :suoritustavat [{:laajuusYksikko "OSAAMISPISTE" :rakenne {:muodostumisSaanto {:laajuus {:minimi 150 :maksimi 150}}}}]})

(defn- oppilaitos1-hierarkia?
  [oid]
  (or (= Oppilaitos1 oid) (= Toimipiste1OfOppilaitos1 oid) (= Toimipiste2OfOppilaitos1 oid)))

(defn- oppilaitos2-hierarkia?
  [oid]
  (or (= Oppilaitos2 oid) (= Toimipiste1OfOppilaitos2 oid)))

(defn- get-oids
  [oid]
  (if (oppilaitos1-hierarkia? oid)
    [Koulutustoimija Oppilaitos1 [Toimipiste1OfOppilaitos1 Toimipiste2OfOppilaitos1]]
    (if (oppilaitos2-hierarkia? oid)
      [Koulutustoimija Oppilaitos2 [Toimipiste1OfOppilaitos2]]
      [(str oid "55") oid [(str oid "1"), (str oid "2"), (str oid "3")]])))

(defn create-organisaatio-hierarkia
  [koulutustoimija oppilaitos oppilaitoksen-osat]
  {:numHits (+ 2 (count oppilaitoksen-osat)),
   :organisaatiot [{:oid (:oid koulutustoimija),
                    :alkuPvm 313106400000,
                    :parentOid "1.2.246.562.10.00000000001",
                    :parentOidPath (str (:oid koulutustoimija)  "/1.2.246.562.10.10101010100"),
                    :nimi (or (:nimi koulutustoimija) { :fi (str "Koulutustoimija fi " (:oid koulutustoimija)), :sv (str "Koulutustoimija sv " (:oid koulutustoimija))})
                    :kieletUris (or (:kielet koulutustoimija) ["oppilaitoksenopetuskieli_1#1"]),
                    :kotipaikkaUri (or (:kotipaikka koulutustoimija) "kunta_091"),
                    :organisaatiotyypit [ "organisaatiotyyppi_01" ],
                    :status "AKTIIVINEN"
                    :children [{:oid (:oid oppilaitos),
                                :alkuPvm 725839200000,
                                :parentOid (:oid koulutustoimija),
                                :parentOidPath (str (:oid oppilaitos) "/"  (:oid koulutustoimija)  "/1.2.246.562.10.10101010100"),
                                :oppilaitosKoodi "00000",
                                :oppilaitostyyppi "oppilaitostyyppi_42#1",
                                :toimipistekoodi "00000",
                                :nimi (or (:nimi oppilaitos) { :fi (str "Oppilaitos fi " (:oid oppilaitos)), :sv (str "Oppilaitos sv " (:oid oppilaitos))})
                                :kieletUris (or (:kielet oppilaitos) [ "oppilaitoksenopetuskieli_1#1", "oppilaitoksenopetuskieli_2#1" ]),
                                :kotipaikkaUri (or (:kotipaikka oppilaitos) "kunta_091"),
                                :aliOrganisaatioMaara (count oppilaitoksen-osat),
                                :organisaatiotyypit [ "organisaatiotyyppi_02" ],
                                :status "AKTIIVINEN"
                                :children (vec (map #(let [toimipiste %] {:oid (:oid toimipiste),
                                                                          :alkuPvm 725839200000,
                                                                          :parentOid (:oid oppilaitos),
                                                                          :parentOidPath (str (:oid toimipiste) "/" (:oid oppilaitos) "/"  (:oid koulutustoimija)  "/1.2.246.562.10.10101010100"),
                                                                          :toimipistekoodi "00000",
                                                                          :nimi (or (:nimi toimipiste) { :fi (str "Toimipiste fi " (:oid toimipiste)), :sv (str "Toimipiste sv " (:oid toimipiste))})
                                                                          :kieletUris (or (:kielet toimipiste) [ "oppilaitoksenopetuskieli_1#1", "oppilaitoksenopetuskieli_2#1" ]),
                                                                          :kotipaikkaUri (or (:kotipaikka toimipiste) "kunta_091"),
                                                                          :aliOrganisaatioMaara 0,
                                                                          :organisaatiotyypit [ "organisaatiotyyppi_03" ],
                                                                          :status "AKTIIVINEN"
                                                                          :children []}) oppilaitoksen-osat))}]}]})
(defn mock-organisaatio-hierarkia
  [oid & {:as params}]
  (locking mock-organisaatio-hierarkia ;with-redefs used in kouta-indexer-fixture is not thread safe
    (let [oids (get-oids oid)]
      (create-organisaatio-hierarkia
       {:oid (first oids)}
       {:oid (second oids)}
       (vec (map (fn [o] {:oid o}) (last oids)))))))
