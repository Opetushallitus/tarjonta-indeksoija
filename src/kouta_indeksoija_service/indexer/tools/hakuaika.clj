(ns kouta-indeksoija-service.indexer.tools.hakuaika)

(defn- ->to-distinct-vec
  [coll]
  (vec (set coll)))

(defn- kaytetaanHaunAikatauluaHakukohteessa?
  [hakukohde]
  (true? (:kaytetaanHaunAikataulua hakukohde)))

(defn- kaytetaanHaunAikatauluaHaussa?
  [haku]
  (and (seq (:hakukohteet haku))
       (some kaytetaanHaunAikatauluaHakukohteessa? (:hakukohteet haku))))

(defn- haun-hakuajat
  [haku]
  (when (kaytetaanHaunAikatauluaHaussa? haku)
    (:hakuajat haku)))

(defn- hakukohteet-hakuajat
  [hakukohde]
  (when (not (kaytetaanHaunAikatauluaHakukohteessa? hakukohde))
    (:hakuajat hakukohde)))

(defn- hakukohteiden-hakuajat
  [haku]
  (apply concat (map hakukohteet-hakuajat (:hakukohteet haku))))

(defn- ->hakuajat
  [haku]
  (concat (haun-hakuajat haku) (hakukohteiden-hakuajat haku)))

(defn ->real-hakuajat
  [hakutieto]
  (->to-distinct-vec (apply concat (map ->hakuajat (:haut hakutieto)))))
