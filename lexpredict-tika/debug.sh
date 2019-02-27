#!/bin/bash
source setenv.sh

java -agentlib:jdwp=transport=dt_socket,server=y,address=8001,suspend=n -cp "./debug/tika-server-${TIKA_VERSION}.jar:./target/lexpredict-tika-${LEXPREDICT_TIKA_VERSION}.jar:libs/*" org.apache.tika.server.TikaServerCli --config ../tika-config.xml