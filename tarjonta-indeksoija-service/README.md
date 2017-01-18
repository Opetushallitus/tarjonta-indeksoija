# tarjonta-indeksoija-service

## Usage

### Run the application locally

Running the application or tests from the commandline work with the aliases provided in
project.clj. 

To run the application: `lein run`

For running tests, use `lein test` or `lein autotest`, `lein midje` will fail due to not setup 
embedded elastic server.

Running the app itself from the repl doesn't seem worth while.

The suggested running for tests is from the repl though. Create a run configuration for the leiningen 
project and set it to run with nREPL in Leiningen, to use profile `+dev-conf` and add Before launh: 
syncronize Leiningen projects.

The dev-conf profile does not use an embedded elastic server, which might be better for hacking since its
data doesn't get destroyed when the process is stopped. If you want to run locally with and embedded
elastic server, set the :use-embedded-elastic to true in resources/dev/config.clj.

### Run the tests

`lein test` or `lein autotest`

### Packaging and running as standalone jar

***TODO***

```
lein do clean, ring uberjar
java -jar target/server.jar
```

### Packaging as war

***TODO***

`lein ring uberwar`

## License

Copyright ©  FIXME
