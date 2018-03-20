(ns tarjonta-indeksoija-service.converter.organisaatio-converter)

(defn convert [doc]
  (let [metadata-without-kuva (dissoc (:metadata doc) :kuvaEncoded)]
    (assoc (dissoc doc :metadata) :metadata metadata-without-kuva)))