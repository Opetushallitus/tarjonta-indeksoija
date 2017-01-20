#!/bin/sh

export JAVA_HOME="${bamboo_capability_system_jdk_JDK_1_8}"
export PATH=$JAVA_HOME/bin:$PATH

test() {
  ./bin/lein clean
  ./bin/lein compile
  ./bin/lein ci-test
}

uberjar() {
  ./bin/lein clean
  mkdir ./resources
  echo ${bamboo_buildResultKey} > ./resources/build.txt
  git rev-parse HEAD > ./resources/git-rev.txt
  ./bin/lein create-uberjar
}

command="$1"

case "$command" in
    "test" )
        test
        ;;
    "uberjar" )
        uberjar
        ;;
    *)
        echo "Unknown command $command"
        ;;
esac