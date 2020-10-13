(ns kouta-indeksoija-service.indexer.tools.general)

(defonce Julkaistu "julkaistu")
(defonce Tallennettu "tallennettu")

(defn julkaistu?
  [entry]
  (and (not (nil? entry)) (= (:tila entry) Julkaistu)))

(defn ammatillinen?
  [koulutus]
  (= "amm" (:koulutustyyppi koulutus)))

(defn asiasana->lng-value-map
  [asiasanat]
  (map (fn [a] { (keyword (:kieli a)) (:arvo a)} ) asiasanat))