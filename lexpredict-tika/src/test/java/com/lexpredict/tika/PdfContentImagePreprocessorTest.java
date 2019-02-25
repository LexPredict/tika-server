package com.lexpredict.tika;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Test;
import java.io.InputStream;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PdfContentImagePreprocessorTest extends TikaTest {
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
