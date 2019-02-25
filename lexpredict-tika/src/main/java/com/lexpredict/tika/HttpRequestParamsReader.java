package com.lexpredict.tika;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.MetaData;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;

// class reads HttpRequest params from InputStream
// if InputStream is from HttpRequest
public class HttpRequestParamsReader {
    public static final String PDF_PARSE_METHOD = "pdf-parse";
    public static final String PDF_PARSE_METHOD_STRIP = "strip";
    public static final String PDF_PARSE_METHOD_PDF_OCR = "pdf_ocr";

    public static HashMap<String, String> readQueryParameters(InputStream stream) {
        HashMap<String, String> map = new HashMap<String, String>();
        MetaData metaDict = getMetaDataField(stream);
        if (metaDict == null)
            return map;

        HttpFields fields = metaDict.getFields();
        for (HttpField field : fields)
            map.put(field.getName(), field.getValue());

        return map;
    }

    // just check the value specified in the dictionary passed
    public static boolean checkParamValue(HashMap<String, String> requestMap,
                                   String ptrName, String expectedValue) {
        return requestMap.containsKey(ptrName) &&
                requestMap.get(ptrName).equalsIgnoreCase(
                        expectedValue);
    }

    // read metadata from HttpRequest
    private static MetaData getMetaDataField(Object stream) {
        while (true) {
            try {
                Field field = FieldLookup.findField(stream.getClass(), "val$req");
                if (field != null) {
                    field.setAccessible(true);
                    HttpServletRequest req = (HttpServletRequest) field.get(stream);
                    field = FieldLookup.findField(req.getClass(), "_metaData");
                    if (field == null)
                        return null;
                    field.setAccessible(true);
                    return (MetaData) field.get(req);
                }
            } catch (IllegalAccessException ex) {
                return null;
            }

            Field inField = FieldLookup.findField(stream.getClass(), "in");
            if (inField == null)
                return null;
            inField.setAccessible(true);
            try {
                stream = inField.get(stream);
            } catch (IllegalAccessException e) {
                return null;
            }
        }
    }


}
