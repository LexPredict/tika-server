package com.lexpredict.tika;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.ToXMLContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class AlterXHTMLContentHandler extends XHTMLContentHandler {
    protected static final char[] emptyChar = new char[0];

    protected ToXMLContentHandler decoratedHandler;

    protected Method charactersRawMethod = null;

    public AlterXHTMLContentHandler(ContentHandler handler, Metadata metadata) {
        super(handler, metadata);
        try {
            Class c = Class.forName("org.apache.tika.sax.ContentHandlerDecorator");
            Field field = c.getDeclaredField("handler");
            field.setAccessible(true);
            Object decoratedHandlerObj = field.get(this);

            if (decoratedHandlerObj instanceof ToXMLContentHandler) {
                // handlerClassName can also be TaggedContentHandler
                this.decoratedHandler = (ToXMLContentHandler)field.get(this);
                c = Class.forName("org.apache.tika.sax.ToXMLContentHandler");
                this.charactersRawMethod = c.getDeclaredMethod("write",
                        String.class);
                this.charactersRawMethod.setAccessible(true);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public boolean isCharacterInvalid(char c) {
        return this.isInvalid(c);
    }

    public void charactersRaw(String data) throws SAXException {
        if (this.charactersRawMethod == null) {
            super.characters(data);
            return;
        }

        super.characters(emptyChar, 0, 0);
        try {
            this.charactersRawMethod.invoke(this.decoratedHandler, data);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
