(ns konfo-indeksoija-service.fixture.external-services)

(def Koulutustoimija "1.2.246.562.10.11111111111")
(def Oppilaitos1 "1.2.246.562.10.54545454545")
(def Toimipiste1OfOppilaitos1 "1.2.246.562.10.54545454511")
(def Toimipiste2OfOppilaitos1 "1.2.246.562.10.54545454522")
(def Oppilaitos2 "1.2.246.562.10.55555555555")
(def Toimipiste1OfOppilaitos2 "1.2.246.562.10.55555555511")

(defn mock-organisaatio
  [oid]
  (condp = oid
    Oppilaitos1 { :nimi { :fi "Kiva ammattikorkeakoulu" :sv "Kiva ammattikorkeakoulu sv"} :oid oid :kotipaikkaUri "kunta_091"}
    Toimipiste1OfOppilaitos1 { :nimi { :fi "Kiva ammattikorkeakoulu, Helsingin toimipiste" :sv "Kiva ammattikorkeakoulu, Helsingin toimipiste sv"} :oid oid :kotipaikkaUri "kunta_091" }
    Toimipiste2OfOppilaitos1 { :nimi { :fi "Kiva ammattikorkeakoulu, Kuopion toimipiste" :sv "Kiva ammattikorkeakoulu, Kuopion toimipiste sv"} :oid oid :kotipaikkaUri "kunta_297" }
    Oppilaitos2 { :nimi { :fi "Toinen kiva ammattikorkeakoulu"} :oid oid :kotipaikkaUri "kunta_532" }
    { :nimi { :fi (str "Nimi " oid " fi") :en (str "Nimi " oid " en")} :oid oid :kotipaikkaUri "kunta_091" } ))

(defn mock-koodisto
  ([koodisto koodi-uri]
   (if koodi-uri
     { :koodiUri koodi-uri :nimi {:fi (str koodi-uri " nimi fi") :sv (str koodi-uri " nimi sv")}}))
  ([koodi-uri]
   (if koodi-uri
     (mock-koodisto (subs koodi-uri 0 (clojure.string/index-of koodi-uri "_")) koodi-uri))))

(defn mock-muokkaaja
  [oid]
  {:nimi "Kalle Ankka"})