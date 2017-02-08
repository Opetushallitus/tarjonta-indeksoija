(ns tarjonta-indeksoija-service.conf
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [defstate start]]
            [clojurewerkz.quartzite.scheduler :as qs]))

(defstate env :start (load-config :merge [(source/from-system-props) (source/from-env)]))

(defstate job-pool :start (qs/start (qs/initialize)))

(def analyzer-settings
  {:analysis {:filter {:finnish_stop {:type "stop"
                                      :stopwords "_finnish_"}
                       :finnish_keywords {:type "keyword_marker"
                                          :keywords "_finnish_keywords_"}
                       :finnish_stemmer {:type "stemmer"
                                         :language "finnish"}
                       :swedish_stop {:type "stop"
                                      :stopwords "_swedish_"}
                       :swedish_keywords {:type "keyword_marker"
                                          :keywords "_swedish_keywords_"}
                       :swedish_stemmer {:type "stemmer"
                                         :language "swedish"}
                       :english_stop {:type "stop"
                                      :stopwords "_english_"}
                       :english_keywords {:type "keyword_marker"
                                          :keywords "_english_keywords_"}
                       :english_stemmer {:type "stemmer"
                                         :language "english"}
                       :english_possessive_stemmer {:type "stemmer"
                                                    :language "possessive_english"}}
              :analyzer {:finnish {:tokenizer "standard"
                                   :filter ["lowercase"
                                            "finnish_stop"
                                            "finnish_keywords"
                                            "finnish_stemmer"]}
                         :swedish {:tokenizer "standard"
                                   :filter ["lowercase"
                                            "swedish_stop"
                                            "swedish_keywords"
                                            "swedish_stemmer"]}
                         :english {:tokenizer "standard"
                                   :filter ["english_possessive_stemmer"
                                            "lowercase"
                                            "english_stop"
                                            "english_keywords"
                                            "english_stemmer"]}}}})

(def stemmer-settings
  {:dynamic_templates [{:fi {:match "kieli_fi"
                             :match_mapping_type "string"
                             :mapping {:type "string"
                                       :analyzer "finnish"}}}
                       {:sv {:match "kieli_sv"
                             :match_mapping_type "string"
                             :mapping {:type "string"
                                       :analyzer "swedish"}}}
                       {:en {:match "kieli_en"
                             :match_mapping_type "string"
                             :mapping {:type "string"
                                       :analyzer "english"}}}]})

(def indexdata-mappings
  {:properties {:oid {:type "text"
                                             :fields {:keyword {:type "keyword"
                                                                :ignore_above 256}}}
                                       :timestamp {:type "long"}
                                       :type {:type "text"
                                              :fields {:keyword {:type "keyword"
                                                                 :ignore_above 256}}}}})