package com.lexpredict.tika;

import org.apache.tika.parser.pdf.PDFParserConfig;

public class AlterPDFParserConfig{
    public static void configureAlterPtf2Xhtml(PDFParserConfig config, PDF2XHTML pdf2XHTML) {
        pdf2XHTML.setSortByPosition(config.getSortByPosition());
        if (config.getEnableAutoSpace()) {
            pdf2XHTML.setWordSeparator(" ");
        } else {
            pdf2XHTML.setWordSeparator("");
        }
        if (config.getAverageCharTolerance() != null) {
            pdf2XHTML.setAverageCharTolerance(config.getAverageCharTolerance());
        }
        if (config.getSpacingTolerance() != null) {
            pdf2XHTML.setSpacingTolerance(config.getSpacingTolerance());
        }
        pdf2XHTML.setSuppressDuplicateOverlappingText(config.getSuppressDuplicateOverlappingText());
    }
}
