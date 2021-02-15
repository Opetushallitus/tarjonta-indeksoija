(ns kouta-indeksoija-service.indexer.tools.general)

(defonce Julkaistu "julkaistu")
(defonce Tallennettu "tallennettu")

(defn julkaistu?
  [entry]
  (and (not (nil? entry)) (= (:tila entry) Julkaistu)))

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
  (or (= "yo" (:koulutustyyppi koulutus))
      (= "amk" (:koulutustyyppi koulutus))))

(defn any-ammatillinen?
  [koulutus]
  (or (ammatillinen? koulutus) (amm-osaamisala? koulutus) (amm-tutkinnon-osa? koulutus)))

(defn asiasana->lng-value-map
  [asiasanat]
  (map (fn [a] {(keyword (:kieli a)) (:arvo a)}) asiasanat))