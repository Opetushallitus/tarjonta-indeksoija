(ns tarjonta-indeksoija-service.indexer-test
  (:require [tarjonta-indeksoija-service.indexer :as indexer]
            [tarjonta-indeksoija-service.elastic-client :as elastic-client]
            [mocks.tarjonta-mock :as mock]
            [midje.sweet :refer :all]))

(def index-identifier (.getTime (java.util.Date.)))

(defn index-name
  [base-name]
  (str base-name "_" index-identifier))

(def hakukohde-index (index-name "hakukohde_test"))
(def koulutus-index (index-name "koulutus_test"))

(against-background
  [(after :contents [(elastic-client/delete-index hakukohde-index)
                     (elastic-client/delete-index koulutus-index)])]

  (fact "Indexer should save hakukohde"
        (let [oid "1.2.246.562.20.99178639649"]
          (mock/with-mock {:oid oid :type hakukohde-index}
                          (indexer/index-object {:oid oid :type hakukohde-index}))
          (elastic-client/get-by-id hakukohde-index hakukohde-index oid) => (contains {:oid oid})))

  (fact "Indexer should save koulutus"
        (let [oid "1.2.246.562.17.81687174185"]
          (mock/with-mock {:oid oid :type koulutus-index}
                          (indexer/index-object {:oid oid :type koulutus-index}))
          (let [indexed-koulutus (elastic-client/get-by-id koulutus-index koulutus-index oid)]
            indexed-koulutus => (contains {:oid oid})
            (get-in indexed-koulutus [:koulutuskoodi :uri]) => "koulutus_371101"
            (get-in indexed-koulutus [:valmistavaKoulutus :kuvaus :SISALTO :kieli_fi])
            => "<p><a href=\"http://www.hyria.fi/koulutukset/aikuiskoulutukset/koulutushaku?e=3807&amp;i=3061\">Tutustu tästä tarkemmin koulutuksen sisältöön.</a></p> <p> </p> <p>Oppisopimuskoulutus mahdollinen</p>"))))