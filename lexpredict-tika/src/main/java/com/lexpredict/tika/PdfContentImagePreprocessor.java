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

    private PDImageXObject makeImageObjectCopy(PDImageXObject img) throws IOException {
        BufferedImage flatImg = flattenImage(img.getImage());
        byte[] bytes = getImageBytes(flatImg);
        PDImageXObject newImg = PDImageXObject.createFromByteArray(document, bytes, "image");
        return newImg;
    }

    private BufferedImage flattenImage(BufferedImage img) {
        BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = copy.createGraphics();
        g2d.setColor(Color.WHITE); // Or what ever fill color you want...
        g2d.fillRect(0, 0, copy.getWidth(), copy.getHeight());
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();
        return copy;
    }

    private byte[] getImageBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos );
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();
        return imageInByte;
    }
}
