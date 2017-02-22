(ns tarjonta-indeksoija-service.tarjonta-client
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [tarjonta-indeksoija-service.util.tools :refer [with-error-logging]]
            [clj-http.client :as client]
            [taoensso.timbre :as log]
            [clojure.java.jdbc :as db]))

(defn get-doc
  [obj]
  (with-error-logging
    (let [url (str (:tarjonta-service-url env) (:type obj) "/" (:oid obj))]
      (-> (client/get url {:as :json})
          :body
          :result))))

(def db-mappings {:koulutus "koulutusmoduuli_toteutus"
                  :hakukohde "hakukohde"
                  :haku "haku"})

(defn find-docs
  [type]
  (let [query (str "SELECT oid FROM " (get db-mappings (keyword type)) " WHERE tila != 'POISTETTU'")]
    (map #(assoc % :type type) (db/query (:tarjonta-db env) [query]))))

(defn find-all-tarjonta-docs []
  (flatten (map find-docs ["koulutus" "hakukohde" "haku"])))

(defn find-koulutus-for-organisaatio
  [organisaatio-oid]
  (let [query (str "SELECT a.koulutusmoduuli_toteutus_oid AS oid "
                   "FROM hakukohde_koulutusmoduuli_toteutus_tarjoajatiedot AS a "
                   "LEFT JOIN koulutusmoduuli_toteutus_tarjoajatiedot_tarjoaja_oid AS b "
                   "ON a.koulutusmoduuli_toteutus_tarjoajatiedot_id = b.koulutusmoduuli_toteutus_tarjoajatiedot_id "
                   "LEFT JOIN koulutusmoduuli_toteutus as k ON a.koulutusmoduuli_toteutus_oid = k.oid "
                   "WHERE b.tarjoaja_oid = '" organisaatio-oid "' AND a.koulutusmoduuli_toteutus_oid IS NOT NULL "
                   "AND k.tila != 'POISTETTU'")]
    (->> query
         (db/query (:tarjonta-db env))
         (map #(assoc % :type "koulutus")))))


(defn find-koulutus-for-hakukohde
  [hakukohde-oid]
  (let [query (str "SELECT k.oid "
                   "FROM hakukohde as h "
                   "LEFT JOIN koulutus_hakukohde as kh ON h.id = kh.hakukohde_id "
                   "LEFT JOIN koulutusmoduuli_toteutus as k ON k.id = kh.koulutus_id "
                   "WHERE h.oid = '" hakukohde-oid "' AND k.oid IS NOT NULL AND k.tila != 'POISTETTU'")]
    (->> query
         (db/query (:tarjonta-db env))
         (map #(assoc % :type "koulutus")))))

(defn find-koulutus-for-haku
  [haku-oid]
  (let [query (str "SELECT k.oid  "
                   "FROM haku as h "
                   "LEFT JOIN hakukohde as hk ON hk.haku_id = h.id "
                   "LEFT JOIN koulutus_hakukohde as kh ON kh.hakukohde_id = hk.id "
                   "LEFT JOIN koulutusmoduuli_toteutus as k ON k.id = kh.koulutus_id "
                   "WHERE h.oid = '" haku-oid "' AND k.oid IS NOT null AND k.tila != 'POISTETTU'")]
    (->> query
         (db/query (:tarjonta-db env))
         (map #(assoc % :type "koulutus")))))

(defn get-related-koulutus [obj]
  (log/debug "Fetching related koulutus for" obj)
  (cond
    (= (:type obj) "organisaatio") (find-koulutus-for-organisaatio (:oid obj))
    (= (:type obj) "hakukohde") (find-koulutus-for-hakukohde (:oid obj))
    (= (:type obj) "haku") (find-koulutus-for-haku (:oid obj))
    (= (:type obj) "koulutus") ()))

(defn get-last-modified
  [since]
  (with-error-logging
    (let [url (str (:tarjonta-service-url env) "lastmodified")
          res (:body (client/get url {:query-params {:lastModified since} :as :json}))]
      (flatten
       (conj
        (map #(hash-map :type "haku" :oid %) (:haku res))
        (map #(hash-map :type "hakukohde" :oid %) (:hakukohde res))
        (map #(hash-map :type "koulutus" :oid %) (:koulutusmoduuliToteutus res)))))))

(defn get-hakukohteet-for-koulutus
  [koulutus-oid]
  (with-error-logging
    (let [url (str (:tarjonta-service-url env) "koulutus/" koulutus-oid "/hakukohteet")]
      (-> (client/get url {:as :json})
          :body
          :result))))

(defn get-haut-by-oids
  [oid-list]
  (with-error-logging
    (let [url (str (:tarjonta-service-url env) "haku/multi?oid=" (clojure.string/join "&oid=" oid-list))]
      (-> (client/get url {:as :json})
          :body
          :result))))
