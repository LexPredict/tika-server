- 1 - how to build

Just run "mvn install" or "mvn install -DskipTests" command in the project directory (lexpredict-tika). Output file lexpredict-tika-<version>.jar would be in lexpredict-tika/target/ folder.


- 2 - how to use

The resulted artifact (JAR) is a part of docker container. There, in docker, the jar-file is used as a parameter while starting Tika server like this:

java -cp "tika-server-${TIKA_VERSION}.jar:lexpredict-tika-<version>.jar:libs/*" org.apache.tika.server.TikaServerCli --config tika.config


- 3 - how to test locally:

Suppose we have a following folders and files:
  documents/
      source_doc.pdf
  parsed/
  scripts/
      tika-server-1.20.jar
      lexpredict-tika-1.0.jar
      tika.config
first thing to do is to


- 3.1 - run Tika server

Currently we are in "parsed" directory. Our tika server has version 1.20, our plugin has version 1.0.
Run the following command:
java -cp 'tika-server-1.20.jar:lexpredict-tika-1.0.jar:libs/*' org.apache.tika.server.TikaServerCli --port 9999 --config tika.config

We should see a number of lines in output, like:
INFO  Starting Apache Tika 1.20 server
...
INFO  Using custom config: tika.config
...
Started Apache Tika server at http://localhost:9999/


- 3.2 - parse a document

Run command:
curl -T documents/source_doc.pdf http://localhost:9999 -H pdf-parse:pdf_ocr > parsed/parsed_doc.zip
Note that -H pdf-parse:pdf_ocr parameter
This parameter comes from the plugin. It could have one of the three values
1) "pdf_ocr" means that plugin will decide what internal parser to use, PDF-2-TEXT or OCR,
2) "strip" means the same, but the "printed" text will be obtained by PDFBox PDFTextStripper class
3) "default" means that the plugin will work as a standard PDFParser plugin


- 4.2 - source files

1) Directory lexpredict-tika/src/main/java/com/lexpredict/tika, files:
1.1) AlterPDFParser.java
here is the plugin itself. A class derived from standard PDFParser.

1.2) HttpRequestParamsReader.java
a class that captures HTTP context for the command passed to the Tika server. Searches for "pdf-parse" request parameter.

1.3) PdfContentImagePreprocessor.java
this class "removes" alpha channel from all embedded in PDDocument images by drawing them on a solid color background. Thus preventing issue with parsing transparent images.

1.4) PdfContentTypeChecker.java
this class determines the content of the PDDocument passed. The content is either "EMPTY, TEXT", "IMAGES" or "MIXED" (text + images). When the content is "IMAGES" and "pdf-parse" is set to "pdf_ocr" the parser uses OCR document processing.

