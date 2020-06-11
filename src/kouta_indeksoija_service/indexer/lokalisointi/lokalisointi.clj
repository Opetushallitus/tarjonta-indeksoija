(ns kouta-indeksoija-service.indexer.lokalisointi.lokalisointi
  (:require [kouta-indeksoija-service.rest.lokalisointi :as lokalisointi-service]
            [kouta-indeksoija-service.lokalisointi.util :as util]
            [kouta-indeksoija-service.indexer.indexable :as indexable]
            [cheshire.core :as cheshire]
            [clojure.string :refer [split]]))

(defn do-index
  [lng]
  (some-> (lokalisointi-service/get lng)
          (util/localisation->nested-json)))




