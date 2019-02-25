package com.lexpredict.tika;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

// TODO: somehow we should determine image type from COSName
// or PDImageXObject before saving it back to ByteArrayOutputStream in getImageBytes()

// TODO: determine contrast background color in flattenImage()

public class PdfContentImagePreprocessor {
    private boolean imagesWereChanged;

    private PDDocument document;

    public boolean removeImagesAlphaChannel(PDDocument document) {
        this.document = document;
        imagesWereChanged = false;
        try {
            removeImagesAlphaChannelUnsafe();
            return imagesWereChanged;
        } catch (Exception e) {
            return false;
        }
    }

    private void removeImagesAlphaChannelUnsafe() {
        try {
            PDPageTree allPages = document.getDocumentCatalog().getPages();
            for (int i = 0; i < allPages.getCount(); i++) {
                PDPage page = allPages.get(i);
                processImagesFromResources(page.getResources());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // search for images in document's resources
    private void processImagesFromResources(PDResources resources) throws IOException {
        for (COSName xObjectName : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(xObjectName);

            if (xObject instanceof PDFormXObject) {
                processImagesFromResources(((PDFormXObject) xObject).getResources());
            } else if (xObject instanceof PDImageXObject) {
                PDImageXObject img = (PDImageXObject) xObject;
                if (!img.getImage().getColorModel().hasAlpha())
                    return;

                PDImageXObject cpy = makeImageObjectCopy(img);
                resources.put(xObjectName, cpy);
                imagesWereChanged = true;
            }
        }
    }

    // load the image, "flatten" it and store it into bytes
    // then return new PDImageXObject from image's bytes
    private PDImageXObject makeImageObjectCopy(PDImageXObject img) throws IOException {
        BufferedImage flatImg = flattenImage(img.getImage());
        byte[] bytes = getImageBytes(flatImg);
        return PDImageXObject.createFromByteArray(document, bytes, "image");
    }

    // make a new BufferedImage drawn on a solid background
    private BufferedImage flattenImage(BufferedImage img) {
        BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = copy.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, copy.getWidth(), copy.getHeight());
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();
        return copy;
    }

    // serialize image as bytes
    private byte[] getImageBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos );
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();
        return imageInByte;
    }
}
