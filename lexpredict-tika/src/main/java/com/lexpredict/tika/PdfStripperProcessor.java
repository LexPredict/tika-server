package com.lexpredict.tika;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.sax.SecureContentHandler;
import org.apache.tika.sax.ToTextContentHandler;
import org.apache.tika.sax.xpath.Matcher;
import org.apache.tika.sax.xpath.MatchingContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// Class uses PDFBox text "stripping" methods
// instead of Tika's ones
// Sometimes PDFBox method format extracted text better than Tika
public class PdfStripperProcessor {
    public static void setTextUsingPDFTextStripper(ContentHandler handler, PDDocument pdfDocument)
            throws IOException, SAXException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, NoSuchFieldException {
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(pdfDocument);
        char[] chars = text.toCharArray();
        setContentHandlerCharacters(handler, chars);
    }

    private static void setContentHandlerCharacters(ContentHandler handler, char[] chars)
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

    private static void advanceSecureContentHandler(ContentHandler handler, int bytesCount)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ContentHandler secHandler = getUnderlyingHandler(handler, SecureContentHandler.class);
        if (!(secHandler instanceof SecureContentHandler))
            return;
        Method adMethod = SecureContentHandler.class.getDeclaredMethod("advance",
                int.class);
        adMethod.setAccessible(true);
        adMethod.invoke(secHandler, bytesCount);
    }

    private static void writeCharsToTextHandler(ToTextContentHandler handler, char[] chars)
            throws IllegalAccessException, NoSuchFieldException, IOException {
        Field writerField = FieldLookup.findField(handler.getClass(), "writer");
        if (writerField == null)
            throw new NoSuchFieldException("writer");
        writerField.setAccessible(true);
        Writer writer = (Writer)writerField.get(handler);
        writer.write(chars);
        writer.close();
    }

    private static void directlySetCharacters(ContentHandler handler, char[] chars)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method setter;
        while (true) {
            try {
                setter = handler.getClass().getDeclaredMethod("characters",
                        char[].class, int.class, int.class);
                break;
            } catch (NoSuchMethodException e) {
                // pass
            }
            Field handField = FieldLookup.findField(handler.getClass(), "handler");
            if (handField == null)
                throw new NoSuchMethodException("characters");
            handField.setAccessible(true);
            handler = (ContentHandler)handField.get(handler);
        }
        setter.invoke(handler, chars, 0, chars.length);
    }

    private static void setCharsBypassingMatching(ContentHandler handler, MatchingContentHandler matchHandler, char[] chars)
            throws IllegalAccessException, NoSuchFieldException,
            InvocationTargetException, NoSuchMethodException {
        Field matchField = MatchingContentHandler.class.getDeclaredField("matcher");
        matchField.setAccessible(true);
        Object oldMatcher = matchField.get(matchHandler);
        matchField.set(matchHandler, new TrueMatcher());

        directlySetCharacters(handler, chars);

        matchField.set(matchHandler, oldMatcher);
    }

    private static ContentHandler getUnderlyingHandler(ContentHandler handler, Class<?> desiredClass)
            throws IllegalAccessException {
        while (true) {
            Class<?> handlerClass = handler.getClass();
            if (handlerClass == desiredClass) break;
            Field handlerField = FieldLookup.findField(handlerClass,"handler");
            if (handlerField == null)
                return handler;

            handlerField.setAccessible(true);
            handler = (ContentHandler) handlerField.get(handler);
        }
        return handler;
    }
}

// the class tells calling code that the passed content should be
// included in the output
class TrueMatcher extends Matcher {
    @Override
    public boolean matchesText() {
        return true;
    }
}