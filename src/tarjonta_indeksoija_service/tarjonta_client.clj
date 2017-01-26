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

(defn- extract-koulutus-hakukohde-docs [type result]
  (->> result
       :body
       :result
       :tulokset
       (map :tulokset)
       (flatten)
       (map :oid)
       (map #(assoc {} :type type :oid %))))

(defn- find-haku-docs [params]
  (let [params-with-defaults (merge {:TARJOAJAOID "1.2.246.562.10.00000000001" :TILA "NOT_POISTETTU"} params)
        url (get-url "haku" "find")]
    (->> (client/get url {:query-params params-with-defaults :as :json})
         :body
         :result
         (map :oid)
         (map #(assoc {} :type "haku" :oid %)))))

(defn find-docs
  [type params]
  (if (.contains type "haku")
    (find-haku-docs params)
    (let [params-with-defaults (merge {:TILA "NOT_POISTETTU"} params)
          url (get-url type "search")]
      (->> (client/get url {:query-params params-with-defaults :as :json})
           (extract-koulutus-hakukohde-docs type)))))

