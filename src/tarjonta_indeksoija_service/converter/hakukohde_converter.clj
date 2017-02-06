(ns tarjonta-indeksoija-service.converter.hakukohde-converter)

(defn convert
  [hakukohde]
  (-> hakukohde
      (update-in [:koulutukset] #(map :oid %))
      (update-in [:koulutusmoduuliToteutusTarjoajatiedot]
                 #(flatten
                    (for [[k v] %]
                      [{:koulutus (name k) :tarjoajaOids (:tarjoajaOids v)}])))))