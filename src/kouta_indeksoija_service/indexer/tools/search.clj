(ns kouta-indeksoija-service.indexer.tools.search)

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

  {:koulutustyyppi koulutustyyppi
   :opetuskielet  (vec opetuskieliUrit)
   :sijainti      (vec (distinct (map :kotipaikkaUri tarjoajat)))
   :koulutusalat  (vec koulutusalaUrit)
   :terms         {:fi (terms :fi)
                   :sv (terms :sv)
                   :en (terms :en)}})