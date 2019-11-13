(ns kouta-indeksoija-service.indexer.tools.search
  (:require [kouta-indeksoija-service.indexer.tools.general :refer [ammatillinen?]]
            [kouta-indeksoija-service.indexer.tools.koodisto :refer :all]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [remove-uri-version]]))

(defn hit
  [& {:keys [koulutustyyppi koulutustyyppiUrit opetuskieliUrit tarjoajat oppilaitokset koulutusalaUrit nimi asiasanat ammattinimikkeet]
      :or {koulutustyyppi nil koulutustyyppiUrit [] opetuskieliUrit [] tarjoajat [] oppilaitokset [] koulutusalaUrit [] nimi {} asiasanat [] ammattinimikkeet []}}]

  (defn- terms
    [lng-keyword]
    (distinct (remove nil? (concat (vector (lng-keyword nimi))
                                   (map #(-> % :nimi lng-keyword) oppilaitokset)
                                   (map #(-> % :nimi lng-keyword) tarjoajat)
                                   (map lng-keyword asiasanat)
                                   (map lng-keyword ammattinimikkeet)))))

  (defn- clean-uris
    [uris]
    (vec (map remove-uri-version uris)))

  (let [kunnat (remove nil? (distinct (map :kotipaikkaUri tarjoajat)))
        maakunnat (remove nil? (distinct (map #(:koodiUri (maakunta %)) kunnat)))]

    {:koulutustyypit (clean-uris (concat (vector koulutustyyppi) koulutustyyppiUrit))
     :opetuskielet   (clean-uris opetuskieliUrit)
     :sijainti       (clean-uris (concat kunnat maakunnat))
     :koulutusalat   (clean-uris koulutusalaUrit)
     :terms          {:fi (terms :fi)
                      :sv (terms :sv)
                      :en (terms :en)}}))

(defn koulutusalaKoodiUrit
  [koulutus]
  (if (ammatillinen? koulutus)
    (vec (map :koodiUri (koulutusalat (:koulutusKoodiUri koulutus))))
    (get-in koulutus [:metadata :koulutusalaKoodiUrit])))

(defn tutkintonimikeKoodiUrit
  [koulutus]
  (if (ammatillinen? koulutus)
    (vec (map :koodiUri (tutkintonimikkeet (:koulutusKoodiUri koulutus))))
    (get-in koulutus [:metadata :tutkintonimikeKoodiUrit])))

(defn koulutustyyppiKoodiUrit
  [koulutus]
  (if (ammatillinen? koulutus)
    (vec (map :koodiUri (koulutustyypit (:koulutusKoodiUri koulutus))))
    []))

(defn opintojenlaajuusKoodiUri
  [koulutus]
  (if (ammatillinen? koulutus)
    (some-> koulutus :koulutusKoodiUri (opintojenlaajuus) :koodiUri)
    (get-in koulutus [:metadata :opintojenLaajuusKoodiUri])))

(defn opintojenlaajuusyksikkoKoodiUri
  [koulutus]
  (if (ammatillinen? koulutus)
    (some-> koulutus :koulutusKoodiUri (opintojenlaajuusyksikko) :koodiUri)
    (get-in koulutus [:metadata :opintojenLaajuusyksikkoKoodiUri])))