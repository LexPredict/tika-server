package com.lexpredict.tika;

import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.junit.Test;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
        return getTextFromDoc(docPath, parseMode, "text");
    }

    private String getTextFromDoc(String docPath,
                                  AlterPDFParser.ParsePdfMode parseMode,
                                  String outputFormat) throws Exception {
        AlterPDFParser pdfParser = new AlterPDFParser();
        pdfParser.defaultParseMode = parseMode;
        ParseContext context = new ParseContext();
        PDFParserConfig config = new PDFParserConfig();
        context.set(PDFParserConfig.class, config);

        InputStream stream = AlterPDFParserTest.class.getResourceAsStream(docPath);
        if (outputFormat.equals("text")) {
            String txt = getText(stream, pdfParser, context);
            stream.close();
            return txt;
        }
        XMLResult rst = getXML(stream, pdfParser, context);
        stream.close();
        return rst.xml;
    }

    @Test
    public void testParseXhtmlNoDetail() throws Exception {
        String text = getTextFromDoc("/test-documents/sample_table.pdf",
                AlterPDFParser.ParsePdfMode.PDF_ONLY, "xml");
        assertTrue(text.length() > 50);
    }

    @Test
    public void testParseJBig() throws Exception {
        String oldSysEnv = setEnvVar("LEXNLP_TIKA_PARSER_MODE", "ocr_only");
        String text = getTextFromDoc("/test-documents/jbig.pdf",
                AlterPDFParser.ParsePdfMode.OCR_ONLY, "xml");
        setEnvVar("LEXNLP_TIKA_PARSER_MODE", oldSysEnv);
        assertTrue(text.length() > 50);
    }

    @Test
    public void testParseXhtmlCoordsEmbedded() throws Exception {
        String oldSysEnv = setEnvVar("LEXNLP_TIKA_XML_DETAIL", "coords_embedded");
        String text = getTextFromDoc("/test-documents/industrial developing authority.pdf",
                AlterPDFParser.ParsePdfMode.PDF_ONLY, "xml");
        setEnvVar("LEXNLP_TIKA_XML_DETAIL", oldSysEnv);
        assertTrue(text.length() > 50);
    }

    @Test
    public void testParseXhtmlCoordsFlat() throws Exception {
        String oldSysEnv = setEnvVar("LEXNLP_TIKA_XML_DETAIL", "coords_flat");
        String text = getTextFromDoc("/test-documents/industrial developing authority.pdf",
                AlterPDFParser.ParsePdfMode.PDF_ONLY, "xml");
        setEnvVar("LEXNLP_TIKA_XML_DETAIL", oldSysEnv);
        assertTrue(text.length() > 50);
    }

    @Test
    public void testParseXhtmlCsTextFlat() throws Exception {
        String oldSysEnv = setEnvVar("LEXNLP_TIKA_XML_DETAIL", "coords_text_flat");
        String text = getTextFromDoc("/test-documents/double_space_test.pdf",
                AlterPDFParser.ParsePdfMode.PDF_ONLY, "xml");
        setEnvVar("LEXNLP_TIKA_XML_DETAIL", oldSysEnv);
        assertTrue(text.length() > 50);
    }

    @Test
    public void testParseToBraces() throws Exception {
        String oldSysEnv = setEnvVar("LEXNLP_TIKA_XML_DETAIL", "coords_text_flat");
        String text = getTextFromDoc("/test-documents/chylde_harold.pdf",
                AlterPDFParser.ParsePdfMode.PDF_ONLY, "xml");
        setEnvVar("LEXNLP_TIKA_XML_DETAIL", oldSysEnv);
        assertTrue(text.length() > 50);
        assertTrue(text.indexOf("] ]") > 0);
    }

    @Test
    public void testParseNoDuplicates() throws Exception {
        String oldSysEnv = setEnvVar("LEXNLP_TIKA_PARSER_MODE", "pdf_prefer_text");
        String text = getTextFromDoc("/test-documents/mixed_scanned_text.pdf",
                AlterPDFParser.ParsePdfMode.PDF_ONLY, "xml");
        setEnvVar("LEXNLP_TIKA_PARSER_MODE", oldSysEnv);
        assertTrue(text.length() > 50);
    }

    protected static String setEnvVar(String varName, String varValue) throws Exception {
        String oldSysEnv = System.getenv(varName);
        oldSysEnv = oldSysEnv == null ? "" : oldSysEnv;
        setEnv(new HashMap<String, String>() {{
            put(varName, varValue);
        }});
        return oldSysEnv;
    }

    protected static void setEnv(Map<String, String> newenv) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for(Class cl : classes) {
                if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
        }
    }
}
