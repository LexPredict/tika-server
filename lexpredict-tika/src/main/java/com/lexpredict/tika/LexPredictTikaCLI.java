package com.lexpredict.tika;

import org.apache.tika.cli.TikaCLI;

import javax.imageio.spi.IIORegistry;

public class LexPredictTikaCLI extends TikaCLI {

    @Override
    public void process(String arg) throws Exception {
        System.out.println("Manually registering JPEG2000 reader...");
        IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(new com.github.jaiimageio.jpeg2000.impl.J2KImageReaderSpi());
        System.out.println("Proceeding to base TIKA CLI...");
        super.process(arg);
        System.out.println("TIKA CLI finished.");
    }
}
