#!/bin/bash
cd "${0%/*}"

eval $(aws ecr get-login --region eu-west-1 --profile oph-utility --no-include-email)
docker tag elasticsearch-kouta 190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/elasticsearch-kouta:7.17.2
docker push 190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/elasticsearch-kouta:7.17.2
cd -