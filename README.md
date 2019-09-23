# kouta-indeksoija-service

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

### Tests

To run tests from command line use commands `lein test` or `lein autotest`

To run single test from command line use command `lein test kouta-indeksoija-service.elastic-client-test`.

Tests require that Docker is installed and Docker daemon is up and running. It
is possible to filter out tests that require Docker by adding filter, ie
`lein test :filter -docker` or `lein autotest :filter -docker`. These tests include
at least those that test SQS integration.


### Running the application locally

#### Requirements

##### Elasticsearch
Application requires a local Elasticsearch index listening on port 9200. On Mac you can
install Elasticsearch with `brew install elasticsearch` and run with `elasticsearch` from
console, it will by default run on correct port.

##### SQS
Application requires a local SQS on port 4576. SQS can be started with `tools\start_localstack`
and stopped with `tools\stop_localstack`, this requires that Docker is installed.

`tools\send_local` can be used to send messages to local queues.

##### Notifier
The application can notify others when information is indexed. This is controlled with `:notifier-targets`
value in `dev_resources/config.edn`. It should be defaulted to `""`, ie. no changes will be sent.

When wanting to validate locally that the notifications are working, one of the easiest way is with `dummy-web-server.py` in `tools`:
* Run `python2 tools/dummy-we-server.py 9900` to start it in port 9900.
* Change `:notifier-targets` in `dev_resources/config.edn` to `"http://localhost:9900"`
* You can edit the script to set the return code and headers.

The script will log every access to console.

#### Running

Running the application or tests from the commandline work with the aliases provided in
project.clj. 

To run the application: `lein run`

Ui can be found in: [http://localhost:3000/kouta-indeksoija/ui/index.html]

Running the app itself from the repl doesn't seem worth while.


### Configuration

App config is handled with Mount, cprops and in (mostly dev) cases with environment variables. To add a conf parameter
to the application, add it to dev_resources/config.edn (for development) AND oph-configuration/config.edn.template. In
order for the template file to work in non-local environments, the variable must also be added to the variable file in
git@git.oph.ware.fi:environment-{ophitest|ophp|ophprod|vagrant}.git in deploy/<env>_vars.yml.

NOTE: The cron-string variable roughly follows cron scheduler syntax with a few alterations shown 
[here](http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html).

## License

Copyright (c) 2017 The Finnish National Board of Education - Opetushallitus

For details see LICENSE.txt
