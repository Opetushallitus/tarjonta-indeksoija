(ns kouta-indeksoija-service.indexer.tools.koulutustyyppi
  (:require [clojure.string :refer [join]]
            [kouta-indeksoija-service.indexer.tools.search :as search-tool]))

(defn assoc-koulutustyyppi-path
  ([entity koulutus toteutus-metadata]
   (assoc entity :koulutustyyppiPath (join "/" (search-tool/deduce-koulutustyypit koulutus toteutus-metadata))))
  ([entity koulutus]
   (assoc entity :koulutustyyppiPath (join "/" (search-tool/deduce-koulutustyypit koulutus)))))