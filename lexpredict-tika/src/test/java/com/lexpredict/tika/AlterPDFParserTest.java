package com.lexpredict.tika;

import org.apache.pdfbox.pdmodel.PDDocument;
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
    public void testScannedText() throws Exception {
        PDFParser pdfParser = new AlterPDFParser();
        ParseContext context = new ParseContext();
        PDFParserConfig config = new PDFParserConfig();
        context.set(PDFParserConfig.class, config);

        InputStream stream = AlterPDFParserTest.class.getResourceAsStream("/test-documents/scanned.pdf");
        String text = getText(stream, pdfParser, context);
        stream.close();

        assertTrue(text.length() > 100);
    }

    @Test
    public void testPdfTypeChecker() throws Exception {
        InputStream stream = AlterPDFParserTest.class.getResourceAsStream("/test-documents/scanned.pdf");
        PdfContentTypeChecker checker = new PdfContentTypeChecker();
        PdfContentTypeChecker.PdfContent docType = checker.determineDocContentType(stream);
        assertEquals(PdfContentTypeChecker.PdfContent.IMAGES, docType);
    }

    @Test
    public void testParseSimpleScannedText() throws Exception {
        PDFParser pdfParser = new AlterPDFParser();
        ParseContext context = new ParseContext();
        PDFParserConfig config = new PDFParserConfig();
        context.set(PDFParserConfig.class, config);

        InputStream stream = AlterPDFParserTest.class.getResourceAsStream("/test-documents/text_on_white.pdf");
        String text = getText(stream, pdfParser, context);
        stream.close();

        assertTrue(text.length() > 100);
    }

    @Test
    public void testParseTransparentScannedText() throws Exception {
        PDFParser pdfParser = new AlterPDFParser();
        ParseContext context = new ParseContext();
        PDFParserConfig config = new PDFParserConfig();
        context.set(PDFParserConfig.class, config);

        InputStream stream = AlterPDFParserTest.class.getResourceAsStream("/test-documents/large_transparent_doc.pdf");
        String text = getText(stream, pdfParser, context);
        stream.close();

        assertTrue(text.length() > 50);
    }

    @Test
    public void testPdfNonAlphaImageReplacing() throws Exception {
        InputStream stream = AlterPDFParserTest.class.getResourceAsStream("/test-documents/scanned.pdf");
        PDDocument doc = PDDocument.load(stream);

        PdfContentImagePreprocessor preproc = new PdfContentImagePreprocessor();
        boolean hasReplaced = preproc.removeImagesAlphaChannel(doc);
        assertFalse(hasReplaced);
    }

    @Test
    public void testPdfAlphaImageReplacing() throws Exception {
        InputStream stream = AlterPDFParserTest.class.getResourceAsStream("/test-documents/transp_scanned.pdf");
        PDDocument doc = PDDocument.load(stream);

        PdfContentImagePreprocessor preproc = new PdfContentImagePreprocessor();
        boolean hasReplaced = preproc.removeImagesAlphaChannel(doc);
        assertTrue(hasReplaced);
    }
}
