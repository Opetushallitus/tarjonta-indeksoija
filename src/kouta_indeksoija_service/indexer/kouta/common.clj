(ns kouta-indeksoija-service.indexer.kouta.common
  (:refer-clojure :exclude [replace])
  (:require [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as hierarkia]
            [kouta-indeksoija-service.rest.oppijanumerorekisteri :refer [get-henkilo-nimi-with-cache]]
            [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [kouta-indeksoija-service.util.tools :refer [get-esitysnimi]]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [clojure.string :refer [replace]]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.walk :refer [postwalk]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure.set :as set]
            [kouta-indeksoija-service.indexer.cache.hierarkia :as cache]))

(defn- strip-koodi-uri-key
  [key]
  (if (keyword? key)
    (-> key
        (name)
        (replace "KoodiUrit" "")
        (replace "KoodiUri" "")
        (replace "Uri" "")
        (keyword))
    key))

(defn koodi-uri? ;Etsitään koodiUria kaavalla KIRJAIMIANUMEROITA_NONWHITESPACEMERKKEJÄ#NUMERO
  [value]         ;Numeroita voi olla 1-3 kpl
  (re-find (re-pattern "^\\w+_\\S+(#\\d{1,3})?$") (string/trim value)))

(def excluded-fields {:externalId true
                      :tunniste true
                      :arvo true
                      :fi true
                      :sv true
                      :en true})

(defn- processable-as-koodi-uri? [v]
  (boolean (or (and (string? v) (koodi-uri? v))
               (and (coll? v) (some #(and (string? %) (koodi-uri? %)) v)))))

(defn- safe-get-koodi-for-value [value]
  (if (koodi-uri? value)
    (get-koodi-nimi-with-cache value)
    value))

(defn- process-koodi-values [input]
  (cond (vector? input) (mapv safe-get-koodi-for-value input)
        (seq? input) (doall (map safe-get-koodi-for-value input))
        :else (safe-get-koodi-for-value input)))

(defn- process-map-entry-for-koodis [map-entry]
  (let [[k v]               map-entry
        allowed-key?        (not (get excluded-fields (keyword k)))
        interesting-values? (processable-as-koodi-uri? v)]
    (when (and interesting-values? (not allowed-key?))
      (log/warn (str "Skip processing for map entry because of disallowed key" map-entry)))
    (if (and allowed-key? interesting-values?)
      [k (process-koodi-values v)]
      map-entry)))

(defn- enrich-koodi-values [value]
  (if (map-entry? value)
    (process-map-entry-for-koodis value)
    value))

(defn decorate-koodi-uris
  [x]
  (postwalk #(-> % strip-koodi-uri-key enrich-koodi-values) x))

(defn- get-tarjoaja
  [oid]
  (when-let [tarjoaja (cache/get-hierarkia-item oid)]
    (-> tarjoaja
      (assoc :paikkakunta (get-koodi-nimi-with-cache "kunta" (:kotipaikkaUri tarjoaja)))
      (select-keys [:oid :nimi :paikkakunta]))))

(defn assoc-organisaatio
  [entry]
  (if-let [oid (:organisaatioOid entry)]
    (assoc (dissoc entry :organisaatioOid) :organisaatio (get-tarjoaja oid))
    entry))

(defn assoc-jarjestyspaikka
  [entry]
  (if-let [oid (:jarjestyspaikkaOid entry)]
    (assoc (dissoc entry :jarjestyspaikkaOid :jarjestaaUrheilijanAmmKoulutusta)
           :jarjestyspaikka
           (assoc (get-tarjoaja oid)
                  :jarjestaaUrheilijanAmmKoulutusta
                  (get-in entry [:jarjestaaUrheilijanAmmKoulutusta])))
    entry))

(defn assoc-muokkaaja
  [entry]
  (if-let [oid (:muokkaaja entry)]
    (if-let [nimi (get-henkilo-nimi-with-cache oid)]
      (assoc entry :muokkaaja {:oid oid :nimi nimi})
      (assoc entry :muokkaaja {:oid oid}))
    entry))

(defn assoc-tarjoajat
  [entry]
  (if-let [oids (:tarjoajat entry)]
    (assoc entry :tarjoajat (map #(get-tarjoaja %1) oids))
    entry))

(defn assoc-organisaatiot
  [entry]
  (let [organisaatio (get-in entry [:organisaatio :oid])
        tarjoajat (map :oid (:tarjoajat entry))]
    (assoc entry :organisaatiot (vec (distinct (remove nil? (conj tarjoajat organisaatio)))))))

(defn new-formatter [fmt-str]
  (f/formatter fmt-str (t/time-zone-for-id "Europe/Helsinki")))

(def finnish-format (new-formatter "d.M.yyyy 'klo' HH:mm"))
(def swedish-format (new-formatter "d.M.yyyy 'kl.' HH:mm"))
(def english-format (new-formatter "MMM. d, yyyy 'at' hh:mm a z"))

(defn- parse-date-time
  [s]
  (let [tz (t/time-zone-for-id "Europe/Helsinki")
        fmt-with-seconds (f/formatter "yyyy-MM-dd'T'HH:mm:ss" tz)
        fmt (f/formatter "yyyy-MM-dd'T'HH:mm" tz)]
    (try
      (t/to-time-zone (f/parse fmt-with-seconds s) tz)
      (catch Exception _
        (try
          (t/to-time-zone (f/parse fmt s) tz)
          (catch Exception e
            (log/error (str "Unable to parse" s) e)))))))

(defn- replace-eet-eest-with-utc-offset [parse-date-time]
  (-> parse-date-time
      (replace #"EET" "UTC+2")
      (replace #"EEST" "UTC+3")))

(defn localize-dates [form]
  (let [format-date         (fn [date]
                              (if-let [parsed (parse-date-time date)]
                                {:fi (f/unparse finnish-format parsed)
                                 :sv (f/unparse swedish-format parsed)
                                 :en (replace-eet-eest-with-utc-offset (f/unparse english-format parsed))}
                                {}))
        format-date-kws     (fn [tree dates]
                              (loop [d dates
                                     t tree]
                                (if-let [date (first d)]
                                  (if-let [aika (date t)]
                                    (recur (rest d)
                                           (assoc t (keyword (str "formatoitu" (string/capitalize (name date)))) (format-date aika)))
                                    (recur (rest d)
                                           t))
                                  t)))]
    (postwalk #(-> %
                   (format-date-kws [:koulutuksenAlkamispaivamaara :koulutuksenPaattymispaivamaara :liitteidenToimitusaika :toimitusaika :modified :paattyy :alkaa])) form)))

(defn clean-langs-not-in-kielivalinta [form]
  (let [langs #{:fi :sv :en}
        used-langs (set (map #(keyword %) (:kielivalinta form)))
        langs-to-clean (set/difference langs used-langs)
        dissoc-langs (fn [x] (if (and (map? x) (not-empty (clojure.set/difference (set (keys x)) langs-to-clean)))
                               (apply dissoc x langs-to-clean)
                               x))]
    (if (and
          (< 0 (count langs-to-clean))
          (< (count langs-to-clean) 3))
      (postwalk dissoc-langs form)
      form)))

(defn complete-entry
  [entry]
  (-> entry
      (clean-langs-not-in-kielivalinta)
      (decorate-koodi-uris)
      (assoc-organisaatio)
      (assoc-tarjoajat)
      (assoc-jarjestyspaikka)
      (assoc-muokkaaja)))

(defn complete-entries
  [entries]
  (map complete-entry entries))

(defn toteutus->list-item
  [toteutus]
  (-> toteutus
      (select-keys [:oid :organisaatio :tila :tarjoajat :muokkaaja :modified :organisaatiot])
      (assoc :nimi (get-esitysnimi toteutus))
      (assoc-organisaatiot)))

(defn- create-ataru-link-for-haku
  [haku-oid lang]
  (resolve-url :ataru-hakija.ataru.hakulomake-for-haku haku-oid lang))

(defn- create-ataru-links-for-haku
  [haku-oid]
  (if haku-oid {:fi (create-ataru-link-for-haku haku-oid "fi")
                :sv (create-ataru-link-for-haku haku-oid "sv")
                :en (create-ataru-link-for-haku haku-oid "en")}))

(defn create-hakulomake-linkki-for-haku
  [hakulomaketiedot haku-oid]
  (when-let [linkki (case (:hakulomaketyyppi hakulomaketiedot)
                      "ataru" (create-ataru-links-for-haku haku-oid)
                      "muu"   (:hakulomakeLinkki hakulomaketiedot)
                      nil)]
    {:hakulomakeLinkki linkki}))

(defn- create-ataru-link-for-hakukohde
  [hakukohde-oid lang]
  (resolve-url :ataru-hakija.ataru.hakulomake-for-hakukohde hakukohde-oid lang))

(defn- create-ataru-links-for-hakukohde
  [hakukohde-oid]
  (when hakukohde-oid {:fi (create-ataru-link-for-hakukohde hakukohde-oid "fi")
                       :sv (create-ataru-link-for-hakukohde hakukohde-oid "sv")
                       :en (create-ataru-link-for-hakukohde hakukohde-oid "en")}))

(defn create-hakulomake-linkki-for-hakukohde
  [hakulomaketiedot hakukohde-oid]
  (when-let [linkki (case (:hakulomaketyyppi hakulomaketiedot)
                      "ataru" (create-ataru-links-for-hakukohde hakukohde-oid)
                      "muu"   (:hakulomakeLinkki hakulomaketiedot)
                      nil)]
    {:hakulomakeLinkki linkki}))

(defn create-sort-names [nimi]
  {:fi (or (not-empty (:fi nimi))
           (not-empty (:sv nimi))
           (not-empty (:en nimi)))
   :sv (or (not-empty (:sv nimi))
           (not-empty (:fi nimi))
           (not-empty (:en nimi)))
   :en (or (not-empty (:en nimi))
           (not-empty (:fi nimi))
           (not-empty (:sv nimi)))})

(defn get-tarjoaja-entries
  [hierarkia entries]
  (->> (for [entry entries]
         (when-let [indexable-oids (seq (organisaatio-tool/filter-indexable-oids-for-hierarkia hierarkia (:tarjoajat entry)))]
           (assoc entry :tarjoajat indexable-oids)))
       (remove nil?)
       (vec)))
