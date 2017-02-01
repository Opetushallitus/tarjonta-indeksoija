(ns tarjonta-indeksoija-service.converter.hakukohde-converter)

(defn convert
  [hakukohde]
  (update-in hakukohde [:koulutukset] #(map :oid %)))