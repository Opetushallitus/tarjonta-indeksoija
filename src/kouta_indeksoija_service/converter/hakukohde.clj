(ns kouta-indeksoija-service.converter.hakukohde)

(defn convert
  [hakukohde]
  (-> hakukohde
      (update-in [:koulutukset] #(map :oid %))
      (update-in [:koulutusmoduuliToteutusTarjoajatiedot]
                 #(flatten
                   (for [[k v] %]
                     [{:koulutus (name k) :tarjoajaOids (:tarjoajaOids v)}])))))
