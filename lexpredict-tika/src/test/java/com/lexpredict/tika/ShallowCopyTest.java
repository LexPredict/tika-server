package com.lexpredict.tika;

import org.apache.tika.parser.pdf.PDFParserConfig;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class ShallowCopyTest {
    @Test
    public void testPdfAlphaImageReplacing() throws Exception {
        PDFParserConfig cfg = new PDFParserConfig();
        cfg.setExtractUniqueInlineImagesOnly(false);
        cfg.setOcrStrategy("OCR_ONLY");

        PDFParserConfig cpy = new PDFParserConfig();
        ShallowCopy.copyFields(cfg, cpy);
        assertTrue(cfg.getExtractUniqueInlineImagesOnly() ==
                cpy.getExtractUniqueInlineImagesOnly());
        assertTrue(cfg.getOcrStrategy() ==
                cpy.getOcrStrategy());
    }
}
