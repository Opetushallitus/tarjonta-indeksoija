(ns misc.analyze-settings-test-tool
  (:gen-class)
  (:require [kouta-indeksoija-service.elastic.tools :as elastic]
            [clj-elasticsearch.elastic-utils :as u]
            [clj-elasticsearch.elastic-connect :as e]
            [kouta-indeksoija-service.indexer.indexable :refer [->index-entry]]
            [kouta-indeksoija-service.test-tools :refer [debug-pretty]]
            [clj-test-utils.elasticsearch-mock-utils :as mock]))

; Tämän työkalun tarkoituksena on helpottaa hakuindeksien erilaisten settingsien ja hakujen testausta.
; Tässä data löyhästi muistuttaa hakuindekseihin indeksoitavaa dataa ja search taas konfo-backendistä tehtävää kyselyä

(defonce port 9235)
(defonce test-index-name "analyze-settings-test-index")
(defonce elastic-url (str "http://localhost:" port))

(defn get-url
  [postfix]
  (str elastic-url "/" test-index-name "/" postfix))

(defonce settings {:analysis {:filter {:compound_words_filter {:type "ngram"
                                                               :min_gram "3"
                                                               :max_gram "30"
                                                               :max_ngram_diff "35"
                                                               :token_chars ["letter", "digit"]}
                                       :conditional_filter {:type "condition"
                                                            :filter ["compound_words_filter"]
                                                            :script {:source "token.getTerm().length() > 3"}}
                                       :finnish_stop {:type "stop"
                                                      :stopwords "_finnish_"}
                                       :finnish_stemmer {:type "stemmer"
                                                         :language "finnish"}
                                       :conditional_stemmer {:type "condition"
                                                             :filter ["finnish_stemmer"]
                                                             :script {:source "token.getTerm().length() > 5"}}},
                              :analyzer {:finnish {:type "custom"
                                                   :tokenizer "standard"
                                                   :filter ["lowercase"
                                                            "finnish_stop"
                                                            "conditional_filter"
                                                            "remove_duplicates"]}
                                         :finnish_keyword {:type "custom"
                                                           :tokenizer "standard"
                                                           :filter ["lowercase"
                                                                    "finnish_stop"
                                                                    "conditional_stemmer"]}}}})

(defonce mappings {:dynamic_templates [{:nested {:match "hits"
                                                 :match_mapping_type "object"
                                                 :mapping { :type "nested" }}}
                                       {:fi {:match "fi"
                                             :match_mapping_type "string"
                                             :mapping {:type "text"
                                                       :analyzer "finnish"
                                                       :search_analyzer "finnish_keyword"
                                                       :norms { :enabled false}
                                                       :fields { :keyword { :type "keyword" :ignore_above 256}}}}}]})

(def oid1  "1.1")
(def oid2  "1.2")
(def oid3  "1.3")
(def oid4  "1.4")
(def oid5  "1.5")
(def oid6  "1.6")
(def oid7  "1.7")
(def oid8  "1.8")
(def oid9  "1.9")
(def oid10 "1.10")
(def oid11 "1.11")
(def oid12 "1.12")
(def oid13 "1.13")
(def oid14 "1.14")
(def oid15 "1.15")
(def oid16 "1.16")
(def oid17 "1.17")
(def oid18 "1.18")
(def oid19 "1.19")

(defn bulk-test-data
  [index]
  (elastic/bulk index [(->index-entry oid1  {:hits [{:terms [{:fi ["Lääketieteen koulutus" "lääkäri"] :sug ["Lääketieteen koulutus" "lääkäri"]}]}]})
                       (->index-entry oid2  {:hits [{:terms [{:fi ["Humanistinen koulutus (ylempi AMK)" "psykologi" "ammattikorkeakoulu"]}]}]})
                       (->index-entry oid3  {:hits [{:terms [{:fi ["Tietojenkäsittelytieteen koulutus"]}]}]})
                       (->index-entry oid4  {:hits [{:terms [{:fi ["Automaatiotekniikka" "automaatioinsinööri" "ammattioppilaitos"]}]}]})
                       (->index-entry oid5  {:hits [{:terms [{:fi ["Muusikon koulutus" "musiikkioppilaitokset"]}]}]})
                       (->index-entry oid6  {:hits [{:terms [{:fi ["Sosiaali- ja terveysalan perustutkinto"]}]}]})
                       (->index-entry oid7  {:hits [{:terms [{:fi ["Maanmittausalan perustutkinto" "lääkekokeilu"] :sug ["Maanmittausalan perustutkinto" "lääkekokeilu"]}]}]})
                       (->index-entry oid8  {:hits [{:terms [{:fi ["Pintakäsittelyalan perustutkinto" "maalari" "erikoistumislinja"]}]}]})
                       (->index-entry oid9  {:hits [{:terms [{:fi ["Puhtaus- ja kiinteistöpalvelualan ammattitutkinto"]}]}]})
                       (->index-entry oid10 {:hits [{:terms [{:fi ["Puhevammaisten tulkkauksen erikoisammattitutkinto"]}]}]})
                       (->index-entry oid11 {:hits [{:terms [{:fi ["Hius- ja kauneudenhoitoalan perustutkinto"]}]}]})
                       (->index-entry oid12 {:hits [{:terms [{:fi ["Autoalan perustutkinto" "automaalari"]}]}]})
                       (->index-entry oid13 {:hits [{:terms [{:fi ["Elintarvikealan perustutkinto" "esimies"]}]}]})
                       (->index-entry oid14 {:hits [{:terms [{:fi ["Eläintenhoidon ammattitutkinto" "eläintenhoitaja"]}]}]})
                       (->index-entry oid15 {:hits [{:terms [{:fi ["Tieto- ja viestintätekniikan ammattitutkinto"]}]}]})
                       (->index-entry oid16 {:hits [{:terms [{:fi ["Tanssialan perustutkinto"]}]}]})
                       (->index-entry oid17 {:hits [{:terms [{:fi ["Merenkulkualan ammattitutkinto" "merimies"]}]}]})
                       (->index-entry oid18 {:hits [{:terms [{:fi ["Hevostalouden perustutkinto" "hevostenhoitaja"]}]}]})
                       (->index-entry oid19 {:hits [{:terms [{:fi ["Pelastusalan tutkinto" "ensihoitaja"]}]}]})])
  (e/refresh-index index))

(defn search
  [keyword]
  (let [res (u/elastic-post (get-url "_search")
                            {:size 15
                             :query {:nested {:path "hits", :query {:bool {:must {:match {:hits.terms.fi {:query keyword :operator "and" :fuzziness "AUTO:8,12"}}}}}}}})] ;Tämä matkii konfo-backedin tekemää hakua
    ;(debug-pretty res)
    (vec (map :_id (get-in res [:hits :hits])))))

(defn execute-test-search
  []
  (let [exec (fn [t e] (let [r (search t)] (println (= (set e) (set r)) "=" t (sort r) "expected=" (sort e))))]
    ;(exec "vammainen" [oid10])
    (exec "tutkinto" [oid6 oid7 oid8 oid9 oid10 oid11 oid12 oid13 oid14 oid15 oid16 oid17 oid18 oid19])
    (exec "erikoisammattitutkinto" [oid10])
    (exec "perust" [oid6 oid7 oid8 oid11 oid12 oid13 oid16 oid18])
    (exec "maalari" [oid8 oid12])                           ;(EI maanmittausala)
    (exec "puhtaus" [oid9])                                 ;(EI puhevammaisten)
    (exec "palvelu" [oid9])
    (exec "ammattitutkinto" [oid9 oid10 oid14 oid15 oid17])                   ;EI ammattioppilaitos tai ammattikorkeakoulu
    ;(exec "sosiaaliala" [oid6])
    (exec "terveys" [oid6])
    (exec "musiikkioppilaitos" [oid5])
    (exec "auto" [oid4 oid12])
    (exec "automaatio" [oid4])
    (exec "humanismi" [oid2])
    (exec "lääketiede" [oid1])
    (exec "muusikko" [oid5])
    (exec "insinööri" [oid4])
    (exec "ins" [oid4])
    (exec "tekniikka" [oid4 oid15])
    (exec "muusikon koulutus" [oid5])
    (exec "maanmittausalan perus" [oid7])           ; ei muita perustutkintoja
    (exec "tietojenkäsittelytiede" [oid3])
    ;(exec "automaatiikka" [oid4])
    (exec "hius" [oid11])
    (exec "kauneudenhoito" [oid11])
    ;(exec "hoito" [oid11 oid14 oid18 oid19])
    (exec "psykologia" [oid2])
    ;(exec "tiede" [oid1 oid3])
    (exec "lääk" [oid1 oid7])
    (exec "eläin" [oid14])
    (exec "eläinten" [oid14])
    (exec "merimies" [oid17])
    (exec "sosiaali" [oid6])
    (exec "tie" [oid1 oid3 oid15])
    (exec "ensihoitaja" [oid19])))

(defn -main
  []
  (intern 'clj-elasticsearch.elastic-utils 'elastic-host elastic-url)
  (try
    (mock/start-embedded-elasticsearch port)
    (e/delete-index test-index-name)
    (e/create-index test-index-name settings)
    (u/elastic-put (get-url (str "_mappings/" test-index-name)) mappings)
    (bulk-test-data test-index-name)
    ;(debug-pretty (u/elastic-get (get-url "_mappings")))
    ;(debug-pretty (u/elastic-get (get-url "_search?q=*")))
    (execute-test-search)
    (e/delete-index test-index-name)
    (finally (mock/stop-elastic-test))))