# tarjonta-indeksoija-service

## Usage

### Run the application locally

Application requires a local Elasticsearch index listening on localhost:9200

Running the application or tests from the commandline work with the aliases provided in
project.clj. 

To run the application: `lein run`

Running the app itself from the repl doesn't seem worth while.


### Run the tests

The suggested running for tests is from the repl though. Create a run configuration for the leiningen 
project and set it to run with nREPL in Leiningen and add Before launch: synchronize Leiningen projects.

To run tests from command line use commands `lein test` or `lein autotest`

### Packaging and running as standalone jar

To create a runable jar file, put a config.edn file to resources folder and run

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