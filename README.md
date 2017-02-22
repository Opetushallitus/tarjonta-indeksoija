# tarjonta-indeksoija-service

## Usage

### Pre configuration

To run the application locally, copy dev_resources/config.edn.template to dev_resources/config.edn 
and replace all `<fillme>` fields with actual values (get from QA common.properties). This config file is ignored in 
Git and should not be committed in the future.

### REPL

The dev and test profiles configurations are separated for the repl to ensure that
tests use different indices than development in elasticsearch. This might seem trivial,
but when hacking manually you don't want the test cases to nuke your data when autotesting.

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

### Command line

#### Run the application locally

Application requires a local Elasticsearch index listening on localhost:9200

Running the application or tests from the commandline work with the aliases provided in
project.clj. 

To run the application: `lein run`

Running the app itself from the repl doesn't seem worth while.

#### Run the tests

To run tests from command line use commands `lein test` or `lein autotest`

### Configuration

App config is handled with Mount, cprops and in (mostly dev) cases with environment variables. To add a conf parameter
to the application, add it to dev_resources/config.edn (for development) AND oph-configuration/config.edn.template. In
order for the template file to work in non-local environments, the variable must also be added to the variable file in
git@git.oph.ware.fi:environment-{ophitest|ophp|ophprod|vagrant}.git in deploy/<env>_vars.yml.

NOTE: The cron-string variable roughly follows cron scheduler syntax with a few alterations shown 
[here](http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html).

### Packaging and running as standalone jar

To create a runnable jar file, put a config.edn file to resources folder and run. 
This is just for testing the jar, DO NOT DO THIS IN BAMBOO!

```
./ci/build.sh uberjar
java -jar target/server.jar
```

## License

Copyright (c) 2017 The Finnish National Board of Education - Opetushallitus

For details see LICENSE.txt