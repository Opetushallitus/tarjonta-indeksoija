(ns kouta-indeksoija-service.rest.lokalisointi
  (:require [kouta-indeksoija-service.util.urls :refer [resolve-url]]
            [kouta-indeksoija-service.rest.util :refer [get->json-body]]
            [kouta-indeksoija-service.rest.cas.session :refer [init-session cas-authenticated-request-as-json]]
            [cheshire.core :as cheshire]
            [kouta-indeksoija-service.util.time :refer :all]))

(defonce cas-session (init-session (resolve-url :lokalisointi.internal.base) true))

(defn do-get
  [lng]
  (get->json-body (resolve-url :lokalisointi.v1.localisation-category-locale "konfo" lng)))

(defn- ->post-request
  [category lng key value]
  {:body (cheshire/generate-string {:category category
                                    :key key
                                    :value value
                                    :locale lng})
   :content-type :json
   :force-redirects true})

(defn post
  [category lng key value]
  (cas-authenticated-request-as-json cas-session :post (resolve-url :lokalisointi.v1.localisation) (->post-request category lng key value)))