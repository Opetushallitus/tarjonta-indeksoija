#!/bin/bash
cd "${0%/*}"

# No need to do ECR login in CI, ci-tools takes care of that
if [[ -z "$CI" ]]
then
    aws ecr get-login-password --region eu-west-1 --profile oph-utility | docker login --username AWS --password-stdin 190073735177.dkr.ecr.eu-west-1.amazonaws.com
fi
docker push 190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/elasticsearch-kouta:7.17.3
cd -