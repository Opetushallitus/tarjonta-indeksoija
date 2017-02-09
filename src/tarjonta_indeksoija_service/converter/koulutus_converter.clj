(ns tarjonta-indeksoija-service.converter.koulutus-converter)

(defn- extract-koodi
  [value]
  {:uri  (:uri value)
   :nimi {:kieli_fi (get-in value [:meta :kieli_fi :nimi])
          :kieli_sv (get-in value [:meta :kieli_sv :nimi])
          :kieli_en (get-in value [:meta :kieli_en :nimi])}})

(defn- extract-koodi-list
  [value path-to-koodi-list]
  (map extract-koodi (vals (get-in value path-to-koodi-list))))

(defn- value [value] value)

(defn- koodi [value] (extract-koodi value))

(defn- koodi-list [value] (extract-koodi-list value [:meta]))

(defn- kuvaus
  [value]
  (reduce-kv #(assoc %1 %2 (:tekstis %3))
             {}
             value))

(defn- kielivalikoima
  [value]
  (reduce-kv #(assoc %1 %2 (extract-koodi-list %3 [:meta]))
             {}
             value))

(defn- valmistava-koulutus [value]
  (merge {:kuvaus                 (kuvaus (:kuvaus value))
          :opetusmuodos           (koodi-list (:opetusmuodos value))
          :opetusAikas            (koodi-list (:opetusAikas value))
          :opetusPaikkas          (koodi-list (:opetusPaikkas value))
          :suunniteltuKestoTyyppi (koodi (:suunniteltuKestoTyyppi value))}
         (select-keys value [:suunniteltuKestoArvo :hintaString :hinta :opintojenMaksullisuus :linkkiOpetussuunnitelmaan])))

(def map-field-to-converter {:version                      value
                             :modified                     value
                             :modifiedBy                   value
                             :nimi                         value
                             :aihees                       koodi-list
                             :koulutustyyppi               koodi
                             :oid                          value
                             :koulutuskoodi                koodi
                             :koulutusaste                 koodi
                             :koulutusala                  koodi
                             :opintoala                    koodi
                             :tutkinto                     koodi
                             :eqf                          koodi
                             :nqf                          koodi
                             :opintojenLaajuusyksikko      koodi
                             :koulutuksenLaajuusKoodi      koodi
                             :toteutustyyppi               value
                             :moduulityyppi                value
                             :komoOid                      value
                             :komotoOid                    value
                             :organisaatio                 value
                             :koulutusohjelma              koodi
                             :tunniste                     value
                             :tila                         value
                             :koulutusmoduuliTyyppi        value
                             :suunniteltuKestoArvo         value
                             :suunniteltuKestoTyyppi       koodi
                             :koulutuksenAlkamiskausi      koodi
                             :koulutuksenAlkamisvuosi      value
                             :koulutuksenAlkamisPvms       value
                             :opetuskielis                 koodi-list
                             :opetusmuodos                 koodi-list
                             :opetusAikas                  koodi-list
                             :opetusPaikkas                koodi-list
                             :opintojenLaajuusarvo         koodi
                             :opetusJarjestajat            value
                             :opetusTarjoajat              value
                             :ammattinimikkeet             koodi
                             :parents                      value
                             :children                     value
                             :opintojenMaksullisuus        value
                             :isAvoimenYliopistonKoulutus  value
                             :oppiaineet                   value
                             :extraParams                  value
                             :sisaltyyKoulutuksiin         value
                             :yhteyshenkilos               value
                             :pohjakoulutusvaatimukset     koodi
                             :tutkintonimikes              koodi-list
                             :opintojenRakenneKuvas        value
                             :koulutuksenTunnisteOid       value
                             :johtaaTutkintoon             value
                             :ohjelmas                     value
                             :hintaString                  value
                             :hinta                        value
                             :kuvausKomo                   kuvaus
                             :kuvausKomoto                 kuvaus
                             :sisaltyvatKoulutuskoodit     koodi-list
                             :kandidaatinKoulutuskoodi     koodi
                             :koulutuksenTavoitteet        value
                             :tutkintonimike               koodi
                             :koulutuslaji                 koodi
                             :opintojenLaajuusarvoKannassa value
                             :tarkenne                     value
                             :jarjestavaOrganisaatio       value
                             :pohjakoulutusvaatimus        koodi
                             :linkkiOpetussuunnitelmaan    value
                             :koulutusohjelmanNimiKannassa value
                             :opintojenLaajuusPistetta     value
                             :koulutusRyhmaOids            value
                             :opintojaksoOids              value
                             :lukiodiplomit                koodi-list
                             :kielivalikoima               kielivalikoima
                             :uniqueExternalId             value
                             :opinnonTyyppiUri             value
                             :hakijalleNaytettavaTunniste  value
                             :opettaja                     value
                             :opintokokonaisuusOid         value
                             :koulutuksenLoppumisPvm       value
                             :tarjoajanKoulutus            value
                             :opintopolkuAlkamiskausi      value
                             :valmistavaKoulutus           valmistava-koulutus})

;; Loops key-value map and transforms the value
(defn convert
  [dto]
  (into {} (for [[k v] dto] [k ((k map-field-to-converter) v)])))