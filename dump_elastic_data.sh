#!/bin/sh

docker run --add-host=host.docker.internal:host-gateway --rm -v $1:/tmp elasticdump/elasticsearch-dump \
multielasticdump --input=$2 --output=/tmp --includeType=data,mapping,alias,settings,template