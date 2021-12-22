(ns kouta-indeksoija-service.indexer.kouta.oppilaitos
  (:require [clojure.set :refer [rename-keys]]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.kouta.oppilaitos-search :as search]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(def index-name "oppilaitos-kouta")

(defn- organisaatio-entry
  [organisaatio]
  (-> organisaatio
      (select-keys [:nimi :kieletUris :kotipaikkaUri :status :organisaatiotyypit :oppilaitostyyppi :oid])
      (rename-keys {:kieletUris :opetuskieliKoodiUrit
                    :oppilaitostyyppi :oppilaitostyyppiKoodiUri
                    :kotipaikkaUri :kotipaikkaKoodiUri
                    :organisaatiotyypit :organisaatiotyyppiKoodiUrit})
      (common/complete-entry)))

(defn- assoc-koulutusohjelmia
  [organisaatio koulutukset]
  (assoc organisaatio :koulutusohjelmia (count (filter :johtaaTutkintoon koulutukset))))

(defn- oppilaitos-entry
  [organisaatio oppilaitos koulutukset]
  (cond-> (assoc-koulutusohjelmia (organisaatio-entry organisaatio) koulutukset)
    (seq oppilaitos) (assoc :oppilaitos (-> oppilaitos
                                            (common/complete-entry)
                                            (dissoc :oid)))))

(defn- oppilaitoksen-osa-entry
  [organisaatio oppilaitoksen-osa]
  ; TODO oppilaitosten osat eivät voi käyttää assoc-koulutusohjelmia sillä kouta-backend/get-koulutukset-by-tarjoaja ei palauta osille mitään
  ; TODO oppilaitoksen osien pitäisi päätellä koulutusohjelmia-lkm eri reittiä: toteutukset -> koulutukset -> johtaaTutkintoon
  (cond-> (organisaatio-entry organisaatio)
    (seq oppilaitoksen-osa) (assoc :oppilaitoksenOsa (-> oppilaitoksen-osa
                                                         (common/complete-entry)
                                                         (dissoc :oppilaitosOid :oid)))))
(defn create-kielistetty-yhteystieto
  [yhteystieto yhteystieto-name languages]
  (into {} (for [lang languages
                 :let [result (yhteystieto-name
                                (first
                                  (filter
                                    (fn [yhteystieto-map]
                                      (re-find (re-pattern lang) (:kieli yhteystieto-map)))
                                    yhteystieto)))]
                 :when (not (nil? result))]
             [(keyword lang) result])))

(defn create-kielistetty-osoitetieto
  [osoitetieto languages]
  {:osoite (create-kielistetty-yhteystieto osoitetieto :osoite languages)
   :postinumeroKoodiUri (:postinumeroUri (first (filter (fn [os] (get-in os [:postinumeroUri])) osoitetieto)))})

(defn parse-yhteystiedot
  [response languages]
  (let [yhteystiedot (:yhteystiedot response)
        sahkopostit (filter (fn [yhteystieto] (get-in yhteystieto [:email])) yhteystiedot)
        puhelinnumerot (filter (fn [yhteystieto] (= "puhelin" (get-in yhteystieto [:tyyppi]))) yhteystiedot)
        postiosoitteet (filter (fn [yhteystieto] (= "posti" (get-in yhteystieto [:osoiteTyyppi]))) yhteystiedot)
        kayntiosoitteet (filter (fn [yhteystieto] (= "kaynti" (get-in yhteystieto [:osoiteTyyppi]))) yhteystiedot)]
  [{:nimi (:nimi response)
   :sahkoposti (create-kielistetty-yhteystieto sahkopostit :email languages)
   :puhelinnumero (create-kielistetty-yhteystieto puhelinnumerot :numero languages)
   :postiosoite (create-kielistetty-osoitetieto postiosoitteet languages)
   :kayntiosoite (create-kielistetty-osoitetieto kayntiosoitteet languages)
   }]))

(defn- add-data-from-organisaatio-palvelu
  [organisaatio]
  (let [org-from-organisaatio-palvelu (organisaatio-client/get-by-oid-cached (:oid organisaatio))
       yhteystiedot (parse-yhteystiedot org-from-organisaatio-palvelu ["fi", "sv", "en"])]
    (-> organisaatio
        (assoc :status (:status org-from-organisaatio-palvelu))
        (assoc-in [:metadata :yhteystiedot] yhteystiedot))
    ))

(defn- oppilaitos-entry-with-osat
  [organisaatio]
  (let [oppilaitos-oid (:oid organisaatio)
        oppilaitos (or (kouta-backend/get-oppilaitos oppilaitos-oid) {})
        oppilaitos-from-organisaatiopalvelu (organisaatio-client/get-by-oid-cached oppilaitos-oid)
        oppilaitos-languages (distinct
                               (for [yhteystieto (get-in oppilaitos-from-organisaatiopalvelu [:yhteystiedot])
                                     :let [language (get-in yhteystieto [:kieli])]]
                                 (let [[_ lang] (re-find #"_(.+)#" language)] lang)))
        yhteystiedot (parse-yhteystiedot oppilaitos-from-organisaatiopalvelu oppilaitos-languages)
        oppilaitos-metadata (assoc (get-in oppilaitos [:metadata]) :yhteystiedot yhteystiedot)
        enriched-oppilaitos (assoc oppilaitos :metadata oppilaitos-metadata)
        oppilaitoksen-osat (map #(add-data-from-organisaatio-palvelu %) (kouta-backend/get-oppilaitoksen-osat oppilaitos-oid))
        koulutukset (kouta-backend/get-koulutukset-by-tarjoaja (:oid organisaatio))
        find-oppilaitoksen-osa (fn [child] (or (first (filter #(= (:oid %) (:oid child)) oppilaitoksen-osat)) {}))
        ]
    (-> (oppilaitos-entry organisaatio enriched-oppilaitos koulutukset)
        (assoc :osat (->> (organisaatio-tool/get-indexable-children organisaatio)
                          (map #(oppilaitoksen-osa-entry % (find-oppilaitoksen-osa %)))
                          (vec)))
        (assoc :jarjestaaUrheilijanAmmKoulutusta (boolean (some (fn [osa] (get-in osa [:metadata :jarjestaaUrheilijanAmmKoulutusta])) oppilaitoksen-osat))))))

(defn create-index-entry
  [oid]
  (let [hierarkia (cache/get-hierarkia oid)]
    (when-let [oppilaitos-oid (:oid (organisaatio-tool/find-oppilaitos-from-hierarkia hierarkia))]
      (let [oppilaitos (organisaatio-client/get-hierarkia-for-oid-without-parents oppilaitos-oid)]
        (if (organisaatio-tool/indexable? oppilaitos)
          (indexable/->index-entry (:oid oppilaitos) (oppilaitos-entry-with-osat oppilaitos))
          (indexable/->delete-entry (:oid oppilaitos)))))))

(defn do-index
  [oids execution-id]
  (indexable/do-index index-name oids create-index-entry execution-id))

(defn get-from-index
  [oid & query-params]
  (apply indexable/get index-name oid query-params))
