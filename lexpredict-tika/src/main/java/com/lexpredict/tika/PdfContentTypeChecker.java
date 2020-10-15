package com.lexpredict.tika;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


// import org.apache.tika.parser.pdf.PDFParser;
// class MyPDF2XHTML extends PDF2XHTML {



// determine content of the PDDocument passed:
// whether it contains text, images, text + images or just nothing
public class PdfContentTypeChecker {
    public enum PdfContent {
        EMPTY, TEXT, IMAGES, MIXED, UNKNOWN
    }

    private PdfContent docContent = PdfContent.EMPTY;

    private int imagesCount = 0;

    private int textBlocks = 0;

    public int getImagesCount() {
        return imagesCount;
    }

    public int getTextBlocks() {
        return textBlocks;
    }

    // reads PDDocument from the stream and calls determineDocContentType
    public PdfContent determineDocContentType(InputStream stream) {
        try {
            PDDocument document = PDDocument.load(stream);
            return determineDocContentType(document);
        } catch (Exception e) {
            return PdfContent.UNKNOWN;
        }
    }

    public PdfContent determineDocContentType(PDDocument document) {
        try {
            calculateObjectsInDocument(document);
        } catch (Exception e) {
            return PdfContent.UNKNOWN;
        }
        int totalCount = imagesCount + textBlocks;
        docContent = totalCount == 0 ? PdfContent.EMPTY :
                imagesCount > 0 && textBlocks > 0 ? PdfContent.MIXED :
                        imagesCount > 0 ? PdfContent.IMAGES : PdfContent.TEXT;
        return docContent;
    }

    // calculate count of text blocks (textBlocks member) and
    // images (imagesCount) in the document
    private void calculateObjectsInDocument(PDDocument document) {
        try {
            PDPageTree allPages = document.getDocumentCatalog().getPages();
            for (int i = 0; i < allPages.getCount(); i++) {
                PDPage page = (PDPage) allPages.get(i);
                readObjectsOnPage(page);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // calculate objects' count for the page passed
    private void readObjectsOnPage(PDPage page) throws IOException {
        getImagesFromResources(page.getResources());
        calculateTextObjectsOnPage(page);
    }

    private void calculateTextObjectsOnPage(PDPage page) throws IOException {
        PDFStreamParser parser = new PDFStreamParser(page);
        parser.parse();
        List<Object> pageTokens = parser.getTokens();
        for (Object token : pageTokens) {
            if (token instanceof Operator) {
                String opName = ((Operator) token).getName();
                if (opName.equals("BT")) // Begin Text
                    textBlocks++;
            }
        }
    }

    private void getImagesFromResources(PDResources resources) throws IOException {
        for (COSName xObjectName : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(xObjectName);

            if (xObject instanceof PDFormXObject) {
                getImagesFromResources(((PDFormXObject) xObject).getResources());
            } else if (xObject instanceof PDImageXObject) {
                //((PDImageXObject) xObject).getImage();
                imagesCount++;
            }
        }
    }
}
