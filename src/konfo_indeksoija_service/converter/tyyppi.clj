(ns konfo-indeksoija-service.converter.tyyppi)

(defn remove-uri-version [uri]
  (if (not (nil? uri)) (first (clojure.string/split uri #"#"))))

(defn oppilaitostyyppi-uri-to-tyyppi [oppilaitostyyppi-uri]
  (case (remove-uri-version oppilaitostyyppi-uri)
    "oppilaitostyyppi_01" "muu" ;;Taiteen perusopetuksen oppilaitokset (ei musiikki)
    "oppilaitostyyppi_11" "muu" ;;Peruskoulut
    "oppilaitostyyppi_12" "muu" ;;Peruskouluasteen erityiskoulut
    "oppilaitostyyppi_15" "lk"  ;;Lukiot
    "oppilaitostyyppi_19" "lk"  ;;Perus- ja lukioasteen koulut
    "oppilaitostyyppi_21" "amm" ;;Ammatilliset oppilaitokset
    "oppilaitostyyppi_22" "amm" ;;Ammatilliset erityisoppilaitokset
    "oppilaitostyyppi_23" "amm" ;;Ammatilliset erikoisoppilaitokset
    "oppilaitostyyppi_24" "amm" ;;Ammatilliset aikuiskoulutuskeskukset
    "oppilaitostyyppi_28" "amm" ;;Palo-, poliisi- ja vartiointialojen oppilaitokset
    "oppilaitostyyppi_29" "amm" ;;Sotilasalan ammatilliset oppilaitokset
    "oppilaitostyyppi_41" "kk"  ;;Ammattikorkeakoulut
    "oppilaitostyyppi_42" "kk"  ;;Yliopistot
    "oppilaitostyyppi_43" "kk"  ;;Sotilaskorkeakoulut
    "oppilaitostyyppi_45" "kk"  ;;Lastentarhanopettajaopistot
    "oppilaitostyyppi_46" "kk"  ;;Väliaikaiset ammattikorkeakoulut
    "oppilaitostyyppi_61" "muu" ;;Musiikkioppilaitokset
    "oppilaitostyyppi_62" "muu" ;;Liikunnan koulutuskeskukset
    "oppilaitostyyppi_63" "muu" ;;Kansanopistot
    "oppilaitostyyppi_64" "muu" ;;Kansalaisopistot
    "oppilaitostyyppi_65" "muu" ;;Opintokeskukset
    "oppilaitostyyppi_66" "muu" ;;Kesäyliopistot
    "oppilaitostyyppi_91" "muu" ;;Kirjeoppilaitokset
    "oppilaitostyyppi_92" "muu" ;;Neuvontajärjestöt
    "oppilaitostyyppi_93" "muu" ;;Muut koulutuksen järjestäjät
    "oppilaitostyyppi_99" "muu" ;;Muut oppilaitokset
    "oppilaitostyyppi_XX" "muu" ;;Ei tiedossa (oppilaitostyyppi)
    "muu" ))

(defn koulutustyyppi-uri-to-tyyppi [koulutustyyppi-uri]
  (case (remove-uri-version koulutustyyppi-uri)
    "koulutustyyppi_1" "amm"  ;; Ammatillinen perustutkinto
    "koulutustyyppi_2" "lk"   ;; Lukiokoulutus
    "koulutustyyppi_3" "kk"   ;; Korkeakoulutus
    "koulutustyyppi_4" "amm"  ;; Ammatillinen perustutkinto erityisopetuksena
    "koulutustyyppi_5" "amm"  ;; Työhön ja itsenäiseen elämään valmentava koulutus (TELMA), ei johda tutkintoon
    "koulutustyyppi_6" "muu"  ;; Perusopetuksen lisäopetus, ei johda tutkintoon
    "koulutustyyppi_7" "amm"  ;; Ammatilliseen peruskoulutukseen ohjaava ja valmistava koulutus
    "koulutustyyppi_8" "amm"  ;; Maahanmuuttajien ammatilliseen peruskoulutukseen valmistava koulutus
    "koulutustyyppi_9" "lk"   ;; Maahanmuuttajien ja vieraskielisten lukiokoulutukseen valmistava koulutus
    "koulutustyyppi_10" "muu" ;; Vapaan sivistystyön koulutus, ei johda tutkintoon
    "koulutustyyppi_11" "amm" ;; Ammattitutkinto
    "koulutustyyppi_12" "amm" ;; Erikoisammattitutkinto
    "koulutustyyppi_13" "amm" ;; Ammatillinen perustutkinto näyttötutkintona
    "koulutustyyppi_14" "lk"  ;; Aikuisten lukiokoulutus
    "koulutustyyppi_15" "muu" ;; Esiopetus
    "koulutustyyppi_16" "muu" ;; Perusopetus
    "koulutustyyppi_17" "muu" ;; Aikuisten perusopetus
    "koulutustyyppi_18" "amm" ;; Ammatilliseen peruskoulutukseen valmentava koulutus (VALMA), ei johda tutkintoon
    "koulutustyyppi_19" "amm" ;; Ammatilliseen peruskoulutukseen valmentava koulutus (VALMA) erityisopetuksena, ei johda tutkintoon
    "koulutustyyppi_20" "muu" ;; Varhaiskasvatus
    "koulutustyyppi_21" "muu" ;; EB, RP, ISH
    "koulutustyyppi_22" "muu" ;; Perusopetukseen valmistava opetus, ei johda tutkintoon
    "koulutustyyppi_23" "lk"  ;; Lukiokoulutukseen valmistava koulutus, ei johda tutkintoon
    "koulutustyyppi_24" "muu" ;; Pelastusalan koulutus
    "koulutustyyppi_26" "amm" ;; Ammatillinen perustutkinto (reformin mukainen)
    "muu"))