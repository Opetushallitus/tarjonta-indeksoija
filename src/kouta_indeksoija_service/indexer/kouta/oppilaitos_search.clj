(ns kouta-indeksoija-service.indexer.kouta.oppilaitos-search
  (:require [kouta-indeksoija-service.rest.kouta :as kouta-backend]
            [kouta-indeksoija-service.rest.koodisto :refer [get-koodi-nimi-with-cache]]
            [kouta-indeksoija-service.rest.organisaatio :as organisaatio-client]
            [kouta-indeksoija-service.indexer.tools.organisaatio :as organisaatio-tool]
            [kouta-indeksoija-service.indexer.tools.hakuaika :refer [->real-hakuajat]]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [kouta-indeksoija-service.indexer.tools.general :refer :all]
            [kouta-indeksoija-service.indexer.tools.search :refer :all]))

(def index-name "oppilaitos-kouta-search")

(defn- tarjoaja-organisaatiot
  [oppilaitos tarjoajat]
  (vec (map #(organisaatio-tool/find-from-organisaatio-and-children oppilaitos %) tarjoajat)))

(defn oppilaitos-hit
  [oppilaitos]
  (hit :opetuskieliUrit (:kieletUris oppilaitos)
       :tarjoajat       (vector oppilaitos)
       :oppilaitokset   (vector oppilaitos)))

(defn koulutus-hit
  [oppilaitos koulutus]
  (hit :koulutustyyppi  (:koulutustyyppi koulutus)
       ;:opetuskieliUrit (:kieletUris oppilaitos)
       :tarjoajat       (tarjoaja-organisaatiot oppilaitos (:tarjoajat koulutus))
       :oppilaitokset   (vector oppilaitos)
       :koulutusalaUrit (koulutusalaKoodiUrit koulutus)
       :nimi            (:nimi koulutus)))

(defn toteutus-hit
  [oppilaitos koulutus toteutus]
  (hit :koulutustyyppi   (:koulutustyyppi koulutus)
       :opetuskieliUrit  (get-in toteutus [:metadata :opetus :opetuskieliKoodiUrit])
       :tarjoajat        (tarjoaja-organisaatiot oppilaitos (:tarjoajat toteutus))
       :oppilaitokset   (vector oppilaitos)
       :koulutusalaUrit  (koulutusalaKoodiUrit koulutus)
       :nimi             (:nimi toteutus)
       :asiasanat        (asiasana->lng-value-map (get-in toteutus [:metadata :asiasanat]))
       :ammattinimikkeet (asiasana->lng-value-map (get-in toteutus [:metadata :ammattinimikkeet]))))

(defn- get-kouta-oppilaitos
  [oid]
  (let [oppilaitos (kouta-backend/get-oppilaitos oid)]
    (when (julkaistu? oppilaitos)
      {:kielivalinta (:kielivalinta oppilaitos)
       :kuvaus (get-in oppilaitos [:metadata :esittely])})))

(defn- create-base-entry
  [oppilaitos koulutukset]
  (-> oppilaitos
      (select-keys [:oid :nimi])
      (merge (get-kouta-oppilaitos (:oid oppilaitos)))
      (assoc :koulutusohjelmia (count (filter :johtaaTutkintoon koulutukset)))))

(defn- remove-not-allowed-tarjoaja-oids
  [allowed-tarjoaja-oids entry]
  (assoc entry :tarjoajat (vec (clojure.set/intersection (set (:tarjoajat entry)) (set allowed-tarjoaja-oids)))))

(defn- filter-and-reduce-entries-by-tarjoajat
  [allowed-tarjoaja-oids entries]
  (seq (filter #(not (empty? (:tarjoajat %))) (map #(remove-not-allowed-tarjoaja-oids allowed-tarjoaja-oids %) entries))))

(defn- assoc-paikkakunnat
  [entry]
  (assoc entry :paikkakunnat (vec (distinct (filter #(clojure.string/starts-with? % "kunta") (mapcat :sijainti (:hits entry)))))))

(defn create-index-entry
  [oid]
  (let [hierarkia (organisaatio-client/get-hierarkia-v4 oid :aktiiviset true :suunnitellut false :lakkautetut false :skipParents false)]
    (when-let [oppilaitos (organisaatio-tool/find-oppilaitos-from-hierarkia hierarkia)]
      (let [allowed-tarjoaja-oids (map :oid (organisaatio-tool/get-organisaatio-keys-flat hierarkia [:oid]))]

        (defn- koulutus-hits
          [koulutus]
          (if-let [toteutukset (filter-and-reduce-entries-by-tarjoajat allowed-tarjoaja-oids (kouta-backend/get-toteutus-list-for-koulutus (:oid koulutus) true))]
            (vec (map #(toteutus-hit oppilaitos koulutus %) toteutukset))
            (vector (koulutus-hit oppilaitos koulutus))))

        (let [koulutukset (filter-and-reduce-entries-by-tarjoajat allowed-tarjoaja-oids (kouta-backend/get-koulutukset-by-tarjoaja (:oid oppilaitos)))]
          (-> oppilaitos
              (create-base-entry koulutukset)
              (assoc :hits (if koulutukset
                             (vec (mapcat #(koulutus-hits %) koulutukset))
                             (vector (oppilaitos-hit oppilaitos))))
              (assoc-paikkakunnat)))))))

(defn create-index-entries
  [oids]
  (doall (pmap create-index-entry oids)))

(defn do-index
  [oids]
  (indexable/do-index index-name oids create-index-entries))

(defn get
  [oid]
  (indexable/get index-name oid))