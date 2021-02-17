# kouta-indeksoija

## 1. Palvelun tehtävä

Indeksoi Elasticsearchiin koulutustarjonnan dataa, jotta sitä voidaan jakaa tehokkaasti eri palveluille, erityisesti 
konfo-ui:lle (beta.opintopolku.fi) ja sen koulutushaulle. 

## 2. Arkkitehtuuri

Kouta-indeksoija on clojurella toteutettu ring/compojure/compojure-api web API. Indeksoija kuuntelee kouta-backendin
lähettämiä sqs-viestejä ja viestin saatuaan hakee viestissä olleella id:llä entiteetin tiedot kouta-backendin 
rajapinnasta ja indeksoi tiedot Elasticsearchiin. Pyörittää myös cron jobia joka tarkastaa kouta-backendin muuttuneet
tiedot siinä tapauksessa että jokin tieto on jäänyt indeksoimatta.

Kouta-indeksoijaan integroituvia oph:n palveluja on mm. konfo-backend, kouta-internal, kouta-external. 

Indeksoijan käyttämät tietolähteet:

| Tietolähde    | Haettavat tiedot                                                                                                            |
|---------------|-----------------------------------------------------------------------------------------------------------------------------|
| eperusteet    | Ammatillisten koulutusten eperusteet sekä osaamisalojen ja tutkinnon osien kuvaukset                                        |
| koodisto      | Konfo-ui:ssa tarvittavien koodistojen koodien lokalisaatiot. Lisäksi tarvittavat koodirelaatiot ja niidenkin lokalisaatiot. |
| kouta-backend | Koulutukset, koulutusten toteutukset, hakukohteet, haut, valintaperustekuvaukset ja SORA-kuvaukset                          |
| lokalisaatio  | Konfo-ui:ssa käyttäjille näytettävät tekstit kolmella eri kielellä (suomi, ruotsi ja englanti)                              |
| organisaatio  | Koulutustoimijoiden, oppilaitosten ja toimipisteiden perustiedot ja niiden organisaatiohierarkia                            |

## 3. Kehitysympäristö

### 3.1. Esivaatimukset

Asenna haluamallasi tavalla koneellesi
1. [Clojure](https://clojure.org/guides/getting_started)
2. [Docker](https://www.docker.com/get-started) (Elasticsearchia ja localstackia varten)
3. [AWS cli](https://aws.amazon.com/cli/) (SQS-jonoja varten)
4. [Leiningen](https://leiningen.org/) (Valitse asennus haluammallasi package managerilla)

Lisäksi tarvitset Java SDK:n (Unix pohjaisissa käyttöjärjestelmissä auttaa esim. [SDKMAN!](https://sdkman.io/)). 
Katso [.travis.yml](.travis.yml) mitä versioita sovellus käyttää. Kirjoitushetkellä käytössä openJDK11.

Indeksoijan saa konfiguroitua luomalla tiedoston `dev_resources/config.edn` (laitettu .gitignoreen) ja asettamalla sinne
tarvittavat arvot. Tiedostosta `dev_resources/config.edn.template` näkee mitä arvoja sovellus tarvitsee toimiakseen.
Kirjoitushetken esimerkki konfigista, joka toimii untuva-testiympäristöä vasten lokaalilla Elasticsearchilla:

```clojure
{:elastic-url "http://localhost:9200"
 :cas {:username "kouta"
 :password "tähän cas:n salasana"}
 :hosts {:kouta-backend "https://virkailija.untuvaopintopolku.fi"
 :kouta-external "https://virkailija.untuvaopintopolku.fi"
 :virkailija-internal "https://virkailija.untuvaopintopolku.fi"
 :cas "https://virkailija.untuvaopintopolku.fi"
 :ataru-hakija ""}
 :queue {:priority {:name "koutaIndeksoijaPriority" :health-threshold 10}
 :fast {:name "koutaIndeksoijaFast" :health-threshold 10}
 :slow {:name "koutaIndeksoijaSlow" :health-threshold 10}
 :dlq {:name "koutaIndeksoijaDlq" :health-threshold 10}
 :notifications {:name "koutaIndeksoijaNotifications" :health-threshold 10}
 :notifications-dlq {:name "koutaIndeksoijaNotificationsDlq" :health-threshold 10}}
 :sqs-region ""
 :sqs-endpoint "http://localhost:4576"
 :dlq-cron-string "*/15 * * ? * *"
 :notification-dlq-cron-string "7/30 * * ? * *"
 :lokalisaatio-indexing-cron-string "* 0/30 * ? * *"
 :queueing-cron-string "*/15 * * ? * *"
 :notifier-targets ""}
```

Cas-salasanan saa kaivettua untuvan ympäristökohtaisesta reposta opintopolku.yml tiedostosta kohdasta `kouta_indeksoija_cas_password`

Jos et tiedä mitä tämä tarkoittaa, kysy neuvoa kehitystiimiltä tai OPH:n ylläpidolta. 

### 3.2. Testien ajaminen

Testit saa ajettua komentoriviltä komennolla `lein test`

Yksittäisen testitiedoston saa ajettua `lein test <namespacen nimi>`. 
Esimerkiksi `lein test kouta-indeksoija-service.indexer.kouta-koulutus-test`

Yksittäisen testin saa ajettua `lein test :only <namespacen nimi>/<testin nimi>`. 
Esimerkiksi `lein test :only kouta-indeksoija-service.indexer.kouta-koulutus-test/index-julkaistu-koulutus-test`

Testit käynnistävät Elasticsearchin docker-kontissa satunnaiseen vapaaseen porttiin.

### 3.3. Ajaminen lokaalisti

Ennen indeksoijan ajamista lokaalisti täytyy pyörimässä olla
1. Elasticsearch
2. localstackin sqs-jonot

---
#### Elasticsearch-kontti

Elasticsearchia kannattaa pyörittää docker-kontissa siten että data tallennetaan levylle vaikka kontin 
sammuttaisi, koska Elasticsearch on "I/O sensitive and the Docker storage driver is not ideal for fast I/O".
Tämä onnnistuu ajamalla ensin (ainoastataan ensimmäisellä kerralla):

```shell
docker volume create kouta-elastic-data
```
Jonka jälkeen kontin saa käyntiin komennolla:
```shell
docker run --rm --name kouta-elastic --env "discovery.type=single-node" -p 127.0.0.1:9200:9200 -p 127.0.0.1:9300:9300 -v kouta-elastic-data:/usr/share/elasticsearch/data docker.elastic.co/elasticsearch/elasticsearch:6.8.13
```
Jos tulee tarve poistaa data, komennolla `docker volume --help` saa apua volumeiden hallinnointiin.

---
#### Localstack SQS-jonot
Indeksoija vaatii lokaalin SQS-palvelun porttiin 4567. Sen voi käynnistää docker-konttiin ajamalla skriptin 
`tools\start_localstack` ja pysäyttää skriptillä `tools\stop_localstack`. 

`tools\send_local` skriptiä voi käyttää viestien lähetykseen lokaaleihin jonoihin, jos jostain syystä
sellainen tarve tulee.

---

Kun Elasticsearch ja sqs-jonot ovat pyörimässä indeksoijan saa käyntiin komennolla `lein run`

Tämä avaa swaggerin selaimeen osoitteeseen `http://localhost:3000/kouta-indeksoija/swagger/index.html`

### 3.4. Kehitystyökalut

Suositeltava kehitysympäristö on [IntelliJ IDEA](https://www.jetbrains.com/idea/) + [Cursive plugin](https://cursive-ide.com/)
mutta sovelluksen ja testien ajamisen kannalta nämä eivät ole välttämättömiä. Jos et tee paljon clojure-kehitystä, myöskään cursive ei
ole välttämätön.

### 3.5. Testidata

Testidataa saa omaan lokaaliin Elasticsearchiin laittamalla tiedoston `dev_resources/config.edn` arvot

```clojure
:hosts {:kouta-backend "https://virkailija.untuvaopintopolku.fi"
:kouta-external "https://virkailija.untuvaopintopolku.fi"
:virkailija-internal "https://virkailija.untuvaopintopolku.fi"
:cas "https://virkailija.untuvaopintopolku.fi"
```
osoittamaan haluamaasi testiympäristöä vasten ja ajamalla swaggerista `POST /kouta-indeksoija/api/kouta/all`.
Voit myös indeksoida vain yksittäisen entiteetin, esimerkiksi ajamalla swaggerista 
`POST /kouta-indeksoija/api/kouta/koulutus/:oid` 

## 4. Ympäristöt

### 4.1. Testiympäristöt

Testiympäristöjen swaggerit löytyvät seuraavista osoitteista:

- [untuva](https://virkailija.untuvaopintopolku.fi/kouta-indeksoija/swagger)
- [hahtuva](https://virkailija.hahtuvaopintopolku.fi/kouta-indeksoija/swagger)
- [QA eli pallero](https://virkailija.testiopintopolku.fi/kouta-indeksoija/swagger)

Jotta pääset näihin käsiksi täytyy sinulla olla OPH:n ympäristöihin päästävä VPN:n päällä ja indeksoijan
tunnus ja salasana tiedossa. Näihin apua saa kehitystiimiltä tai OPH:n ylläpidolta.

### 4.2. Asennus

Asennus hoituu samoilla työkaluilla kuin muidenkin OPH:n palvelujen. 
[Cloud-basen dokumentaatiosta](https://github.com/Opetushallitus/cloud-base/tree/master/docs) ja ylläpidolta löytyy apuja.

Asennuksen yhteydessä tapahtuvaan mahdolliseen uudelleenindeksointiin löytyy ohjeet 
täältä: [Katkoton indeksointi](README_INDEKSOINTI.md)

### 4.3. Lokit

Indeksoijan lokit löytyvät AWS:n cloudwatchista. Ohjeita näihin ylläpidolta.

### 4.4. Continuous integration

https://travis-ci.com/github/Opetushallitus/kouta-indeksoija

## 5. Lisätietoa

Vanhan readme:n tiedot joiden paikkansapitävyyttä ei ole selvitetty:

---
---
---
#### Repl for development

During development it's easy to test what you have done by using a REPL. This works best
with IDEA and cursive (others also, but the instructions here apply for Cursive).

Create a REPL run configuration. When you import the project one should be created for you.
If this is not the case, right click project.clj and select run. Then edit the configuration:

* Name it something like Dev REPL
* Add a JVM argument `-Dtest=false`
* When you start the repl type `(go)` to activate mount.core (brings settings to namespaces).

#### Repl for autotesting

Create another repl configuration, e.g. Test repl and add JVM argument `-Dtest=true`.

To run tests every time code is changed:
* `(use 'midje.repl)` (no need to use `(go)` here)
* `(autotest)`

### Running the application locally

#### Requirements

##### Notifier
The application can notify others when information is indexed. This is controlled with `:notifier-targets`
value in `dev_resources/config.edn`. It should be defaulted to `""`, ie. no changes will be sent.

When wanting to validate locally that the notifications are working, one of the easiest way is with `dummy-web-server.py` in `tools`:
* Run `python2 tools/dummy-web-server.py 9900` to start it in port 9900.
* Change `:notifier-targets` in `dev_resources/config.edn` to `"http://localhost:9900"`
* You can edit the script to set the return code and headers.

The script will log every access to console.


### Nuking ElasticSearch settings

If you need to nuke ElasticSearch analyze settings, there are two tools that can be used test different settings and see how
they affect queries and word analysis.

`test/misc/analyze_settings_test_tool.clj` can be used if you want to test how different analyze settings affect queries.
It mimics both nested structure in kouta-indeksoija search indexes and search queries generated by konfo-backend for koulutus
and oppilaitos searches.

`test/misc/analyzers_test_tool.clj` can be used to test how different search analyze settings handle different words and phraises.

Both tools have main method and they can be run for example in Idea by choosing run command when clicking right mouse button.
Make sure you have test profile enabled in `Leiningen Projects/Profiles`.

### Configuration

App config is handled with Mount, cprops and in (mostly dev) cases with environment variables. To add a conf parameter
to the application, add it to dev_resources/config.edn (for development) AND oph-configuration/config.edn.template. In
order for the template file to work in non-local environments, the variable must also be added to the variable file in
git@git.oph.ware.fi:environment-{ophitest|ophp|ophprod|vagrant}.git in deploy/<env>_vars.yml.

NOTE: The cron-string variable roughly follows cron scheduler syntax with a few alterations shown
[here](http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html).

---
---
---

## License

Copyright (c) 2017 The Finnish National Board of Education - Opetushallitus

For details see LICENSE.txt
