# tika-server

Configurable Apache Tika Server Docker Image with Tesseract 4.

Contains additional PDF parser improvements to workaround problem with obsolete empty lines in PDF files caused by corrupted embedded fonts.

## Contents
- Apache Tika 1.20
- Tesseract OCR 4
- Tesseract Language Packs: English, Italian, French, Spain, German, Russian

Allows providing external configuration file for Tika Server - for disabling OCR or any other needs.

## Building

```
cd build
./build.sh script.
```

## Running

**Pulling lexnlp/tika-server:**
```
docker pull lexpredict/tika-server
```



**Simply running Tika Server with default config and publishing Tika port on the host machine:**
```
docker run -p 9998:9998 -it lexpredict/tika-server
``` 

**Running Tika Server with external configuration:**
1. Create tika-config.xml file.
The following example tika-config.xml can be used for disabling OCR:
```
<?xml version="1.0" encoding="UTF-8"?>
<properties>
  <parsers>
      <parser class="org.apache.tika.parser.DefaultParser">
          <parser-exclude class="org.apache.tika.parser.ocr.TesseractOCRParser"/>
      </parser>
  </parsers>
</properties>
```
2. Run Tika server with this config file:
```
docker run -it -p 9998:9998 -v /home/user/tika-config.xml:/tika-config.xml lexpredict/tika-server
```
If running via sudo ensure you provide full path to the file on the host machine – otherwise it will throw an error.


**Running Tika Server cluster in Docker Swarm:**
1. Assuming you already have a Docker Swarm cluster configured (docker swarm init) and some worker machines are connected to it.
2. To deploy Tika we need docker-compose.yml file (see /deployment-example dir):
```
version: "3.3"
services:
  tika:
    image: lexpredict/tika-server:latest
    ports:
      - 9998:9998
    configs:
      - source: tika_config_3
        target: /tika-config.xml
    networks:
      - net
    deploy:
      replicas: 3

networks:
  net:

configs:
  tika_config_3:
    file: ./tika-config.xml

```
Configuration file (tika-config.xml) should be in the same directory with docker-compose.xml.
3. Deploying Tika to Docker Swarm: 
```
docker stack deploy --compose-file docker-compose.yml tika-cluster
```

## Workaround for fixing obsolete empty lines in PDF documents having corrupted embedded fonts

In some cases the current PDF text extraction routines from TIKA work incorrectly with PDF documents containing corrupted embedded fonts. The extracted text contains an obsolete blank line after almost every line of normal text.

It can be fixed by using PDFTextStripper class from PDFBox which probably was used in previous versions of TIKA.
This workaround is not suitable for all cases because it provides worse results than TIKA's normal text extraction on good uncorrupted PDF documents.

Normaly TIKA configured in this Docker image processes PDFs as usual without using the old-style PDFTextStripper.
To trigger processing the document with PDFTextStripper add a header to the request: "pdf-parse:strip".
