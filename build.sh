#!/bin/bash
set -e
pushd ./lexpredict-tika
mvn clean package
popd
sudo docker build --no-cache -t lexpredict/tika-server-probe ./
