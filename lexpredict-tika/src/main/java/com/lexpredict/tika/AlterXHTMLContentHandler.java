package com.lexpredict.tika;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.SafeContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

class AlterXHTMLContentHandler extends XHTMLContentHandler {
    public AlterXHTMLContentHandler(ContentHandler handler, Metadata metadata) {
        super(handler, metadata);
    }

    public boolean isCharacterInvalid(char c) {
        return this.isInvalid(c);
    }
}
