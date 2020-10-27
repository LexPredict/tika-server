package com.lexpredict.tika;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;

class AlterXHTMLContentHandler extends XHTMLContentHandler {
    public AlterXHTMLContentHandler(ContentHandler handler, Metadata metadata) {
        super(handler, metadata);
    }

    public boolean isCharacterInvalid(char c) {
        return this.isInvalid(c);
    }
}
