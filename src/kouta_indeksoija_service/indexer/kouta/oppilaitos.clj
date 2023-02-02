(ns kouta-indeksoija-service.indexer.kouta.oppilaitos
  (:require [clojure.set :refer [rename-keys]]
            [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.kouta.common :as common]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.util.tools :refer [oppilaitos-jarjestaa-urheilijan-amm-koulutusta?]]
            [clojure.string :as s]))

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
  (let [postinumeroKoodiUri (:postinumeroUri (first (filter (fn [os] (get-in os [:postinumeroUri])) osoitetieto)))]
    {:osoite (create-kielistetty-yhteystieto osoitetieto :osoite languages)
     :postinumeroKoodiUri (when (not (s/blank? postinumeroKoodiUri)) postinumeroKoodiUri)}))

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
  [katuosoite-map postinumero toimipaikat]
  (let [toimipaikat-with-default (into {} (for [[k, v] katuosoite-map]
                                            (if (not (contains? toimipaikat k))
                                              [k (or (:fi toimipaikat) (:sv toimipaikat) (:en toimipaikat))]
                                              [k (k toimipaikat)])))
        capitalized_toimipaikat (zipmap
                                  (keys toimipaikat-with-default)
                                  (map #(clojure.string/capitalize %) (vals toimipaikat-with-default)))]
   (merge-with #(str %1 " " %2)
               (into {} (for [[k v] katuosoite-map] [k (str v ", " postinumero)]))
               capitalized_toimipaikat)))

(defn add-osoite-str-to-yhteystiedot
  [yhteystiedot-from-oppilaitos-metadata osoitetyyppi json-key]
  (if-let [osoite (get-in yhteystiedot-from-oppilaitos-metadata [osoitetyyppi])]
    (if-let [postinumeroKoodiUri (or (get-in osoite [:postinumeroKoodiUri])
                                     (get-in osoite [:postinumero :koodiUri]))]
      (let [postinumero (re-find #"\d{5}" postinumeroKoodiUri)
            postitoimipaikka (get-koodi-nimi-with-cache postinumeroKoodiUri)
            osoite-str (create-osoite-str-for-hakijapalvelut (get-in osoite [:osoite]) postinumero (:nimi postitoimipaikka))]
        (assoc yhteystiedot-from-oppilaitos-metadata json-key osoite-str))
      yhteystiedot-from-oppilaitos-metadata)
    yhteystiedot-from-oppilaitos-metadata))

(defn- oppilaitoksen-osa-entry
  [organisaatio oppilaitoksen-osa]
  ; TODO oppilaitosten osat eivät voi käyttää assoc-koulutusohjelmia sillä kouta-backend/get-koulutukset-by-tarjoaja ei palauta osille mitään
  ; TODO oppilaitoksen osien pitäisi päätellä koulutusohjelmia-lkm eri reittiä: toteutukset -> koulutukset -> johtaaTutkintoon
  (let [update-yhteystiedot-fn (fn [oo] (if (nil? (get-in oo [:metadata :hakijapalveluidenYhteystiedot]))
                                          oo (update-in oo [:metadata :hakijapalveluidenYhteystiedot]
                                                        (fn [yhteystiedot] (-> yhteystiedot
                                                                               (add-osoite-str-to-yhteystiedot :postiosoite :postiosoiteStr)
                                                                               (add-osoite-str-to-yhteystiedot :kayntiosoite :kayntiosoiteStr))))))]
    (cond-> (organisaatio-entry organisaatio)
            (seq oppilaitoksen-osa) (assoc :oppilaitoksenOsa (-> oppilaitoksen-osa
                                                                 (common/complete-entry)
                                                                 (update-yhteystiedot-fn)
                                                                 (dissoc :oppilaitosOid :oid))))))

(defn- add-data-from-organisaatio-palvelu
  [organisaatio]
  (let [org-from-organisaatio-palvelu (cache/get-yhteystiedot (:oid organisaatio))
        yhteystiedot (parse-yhteystiedot org-from-organisaatio-palvelu languages)]
    (-> organisaatio
        (assoc :status (:status org-from-organisaatio-palvelu))
        (assoc-in [:metadata :yhteystiedot] yhteystiedot))))

(defn- oppilaitos-entry-with-osat
  [organisaatio execution-id]
  (let [oppilaitos-oid (:oid organisaatio)
        oppilaitos (or (kouta-backend/get-oppilaitos-with-cache oppilaitos-oid execution-id) {})
        oppilaitos-from-organisaatiopalvelu (cache/get-yhteystiedot oppilaitos-oid)
        yhteystiedot (parse-yhteystiedot oppilaitos-from-organisaatiopalvelu languages)
        hakijapalveluiden-yhteystiedot (-> (get-in oppilaitos [:metadata :hakijapalveluidenYhteystiedot])
                                           (add-osoite-str-to-yhteystiedot :postiosoite :postiosoiteStr)
                                           (add-osoite-str-to-yhteystiedot :kayntiosoite :kayntiosoiteStr))
        oppilaitos-metadata (assoc
                              (get-in oppilaitos [:metadata])
                              :yhteystiedot yhteystiedot
                              :hakijapalveluidenYhteystiedot hakijapalveluiden-yhteystiedot)
        enriched-oppilaitos (assoc oppilaitos :metadata oppilaitos-metadata)
        oppilaitoksen-osat (map #(add-data-from-organisaatio-palvelu %) (kouta-backend/get-oppilaitoksen-osat-with-cache oppilaitos-oid execution-id))
        koulutukset (kouta-backend/get-koulutukset-by-tarjoaja-with-cache (:oid organisaatio) execution-id)
        find-oppilaitoksen-osa (fn [child] (or (first (filter #(= (:oid %) (:oid child)) oppilaitoksen-osat)) {}))]
    (as-> (oppilaitos-entry organisaatio enriched-oppilaitos koulutukset) o
          (assoc o :osat (->> (organisaatio-tool/get-indexable-children organisaatio)
                         (map #(oppilaitoksen-osa-entry % (find-oppilaitoksen-osa %)))
                         (vec)))
          (assoc o :jarjestaaUrheilijanAmmKoulutusta (oppilaitos-jarjestaa-urheilijan-amm-koulutusta? o)))))

(defn create-index-entry
  [oid execution-id]
  (when-let [oppilaitos (cache/find-oppilaitos-by-own-or-child-oid  oid)]
    (if (organisaatio-tool/indexable? oppilaitos)
      (indexable/->index-entry (:oid oppilaitos) (oppilaitos-entry-with-osat oppilaitos execution-id))
      (indexable/->delete-entry (:oid oppilaitos)))))

(defn do-index
  ([oids execution-id clear-cache-before]
    (when (= true clear-cache-before)(cache/clear-all-cached-data))
    (let [oids-to-index (organisaatio-tool/resolve-organisaatio-oids-to-index (cache/get-hierarkia-cached) oids)]
      (indexable/do-index index-name oids-to-index create-index-entry execution-id)))
  ([oids execution-id]
   (do-index oids execution-id true)))

(defn get-from-index
  [oid & query-params]
  (apply indexable/get index-name oid query-params))
