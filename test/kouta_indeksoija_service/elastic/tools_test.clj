(ns kouta-indeksoija-service.elastic.tools-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.elastic.tools :as t]
            [clj-elasticsearch.elastic-connect :as e]
            [kouta-indeksoija-service.elastic.settings :as s]
            [kouta-indeksoija-service.elastic.tools :as tools]
            [clj-elasticsearch.elastic-utils :refer [max-payload-size bulk-partitions elastic-host]]
            [kouta-indeksoija-service.test-tools :refer [refresh-index reset-test-data debug-pretty]]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [kouta-indeksoija-service.indexer.indexable :as indexable]))

(defn dummy-indexdata
  [& {:keys [amount id-offset] :or {amount 10 id-offset 100}}]
  (let [create-data (fn [oid] (indexable/->index-entry oid (hash-map :oid oid :type "testi")))]
    (map #(create-data (+ % id-offset)) (range amount))))

(deftest bulk-update-data-test
  (testing "Payload for bulk operation"
   (testing "should be partitioned correctly when contain only index actions"
     (with-redefs [max-payload-size 2025]
       (let [docs (dummy-indexdata :amount 50 :id-offset 1000)
             data (tools/->bulk-actions "indexdata" docs)
             bulk-data (bulk-partitions data)]
         (is (= 3 (count bulk-data)))
         (println (nth bulk-data 0))
         (println "=======")
         (println (nth bulk-data 1))
         (println "=======")
         (println (nth bulk-data 2))

         (is (< (count (.getBytes (nth bulk-data 0))) 2025))
         (is (< (count (.getBytes (nth bulk-data 1))) 2025))
         (is (< (count (.getBytes (nth bulk-data 2))) 2025))

         (is (.startsWith (nth bulk-data 0) "{\"index"))
         (is (.startsWith (nth bulk-data 1) "{\"index"))
         (is (.startsWith (nth bulk-data 2) "{\"index")))))
   (testing "should be partitioned correctly when contain both index and delete actions"
     (with-redefs [max-payload-size 2025]
       (let [docs (concat (dummy-indexdata :amount 20 :id-offset 1000)
                          (vector (indexable/->delete-entry 1020))
                          (dummy-indexdata :amount 20 :id-offset 1021))
             data (tools/->bulk-actions "indexdata" docs)
             bulk-data (bulk-partitions data)]
         (println docs)
         (is (= 3 (count bulk-data)))
         (println (nth bulk-data 0))
         (println "=======")
         (println (nth bulk-data 1))
         (println "=======")
         (println (nth bulk-data 2))

         (is (< (count (.getBytes (nth bulk-data 0))) 2025))
         (is (< (count (.getBytes (nth bulk-data 1))) 2025))
         (is (< (count (.getBytes (nth bulk-data 2))) 2025))

         (is (.startsWith (nth bulk-data 0) "{\"index"))
         (is (.startsWith (nth bulk-data 1) "{\"index"))
         (is (.startsWith (nth bulk-data 2) "{\"index")))))))

(deftest bulk-upsert-test

  (let [index-name  (str "upsert-" (.toString (java.util.UUID/randomUUID)))
        test-data   (dummy-indexdata :amount 5)
        doc         (:doc (first test-data))
        get-doc     (fn [] (dissoc (tools/get-by-id index-name (:oid doc)) :timestamp))
        bulk-upsert (fn [x] (do (tools/bulk index-name x)
                                (tools/refresh-index index-name)))]

    (e/create-index index-name s/index-settings)
    (e/move-alias (t/->virkailija-alias index-name) index-name true)

    (testing "Bulk upsert should"
      (testing "create document if it doesn't exist"
        (is (nil? (get-doc)))
        (bulk-upsert test-data)
        ;(println (e/simple-search (tools/index-name index-name) "*"))
        (is (= doc (get-doc))))
      (testing "update document"
        (is (= doc (get-doc)))
        (println (type (first test-data)))
        (println (first test-data))
        (bulk-upsert (map (fn [d] (assoc d :doc (assoc (:doc d) :foo "foo"))) test-data))
        (is (= (assoc doc :foo "foo") (get-doc))))
      (testing "not merge document to old document"
        (is (= (assoc doc :foo "foo") (get-doc)))
        (bulk-upsert test-data)
        (is (= doc (get-doc)))))

    (e/delete-index index-name)))

(deftest parse-errors-test
  (let [index-name (str "not-existing-" (.toString (java.util.UUID/randomUUID)))
        test-data  (vector (indexable/->index-entry 1 {:foo 1})
                           (indexable/->index-entry 2 {:foo {:bar "2"}})
                           (indexable/->delete-entry 1)
                           (indexable/->delete-entry 2))
        bulk-upsert (fn [x] (let [result (tools/bulk index-name x)]
                                (tools/refresh-index index-name)
                                result))]

        (e/create-index index-name s/index-settings)
        (e/move-alias (t/->virkailija-alias index-name) index-name true)

        (testing "bulk upsert returns both errors and failures"
          (let [result (bulk-upsert test-data)]
            (is (= 2 (count result)))
            (is (= 1 (count (filter #(= (:result %) "not_found") result))))
            (is (= 1 (count (filter #(= (get-in % [:error :type]) "mapper_parsing_exception") result))))))))