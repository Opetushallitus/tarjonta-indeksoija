(ns mocks.tarjonta-pulssi-mocks
  (:require
   [clj-log.access-log]
   [clojure.string :as str]
   [mocks.export-elastic-data :refer [export-elastic-data]]
   [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
   [clj-test-utils.elasticsearch-docker-utils :as ed-utils]))

(defonce koulutus-oid-template "1.2.246.562.13.00000000000000000000")

(defn- oid
  [template oidNbr]
  (let [lastPart (last (str/split template #"\."))
        padWithLeadingZerosFormat (str "%0" (count lastPart) "d")
        newLastPart (format padWithLeadingZerosFormat oidNbr)]
    (str/replace template lastPart newLastPart)))

(defn- add-koulutukset
  [oidCounter nbr koulutustyyppi tila koodiUri]
  (dotimes [i nbr]
    (if koodiUri
      (fixture/add-koulutus-mock (oid koulutus-oid-template (+ oidCounter i))
                                 :koulutustyyppi koulutustyyppi :tila tila :koulutuksetKoodiUri koodiUri
                                 :metadata {:tyyppi koulutustyyppi})
      (fixture/add-koulutus-mock (oid koulutus-oid-template (+ oidCounter i))
                                 :koulutustyyppi koulutustyyppi :tila tila :metadata {:tyyppi koulutustyyppi})))
  (+ oidCounter nbr))

(defn -main []
  (ed-utils/start-elasticsearch)
  (fixture/init)
  (-> 1
      (add-koulutukset 2 "amm" "julkaistu" "koulutustyyppi_1")
      (add-koulutukset 1 "amm" "arkistoitu" "koulutustyyppi_26"))
  (export-elastic-data "tarjonta-pulssi")
  (ed-utils/stop-elasticsearch)
  )



