package com.lexpredict.tika;

import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.junit.Test;
import java.io.InputStream;
import static org.junit.Assert.*;

public class AlterPDFParserTest extends TikaTest {
    @Test
    public void testDoubleSpacedText() throws Exception {
        PDFParser pdfParser = new AlterPDFParser();
        ParseContext context = new ParseContext();
        PDFParserConfig config = new PDFParserConfig();
        context.set(PDFParserConfig.class, config);

        InputStream stream = AlterPDFParserTest.class.getResourceAsStream("/test-documents/double_space_test.pdf");
        String text = getText(stream, pdfParser, context);
        stream.close();

        assertTrue(text.length() > 100);
    }

    @Test
    public void testParseSimpleScannedText() throws Exception {
        String text = getTextFromDoc("/test-documents/text_on_white.pdf",
                AlterPDFParser.ParsePdfMode.PDF_OCR);
        assertTrue(text.length() > 50);
    }

    @Test
    public void testParseTransparentScannedText() throws Exception {
        String text = getTextFromDoc("/test-documents/transp_scanned.pdf",
                AlterPDFParser.ParsePdfMode.PDF_OCR);
        assertTrue(text.length() > 50);
    }

    private String getTextFromDoc(String docPath,
                                  AlterPDFParser.ParsePdfMode parseMode) throws Exception {
        AlterPDFParser pdfParser = new AlterPDFParser();
        pdfParser.defaultParseMode = parseMode;
        ParseContext context = new ParseContext();
        PDFParserConfig config = new PDFParserConfig();
        context.set(PDFParserConfig.class, config);

        InputStream stream = AlterPDFParserTest.class.getResourceAsStream(docPath);
        String text = getText(stream, pdfParser, context);
        stream.close();
        return text;
    }
}
