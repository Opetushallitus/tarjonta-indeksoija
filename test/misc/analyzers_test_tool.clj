(ns misc.analyzers-test-tool
  (:gen-class)
  (:require [kouta-indeksoija-service.elastic.tools :as elastic]
            [clj-elasticsearch.elastic-utils :as u]
            [clj-elasticsearch.elastic-connect :as e]
            [kouta-indeksoija-service.indexer.indexable :refer [->index-entry]]
            [kouta-indeksoija-service.test-tools :refer [debug-pretty]]
            [clj-test-utils.elasticsearch-mock-utils :as mock]))

; Tämän työkalulla voi testailla, millä tavalla erilaiset analysaattorit indeksoivat/analysoivat sanoja

(defonce port 9236)
(defonce test-index-name "analyzers-test-index")
(defonce elastic-url (str "http://localhost:" port))

(defonce analyzers {:analysis {:filter {:compound_words_filter {:type "ngram" ;automaa utomaat tomaati omaatio maatioi aatioin atioins tioinsi ioinsin oinsinö insinöö nsinöör
                                                                :min_gram "4"
                                                                :max_gram "30"
                                                                :token_chars ["letter", "digit"]}
                                        :conditional_filter {:type "condition"
                                                             :filter ["compound_words_filter"]
                                                             :script {:source "token.getTerm().length() > 4"}}
                                        :edge_ngram_long_words_4_7 {:type "edge_ngram" ;auto autom automa automaa
                                                                    :min_gram "4"
                                                                    :max_gram "7"}
                                        :edge_ngram_long_words_6_8 {:type "edge_ngram" ;auto autom automa automaa
                                                                    :min_gram "6"
                                                                    :max_gram "8"}
                                        :edge_ngram_long_words_4_20 {:type "edge_ngram" ;auto autom automa automaa
                                                                     :min_gram "4"
                                                                     :max_gram "20"}
                                        :truncate_search_keyword_7 {:type "truncate"
                                                                    :length "7"} ;pitää olla sama kuin max_gram edge n-gram filterissä!
                                        :truncate_search_keyword_8 {:type "truncate"
                                                                    :length "8"} ;pitää olla sama kuin max_gram edge n-gram filterissä!
                                        :truncate_search_keyword_20 {:type "truncate"
                                                                     :length "20"} ;pitää olla sama kuin max_gram edge n-gram filterissä!
                                        :finnish_stop {:type "stop"
                                                        :stopwords "_finnish_"}
                                        :finnish_stemmer {:type "stemmer"
                                                           :language "finnish"}
                                        :finnish_snowball {:type "snowball"
                                                            :language "finnish"}}
                               :tokenizer {:ngram_compound_words_tokenizer {:type "ngram" ;automaa utomaat tomaati omaatio maatioi aatioin atioins tioinsi ioinsin oinsinö insinöö nsinöör
                                                                            :min_gram "6"
                                                                            :max_gram "12"
                                                                            :token_chars ["letter", "digit"]}}
                               :analyzer {:conditional {:type "custom"
                                                        :tokenizer "standard"
                                                        :filter ["lowercase"
                                                                 "finnish_stop"
                                                                 "conditional_filter"
                                                                 "remove_duplicates"]}
                                          :keywordish {:type "custom"
                                                       :tokenizer "standard"
                                                       :filter ["lowercase"
                                                                "finnish_stop"
                                                                "finnish_stemmer"]}
                                          :finnish_edge_6_8 {:type "custom"
                                                             :tokenizer "ngram_compound_words_tokenizer"
                                                             :filter ["lowercase"
                                                                      "finnish_stop"
                                                                      "finnish_stemmer"
                                                                      "edge_ngram_long_words_6_8"]}
                                          :finnish_keyword_6_8 {:type "custom"
                                                                :tokenizer "standard"
                                                                :filter ["lowercase"
                                                                         "finnish_stop"
                                                                         "finnish_stemmer"
                                                                         "truncate_search_keyword_8"]}
                                          :finnish_edge_4_7 {:type "custom"
                                                             :tokenizer "ngram_compound_words_tokenizer"
                                                             :filter ["lowercase"
                                                                      "finnish_stop"
                                                                      "finnish_stemmer"
                                                                      "edge_ngram_long_words_4_7"]}
                                          :finnish_keyword_4_7 {:type "custom"
                                                                :tokenizer "standard"
                                                                :filter ["lowercase"
                                                                         "finnish_stop"
                                                                         "finnish_stemmer"
                                                                         "truncate_search_keyword_7"]}
                                          :finnish_edge_4_20 {:type "custom"
                                                              :tokenizer "ngram_compound_words_tokenizer"
                                                              :filter ["lowercase"
                                                                       "finnish_stop"
                                                                       "finnish_stemmer"
                                                                       "edge_ngram_long_words_4_20"]}
                                          :finnish_keyword_4_20 {:type "custom"
                                                                 :tokenizer "standard"
                                                                 :filter ["lowercase"
                                                                          "finnish_stop"
                                                                          "finnish_stemmer"
                                                                          "truncate_search_keyword_20"]}
                                          :finnish_compunds {:type "custom"
                                                             :tokenizer "ngram_compound_words_tokenizer"
                                                             :filter ["lowercase"
                                                                      "finnish_stop"
                                                                      "finnish_stemmer"]}
                                          :finnish_snowball {:type "custom"
                                                             :tokenizer "standard"
                                                             :filter ["lowercase"
                                                                      "finnish_stop"
                                                                      "finnish_snowball"]}
                                          :finnish_stemmer {:type "custom"
                                                            :tokenizer "standard"
                                                            :filter ["lowercase"
                                                                     "finnish_stop"
                                                                     "finnish_stemmer"]}
                                          :finnish_keyword {:type "custom"
                                                            :tokenizer "keyword"
                                                            :filter ["lowercase"
                                                                     "finnish_stop"
                                                                     "finnish_stemmer"]}}}})

(defn analyze
  [index text]
  (doseq [analyzer (vector :conditional :keywordish  :finnish_snowball)                                         ;(vec (keys (get-in analyzers [:analysis :analyzer])))
    ]
    (let [res (u/elastic-post (str elastic-url "/" index "/_analyze") {:analyzer analyzer :text text})]
      (println analyzer "=>" (vec (map :token (:tokens res))))
      ;(debug-pretty res)
      )))

(defn execute-test-search
  [index]
  (analyze index "Puhtaus- ja kiinteistöpalvelualan ammattitutkinto")
  (println "=================")
  (analyze index "Hius- ja kauneudenhoitoalan erikoisammattitutkinto")
  (println "=================")
  (analyze index "Hoito")
  (println "=================")
  (analyze index "Lääketieteen koulutus (ylempi AMK)")
  (println "=================")
  (analyze index "Tiede")
  (println "=================")
  (analyze index "Vaa'anmittauksen erikoisammattikoulutus - (alempi AMK)")
  (println "=================")
  (analyze index "Tietojenkäsittelytieteen koulutus")
  (println "=================")
  (analyze index "Tietojenkäsittelytiede")
  (println "=================")
  (analyze index "Sosiaali- ja terveysalan perustutkinto")
  (println "=================")
  (analyze index "Sosiaaliala")
  (println "=================")
  (analyze index "Maalari")
  (println "=================")
  (analyze index "Automaalari")
  (println "=================")
  (analyze index "Eläintenhoidon ammattitutkinto")
  (println "=================")
  (analyze index "Eläin")
  (println "=================")
  (analyze index "Eläinten")
  (println "=================")
  (analyze index "Elin")
  (println "=================")
  (analyze index "Kauneus")
  (println "=================")
  (analyze index "Koira")
  (println "=================")
  (analyze index "Tiede")
  (println "=================")
  (analyze index "Hius")
  (println "=================")
  (analyze index "Ensihoitaja")
  (println "=================")
  (analyze index "Lähihoitaja")
  (println "=================")
  (analyze index "Sosiaali")
  (println "=================")
  (analyze index "Sosiaaliala")
  (println "=================")
  (analyze index "Tanssialan perustutkinto")
  (println "=================")
  (analyze index "Autoalan perustutkinto")
  (println "=================")
  (analyze index "Musiikkioppilaitos")
  (println "=================")
  (analyze index "Musiikkioppilaitokset")
  (println "=================")
  (analyze index "Seppä"))

(defn -main
  []
  (intern 'clj-elasticsearch.elastic-utils 'elastic-host elastic-url)
  (try
    (mock/start-embedded-elasticsearch port)
    (e/delete-index test-index-name)
    (e/create-index test-index-name analyzers)
    (execute-test-search test-index-name)
    (e/delete-index test-index-name)
    (finally (mock/stop-elastic-test))))