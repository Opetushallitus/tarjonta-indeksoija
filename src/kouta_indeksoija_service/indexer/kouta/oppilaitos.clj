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
(def languages ["fi" "en" "sv"])

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
  [yhteystieto-group yhteystieto-keyword languages]
  (into
    {}
    (for [lang languages
          :let [yhteystieto (yhteystieto-keyword
                              (first
                                (filter
                                  (fn [entry]
                                    (re-find (re-pattern (str "kieli_" lang)) (:kieli entry)))
                                  yhteystieto-group)))]
          :when (not (nil? yhteystieto))]
      [(keyword lang) yhteystieto])))

(defn create-kielistetty-osoitetieto
  [osoitetieto languages]
  {:osoite (create-kielistetty-yhteystieto osoitetieto :osoite languages)
   :postinumeroKoodiUri (:postinumeroUri (first (filter (fn [os] (get-in os [:postinumeroUri])) osoitetieto)))})

(defn create-kielistetty-osoite-str
  [osoitetieto ulkomainen_osoite_en languages]
  (let [katuosoite (create-kielistetty-yhteystieto osoitetieto :osoite languages)
        postinumero_uri (create-kielistetty-yhteystieto osoitetieto :postinumeroUri languages)
        postinumero (zipmap
                      (keys postinumero_uri)
                      (map #(re-find #"\d{5}" %) (vals postinumero_uri)))
        postitoimipaikka (create-kielistetty-yhteystieto osoitetieto :postitoimipaikka languages)
        capitalized_postitoimipaikka (zipmap
                                       (keys postitoimipaikka)
                                       (map #(clojure.string/capitalize %) (vals postitoimipaikka)))
        postinro_ja_toimipaikka (merge-with #(str %1 " " %2)
                                            postinumero
                                            capitalized_postitoimipaikka)
        kielistetyt_osoitteet (merge-with #(str %1 ", " %2)
                                          katuosoite
                                          postinro_ja_toimipaikka)]
    (if (and (not (:en kielistetyt_osoitteet)) (not (empty? ulkomainen_osoite_en)))
      (assoc kielistetyt_osoitteet :en (clojure.string/replace (:osoite (first ulkomainen_osoite_en)) #"\n" ", "))
      kielistetyt_osoitteet)))

(defn parse-yhteystiedot
  [response languages]
  (let [yhteystiedot (:yhteystiedot response)
        sahkopostit (filter (fn [yhteystieto] (get-in yhteystieto [:email])) yhteystiedot)
        puhelinnumerot (filter (fn [yhteystieto] (= "puhelin" (get-in yhteystieto [:tyyppi]))) yhteystiedot)
        postiosoitteet (filter (fn [yhteystieto] (= "posti" (get-in yhteystieto [:osoiteTyyppi]))) yhteystiedot)
        kayntiosoitteet (filter (fn [yhteystieto] (= "kaynti" (get-in yhteystieto [:osoiteTyyppi]))) yhteystiedot)
        ulkomainen_posti_en (filter (fn [yhteystieto] (and
                                                        (= "ulkomainen_posti" (get-in yhteystieto [:osoiteTyyppi]))
                                                        (re-find #"kieli_en" (get-in yhteystieto [:kieli]))))
                                    yhteystiedot)
        ulkomainen_kaynti_en (filter (fn [yhteystieto] (and
                                                        (= "ulkomainen_kaynti" (get-in yhteystieto [:osoiteTyyppi]))
                                                        (re-find #"kieli_en" (get-in yhteystieto [:kieli]))))
                                    yhteystiedot)]
    [{:nimi (:nimi response)
      :sahkoposti (create-kielistetty-yhteystieto sahkopostit :email languages)
      :puhelinnumero (create-kielistetty-yhteystieto puhelinnumerot :numero languages)
      :postiosoite (create-kielistetty-osoitetieto postiosoitteet languages)
      :kayntiosoite (create-kielistetty-osoitetieto kayntiosoitteet languages)
      :postiosoiteStr (create-kielistetty-osoite-str postiosoitteet ulkomainen_posti_en languages)
      :kayntiosoiteStr (create-kielistetty-osoite-str kayntiosoitteet ulkomainen_kaynti_en languages)}]))

(defn create-osoite-str-for-hakijapalvelut
  [katuosoite-map postinumero toimipaikka]
  (let [toimipaikat (select-keys toimipaikka (keys katuosoite-map))
        toimipaikat-with-default (into {} (for [[k, v] katuosoite-map]
                                            (if (not (contains? toimipaikat k))
                                              [k (:fi toimipaikat)]
                                              [k (k toimipaikat)])))
        capitalized_toimipaikat (zipmap
                                  (keys toimipaikat-with-default)
                                  (map #(clojure.string/capitalize %) (vals toimipaikat-with-default)))]
   (merge-with #(str %1 " " %2)
               (into {} (for [[k v] katuosoite-map] [k (str v ", " postinumero)]))
               capitalized_toimipaikat)))

(defn add-osoite-str-to-yhteystiedot
  [yhteystiedot-from-oppilaitos-metadata osoitetyyppi]
  (if-let [postiosoite (get-in yhteystiedot-from-oppilaitos-metadata [osoitetyyppi])]
    (let [postinumerokoodiuri (get-in postiosoite [:postinumeroKoodiUri])
          postinumero (re-find #"\d{5}" postinumerokoodiuri)
          postitoimipaikka (get-koodi-nimi-with-cache postinumerokoodiuri)
          postiosoite-str (create-osoite-str-for-hakijapalvelut (get-in postiosoite [:osoite]) postinumero (:nimi postitoimipaikka))]
      (assoc yhteystiedot-from-oppilaitos-metadata :postiosoiteStr postiosoite-str))
    yhteystiedot-from-oppilaitos-metadata))

(defn- add-data-from-organisaatio-palvelu
  [organisaatio]
  (let [org-from-organisaatio-palvelu (organisaatio-client/get-by-oid-cached (:oid organisaatio))
        yhteystiedot (parse-yhteystiedot org-from-organisaatio-palvelu languages)]
    (-> organisaatio
        (assoc :status (:status org-from-organisaatio-palvelu))
        (assoc-in [:metadata :yhteystiedot] yhteystiedot))))

(defn- oppilaitos-entry-with-osat
  [organisaatio]
  (let [oppilaitos-oid (:oid organisaatio)
        oppilaitos (or (kouta-backend/get-oppilaitos oppilaitos-oid) {})
        oppilaitos-from-organisaatiopalvelu (organisaatio-client/get-by-oid-cached oppilaitos-oid)
        yhteystiedot (parse-yhteystiedot oppilaitos-from-organisaatiopalvelu languages)
        hakijapalveluiden-yhteystiedot (-> (get-in (get-in oppilaitos [:metadata]) [:hakijapalveluidenYhteystiedot])
                                           (add-osoite-str-to-yhteystiedot :postiosoite)
                                           (add-osoite-str-to-yhteystiedot :kayntiosoite))
        oppilaitos-metadata (assoc
                              (get-in oppilaitos [:metadata])
                              :yhteystiedot yhteystiedot
                              :hakijapalveluidenYhteystiedot hakijapalveluiden-yhteystiedot)
        enriched-oppilaitos (assoc oppilaitos :metadata oppilaitos-metadata)
        oppilaitoksen-osat (map #(add-data-from-organisaatio-palvelu %) (kouta-backend/get-oppilaitoksen-osat oppilaitos-oid))
        koulutukset (kouta-backend/get-koulutukset-by-tarjoaja (:oid organisaatio))
        find-oppilaitoksen-osa (fn [child] (or (first (filter #(= (:oid %) (:oid child)) oppilaitoksen-osat)) {}))]
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
