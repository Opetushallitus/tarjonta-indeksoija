#!/bin/bash
cd "${0%/*}"
docker build -t elasticsearch-kouta .
cd -