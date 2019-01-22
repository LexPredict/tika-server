package com.lexpredict.tika;

import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertTrue;

public class AlterPDFParserTest extends TikaTest {
    @Test
    public void testDoubleSpacedText() throws Exception {
        String packageName = AlterPDFParser.class.getPackage().getName();

        PDFParser pdfParser = new AlterPDFParser();
        ParseContext context = new ParseContext();
        PDFParserConfig config = new PDFParserConfig();
        context.set(PDFParserConfig.class, config);

        InputStream stream = AlterPDFParserTest.class.getResourceAsStream("/test-documents/double_space_test.pdf");
        String text = getText(stream, pdfParser, context);
        stream.close();

        assertTrue(text.length() > 100);
    }
}
