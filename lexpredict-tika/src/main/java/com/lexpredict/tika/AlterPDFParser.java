package com.lexpredict.tika;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.tika.exception.AccessPermissionException;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.PDF;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.parser.pdf.*;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class AlterPDFParser extends PDFParser {
    public enum ParsePdfMode {
        DEFAULT, PDF_OCR, TEXT_STRIP
    }

    // uses this value if it is not set in HttpRequest
    ParsePdfMode defaultParseMode = ParsePdfMode.PDF_OCR;

    // Metadata key for giving the document password to the parser
    private static final MediaType MEDIA_TYPE = MediaType.application("pdf");

    // Serial version UID
    private static final long serialVersionUID = -752276948656079347L;

    private PDFParserConfig defaultConfig = new PDFParserConfig();

    @Override
    public void parse(
            InputStream stream, ContentHandler handler,
            Metadata metadata, ParseContext context)
            throws IOException, SAXException, TikaException {

        HttpRequestParamsReader.getInstance().initialize(stream);
        HttpRequestParamsReader.getInstance().outIfVerbose("AlterPDFParser.parse()");
        ParsePdfMode pdfParseMode = getParseMode();

        PDFParserConfig sourceConfig = context.get(PDFParserConfig.class, defaultConfig);
        PDFParserConfig localConfig = makeConfigLocalCopy(sourceConfig);

        if (localConfig.getSetKCMS())
            System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");

        PDDocument pdfDocument = null;
        try {
            TikaInputStream tstream = TikaInputStream.cast(stream);
            String password = callGetPassword(metadata, context);
            MemoryUsageSetting memoryUsageSetting = MemoryUsageSetting.setupMainMemoryOnly();
            if (localConfig.getMaxMainMemoryBytes() >= 0) {
                memoryUsageSetting = MemoryUsageSetting.setupMixed(localConfig.getMaxMainMemoryBytes());
            }

            if (tstream != null && tstream.hasFile()) {
                // File based -- send file directly to PDFBox
                pdfDocument = PDDocument.load(tstream.getPath().toFile(), password, memoryUsageSetting);
            } else
                pdfDocument = PDDocument.load(new CloseShieldInputStream(stream), password, memoryUsageSetting);

            extractAndCheckMetadata(metadata, context, localConfig, pdfDocument);

            if (handler == null)
                return;

            // preprocess document
            //PdfContentImagePreprocessor preproc = new PdfContentImagePreprocessor();
            //preproc.removeImagesAlphaChannel(pdfDocument);

            if (callShouldHandleXFAOnly(pdfDocument, localConfig)) {
                HttpRequestParamsReader.getInstance().outIfVerbose("AlterPDFParser.parse(callShouldHandleXFAOnly)");
                callHandleXFAOnly(pdfDocument, handler, metadata, context);
            }
            else if (localConfig.getOcrStrategy().equals(PDFParserConfig.OCR_STRATEGY.OCR_ONLY)) {
                HttpRequestParamsReader.getInstance().outIfVerbose("AlterPDFParser.parse(OCR_ONLY)");
                metadata.add("X-Parsed-By", TesseractOCRParser.class.toString());
                callOCR2XHTMLProcess(pdfDocument, handler, context, metadata, localConfig);
            }
            else {
                // parse document by using PDFStripper
                if (pdfParseMode == ParsePdfMode.TEXT_STRIP)
                    PdfStripperProcessor.setTextUsingPDFTextStripper(handler, pdfDocument);
                // smart parsing: PDF or OCR
                else if (pdfParseMode == ParsePdfMode.PDF_OCR) {
                    HttpRequestParamsReader.getInstance().outIfVerbose("AlterPDFParser.parse(PDF_OCR)");
                    PdfContentTypeChecker checker = new PdfContentTypeChecker();
                    PdfContentTypeChecker.PdfContent docType = checker.determineDocContentType(pdfDocument);
                    if (docType != PdfContentTypeChecker.PdfContent.IMAGES)
                        callPDF2XHTMLProcess(pdfDocument, handler, context, metadata, localConfig);
                    else {
                        metadata.add("X-Parsed-By", TesseractOCRParser.class.toString());
                        callOCR2XHTMLProcess(pdfDocument, handler, context, metadata, localConfig);
                    }
                }
                else { // ... or parse it default Tika-way
                    HttpRequestParamsReader.getInstance().outIfVerbose("AlterPDFParser.parse(callPDF2XHTMLProcess)");
                    callPDF2XHTMLProcess(pdfDocument, handler, context, metadata, localConfig);
                }
            }

        } catch (InvalidPasswordException e) {
            metadata.set(PDF.IS_ENCRYPTED, "true");
            throw new EncryptedDocumentException(e);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                NoSuchFieldException | ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } // see e.getCause()
        finally {
            if (pdfDocument != null) {
                pdfDocument.close();
            }
        }
    }

    // method determines what parsing strategy to use
    // from HTTPRequest or the default variable value
    private ParsePdfMode getParseMode() {
        String parseMode = HttpRequestParamsReader.getInstance().typedParams.get(CommonParseFlag.PDF_PARSE_METHOD);
        if (parseMode == null || parseMode.length() == 0)
            parseMode = System.getenv("LEXNLP_TIKA_PARSER_MODE");
        if (parseMode == null || parseMode.length() == 0)
            return defaultParseMode;
        
        if (parseMode.equals(HttpRequestParamsReader.PDF_PARSE_METHOD_STRIP))
            return ParsePdfMode.TEXT_STRIP;
        if (parseMode.equals(HttpRequestParamsReader.PDF_PARSE_METHOD_PDF_OCR))
            return ParsePdfMode.PDF_OCR;

        return defaultParseMode;
    }

    // extract doc's metadata and check whether it is accessible
    private void extractAndCheckMetadata(Metadata metadata, ParseContext context, PDFParserConfig localConfig, PDDocument pdfDocument)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, AccessPermissionException {
        metadata.set(PDF.IS_ENCRYPTED, Boolean.toString(pdfDocument.isEncrypted()));
        metadata.set(Metadata.CONTENT_TYPE, MEDIA_TYPE.toString());
        callExtractMetadata(pdfDocument, metadata, context);

        AccessChecker checker = localConfig.getAccessChecker();
        checker.check(metadata);
    }

    // process PDF as a printed (vector) document
    // uses standard Tika's PDF2XHTML class by reflection
    // because this class is private (package restricted) and I don't
    // want to copy the class's code and a bunsh of dependent modules into plugin
    private void callPDF2XHTMLProcess(PDDocument document, ContentHandler handler,
                                        ParseContext context, Metadata metadata,
                                        PDFParserConfig config) throws
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class c = Class.forName("org.apache.tika.parser.pdf.PDF2XHTML");
        Method m = c.getDeclaredMethod("process", PDDocument.class, ContentHandler.class, ParseContext.class, Metadata.class,
                PDFParserConfig.class);
        m.setAccessible(true);
        m.invoke(null, document, handler, context, metadata, config);
    }

    // process PDF as a scanned image set
    // again uses reflection
    private void callOCR2XHTMLProcess(PDDocument document, ContentHandler handler,
                                        ParseContext context, Metadata metadata,
                                        PDFParserConfig config) throws
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        TesseractOCRConfig cfg = new TesseractOCRConfig();
        // here I set default timeout of 2 hours
        // The calling process should check parsing process and terminate it by timeout
        cfg.setTimeout(60 * 60 * 2);
        context.set(TesseractOCRConfig.class, cfg);

        PDFParserConfig.OCR_STRATEGY oldOcrStrategy = config.getOcrStrategy();
        boolean oldExtractInlineImages = config.getExtractInlineImages();
        boolean oldExtractUniqueInlineImagesOnly = config.getExtractUniqueInlineImagesOnly();

        // explicitly tells Tika to use OCR
        config.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.OCR_AND_TEXT_EXTRACTION);
        config.setExtractInlineImages(true);
        config.setExtractUniqueInlineImagesOnly(false);

        Class c = Class.forName("org.apache.tika.parser.pdf.OCR2XHTML");
        Method m = c.getDeclaredMethod("process",
                PDDocument.class, ContentHandler.class, ParseContext.class, Metadata.class,
                PDFParserConfig.class);
        m.setAccessible(true);
        m.invoke(null, document, handler, context, metadata, config);

        config.setOcrStrategy(oldOcrStrategy);
        config.setExtractInlineImages(oldExtractInlineImages);
        config.setExtractUniqueInlineImagesOnly(oldExtractUniqueInlineImagesOnly);
    }

    // check whether the method should read XFA (forms) only
    private boolean callShouldHandleXFAOnly(PDDocument pdDocument, PDFParserConfig config)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = getClass().getSuperclass().getDeclaredMethod("shouldHandleXFAOnly",
                PDDocument.class, PDFParserConfig.class);
        m.setAccessible(true);
        return (boolean)m.invoke(this, pdDocument, config);
    }

    // read XFA forms' content
    private void callHandleXFAOnly(PDDocument pdDocument, ContentHandler handler,
                               Metadata metadata, ParseContext context)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = getClass().getSuperclass().getDeclaredMethod("handleXFAOnly",
                PDDocument.class, ContentHandler.class, Metadata.class, ParseContext.class);
        m.setAccessible(true);
        m.invoke(this, pdDocument, handler, metadata, context);
    }

    // uses reflection, again, for obtaining PDF's metadata
    private void callExtractMetadata(PDDocument document, Metadata metadata, ParseContext context)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method m = getClass().getSuperclass().getDeclaredMethod("extractMetadata",
                PDDocument.class, Metadata.class, ParseContext.class);
        m.setAccessible(true);
        m.invoke(this, document, metadata, context);
    }

    // read password from metadata
    private String callGetPassword(Metadata metadata, ParseContext context)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method m = getClass().getSuperclass().getDeclaredMethod("getPassword",
                Metadata.class, ParseContext.class);
        m.setAccessible(true);
        Object retVal = m.invoke(this, metadata, context);
        return (String)retVal;
    }

    // make a copy because I don't want to modify original config params
    private PDFParserConfig makeConfigLocalCopy(PDFParserConfig srcConfig) {
        PDFParserConfig cpy = new PDFParserConfig();
        ShallowCopy.copyFields(srcConfig, cpy);
        return cpy;
    }
}
