(ns kouta-indeksoija-service.indexer.tools.koulutustyyppi
  (:require [kouta-indeksoija-service.indexer.tools.search :as search-tool]
            [kouta-indeksoija-service.indexer.tools.general :refer [ammatillinen?]]))

(defn assoc-koulutustyyppi-path
  ([entity koulutus toteutus-metadata]
   (let [kouta-koulutustyyppi (:koulutustyyppi koulutus)
         konfo-koulutustyypit (search-tool/deduce-koulutustyypit koulutus toteutus-metadata)
         avoin-korkeakoulutus? (get-in koulutus [:metadata :isAvoinKorkeakoulutus])]
     (assoc entity :koulutustyyppiPath
            (cond (some #{"koulutustyyppi_1" "koulutustyyppi_4" "koulutustyyppi_26"} konfo-koulutustyypit) "amm/ammatillinen-perustutkinto"
                  (some #{"koulutustyyppi_11"} konfo-koulutustyypit) "amm/ammattitutkinto"
                  (some #{"koulutustyyppi_12"} konfo-koulutustyypit) "amm/erikoisammattitutkinto"
                  (= kouta-koulutustyyppi "amk") "kk/amk"
                  (= kouta-koulutustyyppi "yo") "kk/yo"
                  (= kouta-koulutustyyppi "kk-opintojakso") (str "kk-muu/" (if avoin-korkeakoulutus? "kk-opintojakso-avoin" "kk-opintojakso"))
                  (= kouta-koulutustyyppi "kk-opintokokonaisuus") (str "kk-muu/" (if avoin-korkeakoulutus? "kk-opintokokonaisuus-avoin" "kk-opintokokonaisuus"))
                  (some #{"kk-muu"} konfo-koulutustyypit) (str "kk-muu/" kouta-koulutustyyppi)
                  (some #{"vapaa-sivistystyo"} konfo-koulutustyypit) "vapaa-sivistystyo"
                  (some #{"amm-muu" "amm-osaamisala" "amm-tutkinnon-osa" "telma"} [kouta-koulutustyyppi]) (str "amm-tutkintoon-johtamaton/" kouta-koulutustyyppi)
                  (ammatillinen? koulutus) "amm/muu-amm-tutkintoon-johtava"
                  :else kouta-koulutustyyppi))))
  ([entity koulutus]
   (assoc-koulutustyyppi-path entity koulutus nil)))