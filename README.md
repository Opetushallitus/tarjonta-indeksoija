# tarjonta-indeksoija-service

## Usage

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

#### Repl for autotesting

Create another repl configuration, e.g. Test repl and add JVM argument `-Dtest=true`.

To run tests every time code is changed:
* `(use 'midje.repl)`
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

### Packaging and running as standalone jar

To create a runnable jar file, put a config.edn file to resources folder and run. 
This is just for testing the jar, DO NOT DO THIS IN BAMBOO!

```
lein create-uberjar
java -jar target/server.jar
```

### Packaging as war

***TODO***

`lein ring uberwar`

## License

Copyright (c) 2017 The Finnish National Board of Education - Opetushallitus

For details see LICENSE.txt