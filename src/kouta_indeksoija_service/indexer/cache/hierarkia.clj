(ns kouta-indeksoija-service.indexer.cache.hierarkia
  (:require [clojure.core.cache :as cache]
            [clojure.set :refer [rename-keys]]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as o]
            [kouta-indeksoija-service.rest.organisaatio :refer [get-all-organisaatiot get-by-oid find-last-changes oph-oid]]
            [kouta-indeksoija-service.util.conf :refer [env]]
            [clojure.core.memoize :as memoize]))

(defonce organisaatio_cache_time_millis (Integer. (:kouta-indeksoija-organisaatio-cache-time-millis env)))

(defn- make-cache-factory [] (cache/ttl-cache-factory {} :ttl organisaatio_cache_time_millis))

(defonce YHTEYSTIETO_CACHE (atom (make-cache-factory)))

(defn clear-yhteystieto-cache [] (reset! YHTEYSTIETO_CACHE (make-cache-factory)))

(defn- do-cache-yhteystiedot
  [yhteystiedot-from-org-palvelu]
  (swap! YHTEYSTIETO_CACHE cache/through-cache
    (:oid yhteystiedot-from-org-palvelu)
         (constantly (select-keys yhteystiedot-from-org-palvelu [:nimi :status :yhteystiedot]))))

(defn- cache-yhteystiedot
  [oid]
  (when-let [yhteystiedot (get-by-oid oid)]
    (do-cache-yhteystiedot yhteystiedot)))

(defn- koulutustoimija-from-hierarkia-item
  [hierarkia-item]
  (select-keys hierarkia-item [:oid :status :organisaatiotyypit :nimi :kotipaikkaUri]))

(defn- oppilaitos-from-hierarkia-item
  [hierarkia-item]
  (select-keys hierarkia-item [:oid :status :organisaatiotyypit :oppilaitostyyppi :parentOid :nimi :kotipaikkaUri :kieletUris]))

(defn- toimipiste-from-hierarkia-item
  [hierarkia-item]
  (select-keys hierarkia-item [:oid :status :organisaatiotyypit :parentOid :nimi :kotipaikkaUri :kieletUris]))

(defn- find-parent-oppilaitos-recursively
  [cache-atom oid]
  (when-let [parent-oid (:parentOid (get @cache-atom oid))]
    (let [parent (get @cache-atom parent-oid)]
      (if (o/oppilaitos? parent)
        parent-oid
        (find-parent-oppilaitos-recursively cache-atom parent-oid)))))

(defn fix-toimipiste-parents
  [cache-atom]
  (let [toimipiste-oids (filter (fn [oid] (o/toimipiste? (get @cache-atom oid))) (keys @cache-atom))]
    (doseq [oid toimipiste-oids]
      (let [toimipiste (get @cache-atom oid)
            parent-oid (:parentOid toimipiste)]
        (when (not (o/oppilaitos? (get @cache-atom parent-oid)))
          (let [parent-oppilaitos-oid (find-parent-oppilaitos-recursively cache-atom parent-oid)]
            (if (not (nil? parent-oppilaitos-oid))
              (swap! cache-atom assoc oid (assoc toimipiste :parentOid parent-oppilaitos-oid))
              (swap! cache-atom assoc oid (dissoc toimipiste :parentOid)))))))))

(defn- update-children-of-parents
  [cache-atom]
  (doseq [oid (keys @cache-atom)]
    (let [hierakia-item (get @cache-atom oid)]
      (when (or (o/koulutustoimija? hierakia-item)(o/oppilaitos? hierakia-item))
        (let [child-oids (vec (filter (fn [item-oid] (= oid (:parentOid (get @cache-atom item-oid)))) (keys @cache-atom)))]
          (swap! cache-atom assoc oid (assoc hierakia-item :childOids child-oids)))))))

(defn- cache-hierarkia-recursively
  [cache hierarkia-item]
  (let [oid (:oid hierarkia-item)]
  (cond
    (o/koulutustoimija? hierarkia-item) (swap! cache assoc oid (koulutustoimija-from-hierarkia-item hierarkia-item))
    (o/oppilaitos? hierarkia-item) (swap! cache assoc oid (oppilaitos-from-hierarkia-item hierarkia-item))
    (o/toimipiste? hierarkia-item) (swap! cache assoc oid (toimipiste-from-hierarkia-item hierarkia-item)))
  (doseq [child (:children hierarkia-item)] (cache-hierarkia-recursively cache child))))

(defn- cache-whole-hierarkia
  []
  (let [cache (atom {})
        orgs (:organisaatiot (get-all-organisaatiot))]
    (doseq [hierarkia-item orgs] (cache-hierarkia-recursively cache hierarkia-item))
    (fix-toimipiste-parents cache)
    (update-children-of-parents cache)
    cache))

(def hierarkia-cached
  (memoize/ttl cache-whole-hierarkia :ttl/threshold organisaatio_cache_time_millis))

(defn clear-hierarkia-cache []
  (memoize/memo-clear! hierarkia-cached))

(defn clear-all-cached-data [] (do (clear-hierarkia-cache) (clear-yhteystieto-cache)))


(defn get-hierarkia-cached []
  (hierarkia-cached))

(defn get-yhteystiedot
  [oid]
  (if-let [yhteystiedot (cache/lookup @YHTEYSTIETO_CACHE oid)]
    yhteystiedot
    (do (cache-yhteystiedot oid)
        (cache/lookup @YHTEYSTIETO_CACHE oid))))

(defn get-hierarkia-item
  [oid]
  (if (= oph-oid oid)
    (-> (get-yhteystiedot oph-oid)
        (assoc :oid oph-oid)
        (dissoc :yhteystiedot))
    (get @(get-hierarkia-cached) oid)))

(defn find-oppilaitos-by-own-or-child-oid
  [oid]
  (when-let [member (get-hierarkia-item oid)]
    (let [assoc-toimipisteet (fn [oppilaitos] (-> oppilaitos
                                                (assoc :children (vec (map get-hierarkia-item (sort (:childOids oppilaitos)))))
                                                (dissoc :childOids)))]
      (cond
        (o/oppilaitos? member)(assoc-toimipisteet member)
        (o/toimipiste? member)(assoc-toimipisteet (get-hierarkia-item (:parentOid member)))))))


(defn get-all-muutetut-organisaatiot-cached
  [last-modified]
  (when-let [muutetut-all (->> (find-last-changes last-modified)
                               (map (fn [m] (rename-keys m {:tyypit :organisaatiotyypit}))))]
    (let [muutetut-oppilaitokset-and-toimipisteet (filter (fn [m] (or (o/oppilaitos? m) (o/toimipiste? m))) muutetut-all)]
      (doseq [muutettu muutetut-oppilaitokset-and-toimipisteet] (do (swap! YHTEYSTIETO_CACHE cache/evict (:oid muutettu)) (do-cache-yhteystiedot muutettu)))
      (map :oid muutetut-all))))

(defn get-all-indexable-oppilaitos-oids
  []
  (->> (vals @(get-hierarkia-cached))
       (filter o/indexable-oppilaitos?)
       (map :oid)
       vec))
