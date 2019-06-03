(ns kouta-indeksoija-service.elastic.tools-test
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-connect :as e]
            [kouta-indeksoija-service.elastic.settings :as s]
            [kouta-indeksoija-service.elastic.tools :as tools]
            [clj-elasticsearch.elastic-utils :refer [max-payload-size bulk-partitions elastic-host]]
            [kouta-indeksoija-service.test-tools :refer [refresh-index reset-test-data]]
            [clj-test-utils.elasticsearch-mock-utils :refer :all]
            [kouta-indeksoija-service.elastic.admin :as admin]))

(defn dummy-indexdata
  [& {:keys [amount id-offset] :or {amount 10 id-offset 100}}]
  (map #(hash-map :oid (+ % id-offset) :type "testi") (range amount)))

(deftest bulk-update-data-test
  (testing "Payload for bulk operation should be partitioned correctly"
    (with-redefs [max-payload-size 2025]
      (let [docs (dummy-indexdata :amount 50 :id-offset 1000)
            data (tools/bulk-upsert-data "indexdata" "indexdata" docs)
            bulk-data (bulk-partitions data)]
        (is (= 4 (count bulk-data)))
        (println (nth bulk-data 0))
        (println "=======")
        (println (nth bulk-data 1))
        (println "=======")
        (println (nth bulk-data 2))
        (println "=======")
        (println (nth bulk-data 3))

        (is (< (count (.getBytes (nth bulk-data 0))) 2025))
        (is (< (count (.getBytes (nth bulk-data 1))) 2025))
        (is (< (count (.getBytes (nth bulk-data 2))) 2025))
        (is (< (count (.getBytes (nth bulk-data 3))) 2025))

        (is (.startsWith (nth bulk-data 0) "{\"index"))
        (is (.startsWith (nth bulk-data 1) "{\"index"))
        (is (.startsWith (nth bulk-data 2) "{\"index") )
        (is (.startsWith (nth bulk-data 3) "{\"index"))))))

(deftest bulk-upsert-test

  (let [index-name  (str "upsert-" (.toString (java.util.UUID/randomUUID)))
        test-data   (dummy-indexdata :amount 5)
        doc         (first test-data)
        get-doc     (fn [] (dissoc (tools/get-by-id index-name index-name (:oid doc)) :timestamp))
        bulk-upsert (fn [x] (do (tools/bulk-upsert index-name index-name x)
                                (tools/refresh-index index-name)))]

    (e/create-index (tools/index-name index-name) s/index-settings)

    (testing "Bulk upsert should"
      (testing "create document if it doesn't exist"
        (is (nil? (get-doc)))
        (bulk-upsert test-data)
        ;(println (e/simple-search (tools/index-name index-name) "*"))
        (is (= doc (get-doc))))
      (testing "update document"
        (is (= doc (get-doc)))
        (bulk-upsert (map #(assoc % :foo "foo") test-data))
        (is (= (assoc doc :foo "foo") (get-doc))))
      (testing "not merge document to old document"
        (is (= (assoc doc :foo "foo") (get-doc)))
        (bulk-upsert test-data)
        (is (= doc (get-doc)))))

    (e/delete-index (tools/index-name index-name))))