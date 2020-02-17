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
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

// determine content of the PDDocument passed:
// whether it contains text, images, text + images or just nothing
public class PdfContentTypeChecker {
    public enum PdfContent {
        EMPTY, TEXT, IMAGES, MIXED, UNKNOWN
    }

    private PdfContent docContent = PdfContent.EMPTY;

    private int pageCount = 0;

    private int imagesCount = 0;

    private int textBlocks = 0;

    private int fullTextLength = 0;

    private PDFTextStripper pdfTextStripper;

    // reads PDDocument from the stream and calls determineDocContentType
    public PdfContent determineDocContentType(InputStream stream) {
        try {
            PDDocument document = PDDocument.load(stream);
            return determineDocContentType(document);
        } catch (Exception e) {
            return PdfContent.UNKNOWN;
        }
    }

    public PdfContent determineDocContentType(PDDocument document) throws IOException {
        try {
            calculateObjectsInDocument(document);
        } catch (Exception e) {
            return PdfContent.UNKNOWN;
        }
        int totalCount = imagesCount + textBlocks;
        docContent = totalCount == 0 ? PdfContent.EMPTY
                : imagesCount > 0 && textBlocks > 0 && fullTextLength > 500 * pageCount ? PdfContent.MIXED
                : imagesCount > 0 ? PdfContent.IMAGES
                : PdfContent.TEXT;
        return docContent;
    }

    // calculate count of text blocks (textBlocks member) and
    // images (imagesCount) in the document
    private void calculateObjectsInDocument(PDDocument document) throws IOException {
        this.pdfTextStripper = new PDFTextStripper();

        try {
            PDPageTree allPages = document.getDocumentCatalog().getPages();
            this.pageCount = allPages.getCount();
            for (int i = 0; i < allPages.getCount(); i++) {
                PDPage page = allPages.get(i);
                readObjectsOnPage(page);
                calculateTextLengthOnPage(document, i + 1);
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


    private void calculateTextLengthOnPage(PDDocument doc, int pageNum1Based) throws IOException {
        this.pdfTextStripper.setStartPage(pageNum1Based);
        this.pdfTextStripper.setEndPage(pageNum1Based);
        String text = this.pdfTextStripper.getText(doc);
        if (text != null) {
            text = text.trim().replaceAll("\\s+", " ");
            this.fullTextLength += text.length();
        }
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
