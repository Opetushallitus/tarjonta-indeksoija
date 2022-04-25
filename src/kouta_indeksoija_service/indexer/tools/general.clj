(ns kouta-indeksoija-service.indexer.tools.general)

(defonce Julkaistu "julkaistu")
(defonce Tallennettu "tallennettu")
(defonce Poistettu "poistettu")
(defonce Arkistoitu "arkistoitu")

(defn julkaistu?
  [entry]
  (and (not (nil? entry)) (= (:tila entry) Julkaistu)))

(defn not-poistettu?
  [entry]
  (and (not (nil? entry)) (not= (:tila entry) Poistettu)))

(defn luonnos?
  [entry]
  (and (not (nil? entry)) (= (:tila entry) Tallennettu)))

(defn not-arkistoitu?
  [entry]
  (and (not (nil? entry)) (not= (:tila entry) Arkistoitu)))

(defn ammatillinen?
  [koulutus]
  (= "amm" (:koulutustyyppi koulutus)))

(defn amm-tutkinnon-osa?
  [koulutus]
  (= "amm-tutkinnon-osa" (:koulutustyyppi koulutus)))

(defn amm-osaamisala?
  [koulutus]
  (= "amm-osaamisala" (:koulutustyyppi koulutus)))

(defn korkeakoulutus?
  [koulutus]
  (some
    #(= (:koulutustyyppi koulutus) %)
    ["yo", "amk", "amm-ope-erityisope-ja-opo"]))

(defn lukio?
  [koulutus]
  (= "lk" (:koulutustyyppi koulutus)))

(defn tuva?
  [koulutus]
  (= "tuva" (:koulutustyyppi koulutus)))

(defn telma?
  [koulutus]
  (= "telma" (:koulutustyyppi koulutus)))

(defn vapaa-sivistystyo-opistovuosi?
  [koulutus]
  (= "vapaa-sivistystyo-opistovuosi" (:koulutustyyppi koulutus)))

(defn vapaa-sivistystyo-muu?
  [koulutus]
  (= "vapaa-sivistystyo-muu" (:koulutustyyppi koulutus)))

(defn amm-ope-erityisope-ja-opo?
  [koulutus]
  (= "amm-ope-erityisope-ja-opo" (:koulutustyyppi koulutus)))

(defn any-ammatillinen?
  [koulutus]
  (or (ammatillinen? koulutus) (amm-osaamisala? koulutus) (amm-tutkinnon-osa? koulutus)))

(defn get-non-korkeakoulu-koodi-uri
  [koulutus]
  (when (not (korkeakoulutus? koulutus))
    (-> koulutus
        (:koulutuksetKoodiUri)
        (first)))) ;Ainoastaan korkeakoulutuksilla voi olla useampi kuin yksi koulutusKoodi

(defn asiasana->lng-value-map
  [asiasanat]
  (map (fn [a] {(keyword (:kieli a)) (:arvo a)}) asiasanat))

(defn set-hakukohde-tila-by-related-haku
  [hakukohde haku]
  (if (and (julkaistu? hakukohde) (not (nil? haku)) (not (nil? (:tila haku))) (not (julkaistu? haku)))
    (assoc hakukohde :tila (:tila haku))
      hakukohde
  ))

(defn remove-version-from-koodiuri
  [entity path-to-koodiuri]
  (let [koodiuri (get-in entity path-to-koodiuri)]
    (if (not (nil? koodiuri))
      (assoc-in entity
                path-to-koodiuri
                (clojure.string/replace koodiuri #"#\w+" ""))
      entity)))
