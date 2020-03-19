FROM ubuntu:18.04

ENV TIKA_VERSION 1.24
ENV TIKA_SERVER_URL https://www.apache.org/dist/tika/tika-server-$TIKA_VERSION.jar



RUN apt-get -y --fix-missing update

RUN apt-get install -y gpg curl gdal-bin openjdk-8-jre-headless

RUN \
    apt-get -y install \
        tesseract-ocr \
        tesseract-ocr-eng tesseract-ocr-ita tesseract-ocr-fra tesseract-ocr-spa tesseract-ocr-deu tesseract-ocr-rus \
 && tesseract -v

RUN \
    curl -sSL https://people.apache.org/keys/group/tika.asc -o /tmp/tika.asc \
 && gpg --import /tmp/tika.asc \
 && curl -sSL "$TIKA_SERVER_URL.asc" -o /tmp/tika-server-${TIKA_VERSION}.jar.asc \
 && NEAREST_TIKA_SERVER_URL=$(curl -sSL http://www.apache.org/dyn/closer.cgi/${TIKA_SERVER_URL#https://www.apache.org/dist/}\?asjson\=1 \
 	    | awk '/"path_info": / { pi=$2; }; /"preferred":/ { pref=$2; }; END { print pref " " pi; };' \
		| sed -r -e 's/^"//; s/",$//; s/" "//') \
 && echo "Nearest mirror: $NEAREST_TIKA_SERVER_URL" \
 && curl -sSL "$NEAREST_TIKA_SERVER_URL" -o /tika-server-${TIKA_VERSION}.jar


RUN apt-get -y clean autoclean \
    && apt-get -y autoremove \
    && rm -rf /var/lib/{apt,dpkg,cache,log}/

# default Tika config - may be overriden by Docker Swarm config mounting
COPY ./tika-config.xml /tika-config.xml
COPY ./lexpredict-tika/target/lexpredict-tika-1.0.jar /
RUN echo $(date) > /build.date

EXPOSE 9998
ENTRYPOINT  echo "Tika Server Docker Image built $(cat /build.date)" \
	    && echo "Java Version:" \
	    && java -version \
            && echo "Tesseract:" \
            && tesseract -v \
            && echo "Tika: ${TIKA_VERSION}" \
            && echo "Config:" \
            && cat /tika-config.xml \
            && java -cp "tika-server-${TIKA_VERSION}.jar:lexpredict-tika-1.0.jar:libs/*" org.apache.tika.server.TikaServerCli --h 0.0.0.0 --port 9998 --config /tika-config.xml
