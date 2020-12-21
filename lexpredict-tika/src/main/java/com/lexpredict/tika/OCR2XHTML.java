package com.lexpredict.tika;
import java.io.IOException;
import java.io.Writer;
import org.apache.commons.io.IOExceptionWithCause;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.TextPosition;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

class OCR2XHTML extends AbstractPDF2XHTML {
    private OCR2XHTML(PDDocument document, ContentHandler handler, ParseContext context, 
                      Metadata metadata, PDFParserConfig config) throws IOException {
        super(document, handler, context, metadata, config);
    }

    public static void process(PDDocument document, ContentHandler handler, 
                               ParseContext context, Metadata metadata, PDFParserConfig config) throws SAXException, TikaException {
        OCR2XHTML ocr2XHTML = null;

        try {
            ocr2XHTML = new OCR2XHTML(document, handler, context, metadata, config);
            ocr2XHTML.writeText(document, new Writer() {
                public void write(char[] cbuf, int off, int len) {
                }

                public void flush() {
                }

                public void close() {
                }
            });
        } catch (IOException var7) {
            if (var7.getCause() instanceof SAXException) {
                throw (SAXException)var7.getCause();
            }

            throw new TikaException("Unable to extract PDF content", var7);
        }

        if (ocr2XHTML.exceptions.size() > 0) {
            throw new TikaException("Unable to extract all PDF content", (Throwable)ocr2XHTML.exceptions.get(0));
        }
    }

    public void processPage(PDPage pdPage) throws IOException {
        try {
            this.startPage(pdPage);
            this.doOCROnCurrentPage();
            this.endPage(pdPage);
        } catch (SAXException | TikaException var3) {
            throw new IOExceptionWithCause(var3);
        } catch (IOException var4) {
            this.handleCatchableIOE(var4);
        }

    }

    protected void writeString(String text) throws IOException {
    }

    protected void writeCharacters(TextPosition text) throws IOException {
    }

    protected void writeWordSeparator() throws IOException {
    }

    protected void writeLineSeparator() throws IOException {
    }
}
