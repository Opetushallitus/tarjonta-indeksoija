#!/bin/bash
cd "${0%/*}"

aws ecr get-login-password --region eu-west-1 --profile oph-utility | docker login --username AWS --password-stdin 190073735177.dkr.ecr.eu-west-1.amazonaws.com
docker tag elasticsearch-kouta 190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/elasticsearch-kouta:7.17.2
docker push 190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/elasticsearch-kouta:7.17.2
cd -