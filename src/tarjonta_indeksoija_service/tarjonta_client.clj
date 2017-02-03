(ns tarjonta-indeksoija-service.tarjonta-client
  (:require [tarjonta-indeksoija-service.conf :refer [env]]
            [clj-http.client :as client]))

(defn get-url
  [type path]
  (condp = type
    "hakukohde" (str (:tarjonta-service-url env) "hakukohde/" path)
    "koulutus" (str (:tarjonta-service-url env) "koulutus/" path)
    "haku" (str (:tarjonta-service-url env) "haku/" path)))

(defn get-doc
  [obj]
  (let [url (get-url (:type obj) (:oid obj))]
    (-> (client/get url {:as :json})
        :body
        :result)))

(defn- extract-koulutus-hakukohde-docs
  [type result]
  (->> result
       :body
       :result
       :tulokset
       (map :tulokset)
       (flatten)
       (map :oid)
       (map #(assoc {} :type type :oid %))))

(defn- find-haku-docs
  [params]
  (if (contains? params :oid)
    [{:type "haku" :oid (:oid params)}]
    (let [params-with-defaults (merge {:tarjoajaoid "1.2.246.562.10.00000000001"
                                       :tila "NOT_POISTETTU"} params)
          url (get-url "haku" "find")]
      (->> (client/get url {:query-params params-with-defaults :as :json})
           :body
           :result
           (map :oid)
           (map #(assoc {} :type "haku" :oid %))))))

(defn find-docs
  [type params]
  (if (= type "haku")
    (find-haku-docs params)
    (let [params-with-defaults (merge {:TILA "NOT_POISTETTU"} params)
          url (get-url type "search")]
      (extract-koulutus-hakukohde-docs type
        (client/get url {:query-params params-with-defaults, :as :json})))))

(defn get-last-modified
  [since]
  (let [url (str (:tarjonta-service-url env) "lastmodified")
        res (:body (client/get url {:query-params {:lastModified since} :as :json}))]
    (flatten
      (conj
        (map #(hash-map :type "haku" :oid %) (:haku res))
        (map #(hash-map :type "hakukohde" :oid %) (:hakukohde res))))))