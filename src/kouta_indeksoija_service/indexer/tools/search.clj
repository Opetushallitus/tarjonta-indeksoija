(ns kouta-indeksoija-service.indexer.tools.search
  (:require [kouta-indeksoija-service.indexer.tools.general :refer [ammatillinen?]]
            [kouta-indeksoija-service.indexer.tools.koodisto :refer :all]))

(defn hit
  [& {:keys [koulutustyyppi opetuskieliUrit tarjoajat oppilaitokset koulutusalaUrit nimi asiasanat ammattinimikkeet]
      :or {koulutustyyppi nil opetuskieliUrit [] tarjoajat [] oppilaitokset [] koulutusalaUrit [] nimi {} asiasanat [] ammattinimikkeet []}}]

  (defn- terms
    [lng-keyword]
    (distinct (remove nil? (concat (vector (lng-keyword nimi))
                                   (map #(-> % :nimi lng-keyword) oppilaitokset)
                                   (map #(-> % :nimi lng-keyword) tarjoajat)
                                   (map lng-keyword asiasanat)
                                   (map lng-keyword ammattinimikkeet)))))

  (let [kunnat (distinct (map :kotipaikkaUri tarjoajat))
        maakunnat (distinct (map #(:koodiUri (maakunta %)) kunnat))]

    {:koulutustyyppi koulutustyyppi
     :opetuskielet  (vec opetuskieliUrit)
     :sijainti      (vec (concat kunnat maakunnat))
     :koulutusalat  (vec koulutusalaUrit)
     :terms         {:fi (terms :fi)
                     :sv (terms :sv)
                     :en (terms :en)}}))

(defn koulutusalaKoodiUrit
  [koulutus]
  (if (ammatillinen? koulutus)
    (vec (map :koodiUri (koulutusalat (get-in koulutus [:koulutus :koodiUri]))))
    (get-in koulutus [:metadata :koulutusalaKoodiUrit])))

(defn tutkintonimikeKoodiUrit
  [koulutus]
  (if (ammatillinen? koulutus)
    (vec (map :koodiUri (tutkintonimikkeet (get-in koulutus [:koulutus :koodiUri]))))
    (get-in koulutus [:metadata :tutkintonimikeKoodiUrit])))