(ns kouta-indeksoija-service.queue.conf
  (:refer-clojure :exclude [name])
  (:require [kouta-indeksoija-service.util.conf :refer [env]]
            [clojure.string :refer [blank?]]))

(defn- ->not-blank
  [s]
  (if (not (blank? s))
    s))

(defonce sqs-endpoint (or (->not-blank (:sqs-endpoint env)) (->not-blank (:sqs-region env))))

(defn priorities
  []
  (keys (:queue env)))

(defn name
  [priority]
  (get-in (:queue env) [(keyword priority) :name]))

(defn health-threshold
  [priority]
  (get-in (:queue env) [(keyword priority) :health-threshold]))