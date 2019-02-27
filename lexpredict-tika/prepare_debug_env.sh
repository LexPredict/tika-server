#!/bin/bash

source setenv.sh
TIKA_SERVER_URL=https://www.apache.org/dist/tika/tika-server-$TIKA_VERSION.jar


mkdir -p ./debug
pushd debug

sudo apt-get install -y gpg curl gdal-bin openjdk-8-jre-headless

sudo apt-get -y install \
        tesseract-ocr \
        tesseract-ocr-eng tesseract-ocr-ita tesseract-ocr-fra tesseract-ocr-spa tesseract-ocr-deu tesseract-ocr-rus \
 && tesseract -v

curl -sSL https://people.apache.org/keys/group/tika.asc -o /tmp/tika.asc \
 && gpg --import /tmp/tika.asc \
 && curl -sSL "$TIKA_SERVER_URL.asc" -o /tmp/tika-server-${TIKA_VERSION}.jar.asc \
 && NEAREST_TIKA_SERVER_URL=$(curl -sSL http://www.apache.org/dyn/closer.cgi/${TIKA_SERVER_URL#https://www.apache.org/dist/}\?asjson\=1 \
 | awk '/"path_info": / { pi=$2; }; /"preferred":/ { pref=$2; }; END { print pref " " pi; };' \
 | sed -r -e 's/^"//; s/",$//; s/" "//') \
 && echo "Nearest mirror: $NEAREST_TIKA_SERVER_URL" \
 && wget "$NEAREST_TIKA_SERVER_URL" -O tika-server-${TIKA_VERSION}.jar




popd
