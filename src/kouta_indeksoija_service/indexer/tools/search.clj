(ns kouta-indeksoija-service.indexer.tools.search
  (:require [kouta-indeksoija-service.indexer.tools.general :refer [ammatillinen?]]
            [kouta-indeksoija-service.indexer.tools.koodisto :refer :all]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [remove-uri-version]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [oppilaitostyyppi-uri-to-tyyppi]]))

(defn- clean-uris
  [uris]
  (vec (map remove-uri-version uris)))

(defn hit
  [& {:keys [koulutustyyppi
             koulutustyyppiUrit
             opetuskieliUrit
             tarjoajat
             oppilaitos
             koulutusalaUrit
             nimet
             asiasanat
             ammattinimikkeet
             oppilaitosOid
             koulutusOid
             toteutusOid
             onkoTuleva
             nimi
             metadata]
      :or {koulutustyyppi nil
           koulutustyyppiUrit []
           opetuskieliUrit []
           tarjoajat []
           oppilaitos []
           koulutusalaUrit []
           nimet []
           asiasanat []
           ammattinimikkeet []
           oppilaitosOid nil
           koulutusOid nil
           toteutusOid nil
           onkoTuleva nil
           nimi {}
           metadata {}}}]

  (let [kunnat (remove nil? (distinct (map :kotipaikkaUri tarjoajat)))
        maakunnat (remove nil? (distinct (map #(:koodiUri (maakunta %)) kunnat)))

        terms (fn [lng-keyword] (distinct (remove nil? (concat (map lng-keyword nimet) ;HUOM! Älä tee tästä defniä, koska se ei enää ole thread safe!
                                                               (vector (-> oppilaitos :nimi lng-keyword))
                                                               (map #(-> % :nimi lng-keyword) tarjoajat)
                                                               (map lng-keyword asiasanat)
                                                               (map lng-keyword ammattinimikkeet)))))]

    (cond-> {:koulutustyypit (clean-uris (concat (vector koulutustyyppi) koulutustyyppiUrit))
             :opetuskielet (clean-uris opetuskieliUrit)
             :sijainti (clean-uris (concat kunnat maakunnat))
             :koulutusalat (clean-uris koulutusalaUrit)
             :terms {:fi (terms :fi)
                     :sv (terms :sv)
                     :en (terms :en)}
             :metadata (common/decorate-koodi-uris (merge metadata {:kunnat kunnat}))}

            (not (nil? koulutusOid))   (assoc :koulutusOid koulutusOid)
            (not (nil? toteutusOid))   (assoc :toteutusOid toteutusOid)
            (not (nil? oppilaitosOid)) (assoc :oppilaitosOid oppilaitosOid)
            (not (nil? onkoTuleva))    (assoc :onkoTuleva onkoTuleva)
            (not (empty? nimi))        (assoc :nimi nimi))))

(defn koulutusalaKoodiUrit
  [koulutus]
  (if (ammatillinen? koulutus)
    (let [koulutusKoodiUri (:koulutusKoodiUri koulutus)]
      (vec (concat (map :koodiUri (koulutusalat-taso1 koulutusKoodiUri))
                   (map :koodiUri (koulutusalat-taso2 koulutusKoodiUri)))))
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

(defn koulutustyyppi-for-organisaatio
  [organisaatio]
  (when-let [oppilaitostyyppi (:oppilaitostyyppi organisaatio)]
    (oppilaitostyyppi-uri-to-tyyppi oppilaitostyyppi)))