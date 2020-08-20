(ns kouta-indeksoija-service.indexer.tools.search
  (:require [kouta-indeksoija-service.indexer.tools.general :refer [ammatillinen?]]
            [kouta-indeksoija-service.indexer.tools.koodisto :refer :all]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [remove-uri-version]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.cache.eperuste :refer [get-eperuste-by-koulutuskoodi, get-eperuste-by-id]]
            [kouta-indeksoija-service.indexer.tools.tyyppi :refer [oppilaitostyyppi-uri-to-tyyppi]]))

(defn- clean-uris
  [uris]
  (vec (map remove-uri-version uris)))

(defn hit
  [& {:keys [koulutustyyppi
             koulutustyyppiUrit
             opetuskieliUrit
             tarjoajat
             tarjoajaOids
             oppilaitos
             koulutusalaUrit
             nimet
             asiasanat
             ammattinimikkeet
             tutkintonimikeUrit
             oppilaitosOid
             koulutusOid
             toteutusOid
             onkoTuleva
             nimi
             kuva
             metadata]
      :or {koulutustyyppi nil
           koulutustyyppiUrit []
           opetuskieliUrit []
           tarjoajat []
           tarjoajaOids []
           oppilaitos []
           koulutusalaUrit []
           nimet []
           asiasanat []
           ammattinimikkeet []
           tutkintonimikeUrit []
           oppilaitosOid nil
           koulutusOid nil
           toteutusOid nil
           onkoTuleva nil
           nimi {}
           kuva nil
           metadata {}}}]

  (let [tutkintonimikkeet (vec (map #(-> % get-koodi-nimi-with-cache :nimi) tutkintonimikeUrit))
        kunnat (remove nil? (distinct (map :kotipaikkaUri tarjoajat)))
        maakunnat (remove nil? (distinct (map #(:koodiUri (maakunta %)) kunnat)))

        terms (fn [lng-keyword] (distinct (remove nil? (concat (map lng-keyword nimet) ;HUOM! Älä tee tästä defniä, koska se ei enää ole thread safe!
                                                               (vector (-> oppilaitos :nimi lng-keyword))
                                                               (map #(-> % :nimi lng-keyword) tarjoajat)
                                                               (map lng-keyword asiasanat)
                                                               (map lng-keyword ammattinimikkeet)
                                                               (map lng-keyword tutkintonimikkeet)))))]

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
            (not (nil? kuva))          (assoc :kuva kuva)
            (not (nil? onkoTuleva))    (assoc :onkoTuleva onkoTuleva)
            (not (empty? tarjoajaOids)) (assoc :tarjoajat tarjoajaOids)
            (not (empty? nimi))        (assoc :nimi nimi))))

(defn koulutusalaKoodiUrit
  [koulutus]
  (if (ammatillinen? koulutus)
    (let [koulutusKoodiUri (:koulutusKoodiUri koulutus)]
      (vec (concat (map :koodiUri (koulutusalat-taso1 koulutusKoodiUri))
                   (map :koodiUri (koulutusalat-taso2 koulutusKoodiUri)))))
    (get-in koulutus [:metadata :koulutusalaKoodiUrit])))

;TODO korvaa pelkällä get-eperuste-by-id, kun kaikki tuotantodata käyttää ePeruste id:tä
(defn- get-eperuste
  [koulutus]
  (let [eperuste-id (:ePerusteId koulutus)]
    (if eperuste-id
      (get-eperuste-by-id eperuste-id)
      (get-eperuste-by-koulutuskoodi (:koulutusKoodiUri koulutus)))))

(defn tutkintonimikeKoodiUrit
  [koulutus]
  (if (ammatillinen? koulutus)
    (when-let [eperuste (get-eperuste koulutus)]
      (vec (map :tutkintonimikeUri (:tutkintonimikkeet eperuste))))
    (get-in koulutus [:metadata :tutkintonimikeKoodiUrit])))

(defn koulutustyyppiKoodiUrit
  [koulutus]
  (if (ammatillinen? koulutus)
    (vec (map :koodiUri (koulutustyypit (:koulutusKoodiUri koulutus))))
    []))

(defn opintojenlaajuusKoodiUri
  [koulutus]
  (if (ammatillinen? koulutus)
    (when-let [eperuste (get-eperuste koulutus)]
      (get-in eperuste [:opintojenlaajuus :koodiUri]))
    (get-in koulutus [:metadata :opintojenLaajuusKoodiUri])))

(defn opintojenlaajuusyksikkoKoodiUri
  [koulutus]
  (if (ammatillinen? koulutus)
    (when-let [eperuste (get-eperuste koulutus)]
      (get-in eperuste [:opintojenlaajuusyksikko :koodiUri]))
    (get-in koulutus [:metadata :opintojenLaajuusyksikkoKoodiUri])))

(defn koulutustyyppi-for-organisaatio
  [organisaatio]
  (when-let [oppilaitostyyppi (:oppilaitostyyppi organisaatio)]
    (oppilaitostyyppi-uri-to-tyyppi oppilaitostyyppi)))
