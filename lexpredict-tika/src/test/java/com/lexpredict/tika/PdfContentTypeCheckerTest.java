package com.lexpredict.tika;

import org.junit.Test;
import java.io.InputStream;
import static org.junit.Assert.assertEquals;

public class PdfContentTypeCheckerTest extends TikaTest {
    @Test
    public void testPdfTypeChecker() throws Exception {
        InputStream stream = AlterPDFParserTest.class.getResourceAsStream("/test-documents/scanned.pdf");
        PdfContentTypeChecker checker = new PdfContentTypeChecker();
        PdfContentTypeChecker.PdfContent docType = checker.determineDocContentType(stream);
        assertEquals(PdfContentTypeChecker.PdfContent.IMAGES, docType);
    }
}
