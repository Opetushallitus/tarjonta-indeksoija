(ns kouta-indeksoija-service.elastic.settings)

(def index-settings
  {:index.mapping.total_fields.limit 2000
   :analysis {:filter {:ngram_compound_words_and_conjugations {:type "ngram" ;automaa utomaat tomaati omaatio maatioi aatioin atioins tioinsi ioinsin oinsinö insinöö nsinöör
                                                               :min_gram "5"
                                                               :max_gram "30"
                                                               :max_ngram_diff "35"
                                                               :token_chars ["letter", "digit"]}
                       :ngram_for_long_words {:type "condition"
                                              :filter ["ngram_compound_words_and_conjugations"]
                                              :script {:source "token.getTerm().length() > 4"}}
                       :finnish_stop {:type "stop"
                                      :stopwords "_finnish_"}
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
                                                    :language "possessive_english"}},
              :analyzer {:finnish {:type "custom"
                                   :tokenizer "standard"
                                   :filter ["lowercase"
                                            "finnish_stop"
                                            "ngram_for_long_words"
                                            "remove_duplicates"]}
                         :finnish_keyword {:type "custom"
                                           :tokenizer "standard"
                                           :filter ["lowercase"
                                                    "finnish_stop"
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
                                            "english_stemmer"]}}
              :normalizer {:case_insensitive {:filter "lowercase"}}}})

(def index-settings-eperuste (merge index-settings {:index.mapping.total_fields.limit 4000}))

(def stemmer-settings-eperuste
  {:dynamic_templates [{:fi {:match "kieli_fi"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "finnish"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256 :normalizer "case_insensitive"}}}}}
                       {:sv {:match "kieli_sv"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "swedish"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256 :normalizer "case_insensitive"}}}}}
                       {:en {:match "kieli_en"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "english"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256 :normalizer "case_insensitive"}}}}}]})

(def settings-koodisto
  {:dynamic_templates [{:nested {:match "koodit"
                                 :match_mapping_type "object"
                                 :mapping { :type "nested" }}}
                       {:fi {:match "fi"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "finnish"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:sv {:match "sv"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "swedish"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:en {:match "en"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "english"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}]})

(def kouta-settings-search
  {:dynamic_templates [{:nested {:match "hits"
                                 :match_mapping_type "object"
                                 :mapping { :type "nested" }}}
                       {:hakuOnKaynnissa {:match "hakuOnKaynnissa"
                                          :match_mapping_type "object"
                                          :mapping {:type "date_range"
                                                    :format "yyyy-MM-dd'T'HH:mm"}}}
                       {:fi {:match "fi"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "finnish"
                                       :search_analyzer "finnish_keyword"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:sv {:match "sv"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "swedish"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:en {:match "en"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "english"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}
                       {:tila {:match "tila"
                               :match_mapping_type "string"
                               :mapping {:type "text"
                                         :analyzer "finnish"
                                         :norms { :enabled false}
                                         :fields { :keyword { :type "keyword" :ignore_above 256}}}}}]})

(def kouta-settings
  {:dynamic_templates [{:muokkaaja {:match "muokkaaja.nimi"
                                    :match_mapping_type "string"
                                    :mapping {:type "text"
                                              :analyzer "finnish"
                                              :norms { :enabled false}
                                              :fields { :keyword { :type "keyword" :ignore_above 256 :normalizer "case_insensitive"}}}}}
                       {:fi {:match "fi"
                             :match_mapping_type "string"
                             :mapping {:type "text"
                                       :analyzer "finnish"
                                       :search_analyzer "finnish_keyword"
                                       :norms { :enabled false}
                                       :fields { :keyword { :type "keyword" :ignore_above 256 :normalizer "case_insensitive"}}}}}
                       {:tila {:match "tila"
                               :match_mapping_type "string"
                               :mapping {:type "text"
                                         :analyzer "finnish"
                                         :norms { :enabled false}
                                         :fields { :keyword { :type "keyword" :ignore_above 256 :normalizer "case_insensitive"}}}}}]})