package com.lexpredict.tika;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.config.InitializableProblemHandler;
import org.apache.tika.exception.AccessPermissionException;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.PDF;
import org.apache.tika.metadata.Property;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.parser.pdf.*;
import org.apache.tika.sax.*;
import org.apache.tika.sax.xpath.Matcher;
import org.apache.tika.sax.xpath.MatchingContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

public class AlterPDFParser extends PDFParser {
    public enum ParsePdfMode {
        DEFAULT, PDF_OCR, TEXT_STRIP
    }

    public ParsePdfMode defaultParseMode = ParsePdfMode.DEFAULT;

    class TrueMatcher extends Matcher {
        @Override
        public boolean matchesText() {
            return true;
        }
    }

    private static volatile boolean HAS_WARNED = false;
    private static final Object[] LOCK = new Object[0];
    //the old "created" metadata.  This will go away in Tika 2.0
    private static final Property DEPRECATED_CREATED = Property.externalDate("created");

    // Metadata key for giving the document password to the parser
    public static final String PASSWORD = "org.apache.tika.parser.pdf.password";
    private static final MediaType MEDIA_TYPE = MediaType.application("pdf");

    // Serial version UID
    private static final long serialVersionUID = -752276948656079347L;
    private static final Set<MediaType> SUPPORTED_TYPES =
            Collections.singleton(MEDIA_TYPE);
    private PDFParserConfig defaultConfig = new PDFParserConfig();


    @Override
    public void parse(
            InputStream stream, ContentHandler handler,
            Metadata metadata, ParseContext context)
            throws IOException, SAXException, TikaException {
        HashMap<String, String> requestMap = HttpRequestParamsReader.readQueryParameters(stream);
        ParsePdfMode pdfParseMode = readParseMode(requestMap);

        PDFParserConfig localConfig = context.get(PDFParserConfig.class, defaultConfig);
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
                callHandleXFAOnly(pdfDocument, handler, metadata, context);
            }
            else if (localConfig.getOcrStrategy().equals(PDFParserConfig.OCR_STRATEGY.OCR_ONLY)) {
                metadata.add("X-Parsed-By", TesseractOCRParser.class.toString());
                callOCR2XHTMLProcess(pdfDocument, handler, context, metadata, localConfig);
            }
            else {
                // parse document by using PDFStripper
                if (pdfParseMode == ParsePdfMode.TEXT_STRIP)
                    setTextUsingPDFTextStripper(handler, pdfDocument);
                // smart parsing: PDF or OCR
                else if (pdfParseMode == ParsePdfMode.PDF_OCR) {
                    PdfContentTypeChecker checker = new PdfContentTypeChecker();
                    PdfContentTypeChecker.PdfContent docType = checker.determineDocContentType(pdfDocument);
                    if (docType != PdfContentTypeChecker.PdfContent.IMAGES)
                        callPDF2XHTMLProcess(pdfDocument, handler, context, metadata, localConfig);
                    else {
                        metadata.add("X-Parsed-By", TesseractOCRParser.class.toString());
                        callOCR2XHTMLProcess(pdfDocument, handler, context, metadata, localConfig);
                    }
                }
                else // ... or parse it default Tika-way
                    callPDF2XHTMLProcess(pdfDocument, handler, context, metadata, localConfig);
            }

        } catch (InvalidPasswordException e) {
            metadata.set(PDF.IS_ENCRYPTED, "true");
            throw new EncryptedDocumentException(e);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // see e.getCause()
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pdfDocument != null) {
                pdfDocument.close();
            }
        }
    }

    private ParsePdfMode readParseMode(HashMap<String, String> requestMap) {
        if (HttpRequestParamsReader.checkParamValue(requestMap,
                HttpRequestParamsReader.PDF_PARSE_METHOD,
                HttpRequestParamsReader.PDF_PARSE_METHOD_STRIP))
            return ParsePdfMode.TEXT_STRIP;
        if (HttpRequestParamsReader.checkParamValue(requestMap,
                HttpRequestParamsReader.PDF_PARSE_METHOD,
                HttpRequestParamsReader.PDF_PARSE_METHOD_PDF_OCR))
            return ParsePdfMode.PDF_OCR;
        return defaultParseMode;
    }

    private void extractAndCheckMetadata(Metadata metadata, ParseContext context, PDFParserConfig localConfig, PDDocument pdfDocument)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, AccessPermissionException {
        metadata.set(PDF.IS_ENCRYPTED, Boolean.toString(pdfDocument.isEncrypted()));
        metadata.set(Metadata.CONTENT_TYPE, MEDIA_TYPE.toString());
        callExtractMetadata(pdfDocument, metadata, context);

        AccessChecker checker = localConfig.getAccessChecker();
        checker.check(metadata);
    }

    private void setTextUsingPDFTextStripper(ContentHandler handler, PDDocument pdfDocument)
            throws IOException, SAXException, NoSuchMethodException, InvocationTargetException,
                IllegalAccessException, NoSuchFieldException {
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(pdfDocument);
        char[] chars = text.toCharArray();
        setContentHandlerCharacters(handler, chars);
    }

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

    private void callOCR2XHTMLProcess(PDDocument document, ContentHandler handler,
                                        ParseContext context, Metadata metadata,
                                        PDFParserConfig config) throws
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        PDFParserConfig.OCR_STRATEGY oldOcrStrategy = config.getOcrStrategy();
        boolean oldExtractInlineImages = config.getExtractInlineImages();
        boolean oldExtractUniqueInlineImagesOnly = config.getExtractUniqueInlineImagesOnly();

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

    private boolean callShouldHandleXFAOnly(PDDocument pdDocument, PDFParserConfig config)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = getClass().getSuperclass().getDeclaredMethod("shouldHandleXFAOnly", PDDocument.class, PDFParserConfig.class);
        m.setAccessible(true);
        return (boolean)m.invoke(this, pdDocument, config);
    }

    private void callHandleXFAOnly(PDDocument pdDocument, ContentHandler handler,
                               Metadata metadata, ParseContext context)
            throws SAXException, IOException, TikaException, NoSuchMethodException,
                InvocationTargetException, IllegalAccessException {
        Method m = getClass().getSuperclass().getDeclaredMethod("handleXFAOnly", PDDocument.class, ContentHandler.class, Metadata.class, ParseContext.class);
        m.setAccessible(true);
        m.invoke(this, pdDocument, handler, metadata, context);
    }

    private void callExtractMetadata(PDDocument document, Metadata metadata, ParseContext context)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method m = getClass().getSuperclass().getDeclaredMethod("extractMetadata", PDDocument.class, Metadata.class, ParseContext.class);
        m.setAccessible(true);
        m.invoke(this, document, metadata, context);
    }

    private String callGetPassword(Metadata metadata, ParseContext context)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method m = getClass().getSuperclass().getDeclaredMethod("getPassword",
                Metadata.class, ParseContext.class);
        m.setAccessible(true);
        Object retVal = m.invoke(this, metadata, context);
        return (String)retVal;
    }

    private void setContentHandlerCharacters(ContentHandler handler, char[] chars)
            throws SAXException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
            NoSuchFieldException, IOException {

        advanceSecureContentHandler(handler, chars.length);

        ContentHandler textHandler = getUnderlyingHandler(handler, ToTextContentHandler.class);
        if (textHandler instanceof ToTextContentHandler) {
            writeCharsToTextHandler((ToTextContentHandler)textHandler, chars);
            return;
        }

        ContentHandler matchHandler = getUnderlyingHandler(handler, MatchingContentHandler.class);
        if (matchHandler instanceof MatchingContentHandler) {
            setCharsBypassingMatching(handler, (MatchingContentHandler)matchHandler, chars);
            return;
        }

        handler.characters(chars, 0, chars.length);
    }

    private void advanceSecureContentHandler(ContentHandler handler, int bytesCount)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ContentHandler secHandler = getUnderlyingHandler(handler, SecureContentHandler.class);
        if (!(secHandler instanceof SecureContentHandler))
            return;
        Method adMethod = SecureContentHandler.class.getDeclaredMethod("advance",
                int.class);
        adMethod.setAccessible(true);
        adMethod.invoke(secHandler, bytesCount);
    }

    private void writeCharsToTextHandler(ToTextContentHandler handler, char[] chars)
            throws IllegalAccessException, NoSuchFieldException, IOException {
        Field writerField = lookupFieldInClass(handler.getClass(), "writer");
        if (writerField == null)
            throw new NoSuchFieldException("writer");
        writerField.setAccessible(true);
        Writer writer = (Writer)writerField.get(handler);
        writer.write(chars);
        writer.close();
    }

    private void directlySetCharacters(ContentHandler handler, char[] chars)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method setter;
        while (true) {
            try {
                setter = handler.getClass().getDeclaredMethod("characters",
                        char[].class, int.class, int.class);
                break;
            } catch (NoSuchMethodException e) {
            }
            Field handField = lookupFieldInClass(handler.getClass(), "handler");
            if (handField == null)
                throw new NoSuchMethodException("characters");
            handField.setAccessible(true);
            handler = (ContentHandler)handField.get(handler);
        }
        setter.invoke(handler, chars, 0, chars.length);
    }

    private void setCharsBypassingMatching(ContentHandler handler, MatchingContentHandler matchHandler, char[] chars)
            throws IllegalAccessException, NoSuchFieldException,
                InvocationTargetException, NoSuchMethodException {
        Field matchField = MatchingContentHandler.class.getDeclaredField("matcher");
        matchField.setAccessible(true);
        Object oldMatcher = matchField.get(matchHandler);
        matchField.set(matchHandler, new TrueMatcher());

        directlySetCharacters(handler, chars);

        matchField.set(matchHandler, oldMatcher);
    }

    private ContentHandler getUnderlyingHandler(ContentHandler handler, Class<?> desiredClass)
            throws IllegalAccessException {
        while (true) {
            Class<?> handlerClass = handler.getClass();
            if (handlerClass == desiredClass) break;
            Field handlerField = lookupFieldInClass(handlerClass,"handler");
            if (handlerField == null)
                return handler;

            handlerField.setAccessible(true);
            handler = (ContentHandler) handlerField.get(handler);
        }
        return handler;
    }

    private Field lookupFieldInClass(Class<?> cls, String fieldName) {
        while (true) {
            try {
                return cls.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
            }
            cls = cls.getSuperclass();
            if (cls == null) break;
        }
        return null;
    }
}
