package com.lexpredict.tika;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.MetaData;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;

enum CommonParseFlag
{
    VERBOSE, PDF_PARSE_METHOD;
}

// class reads HttpRequest params from InputStream
// if InputStream is from HttpRequest
public class HttpRequestParamsReader {
    public static final String PDF_PARSE_METHOD_STRIP = "strip";
    public static final String PDF_PARSE_METHOD_PDF_OCR = "pdf_ocr";

    public HashMap<String, String> rawParams = new HashMap<String, String>();
    public HashMap<CommonParseFlag, String> typedParams = new HashMap<>();

    private static HashMap<String, CommonParseFlag> flagByName = new HashMap<String, CommonParseFlag>() {
        {
            put("v", CommonParseFlag.VERBOSE);
            put("-verbose", CommonParseFlag.VERBOSE);
            put("pdf-parse", CommonParseFlag.PDF_PARSE_METHOD);
        }
    };

    private static HttpRequestParamsReader single_instance = null;

    private boolean initialized = false;

    private HttpRequestParamsReader()
    {
    }

    // static method to create instance of Singleton class
    public static HttpRequestParamsReader getInstance()
    {
        if (single_instance == null)
            single_instance = new HttpRequestParamsReader();
        return single_instance;
    }

    public void initialize(InputStream stream) {
        if (initialized)
            return;
        initialized = true;
        MetaData metaDict = getMetaDataField(stream);
        if (metaDict == null)
            return;

        HttpFields fields = metaDict.getFields();
        for (HttpField field : fields)
            rawParams.put(field.getName(), field.getValue());
        GetCommonFlags();
    }

    public boolean IsVerbose() {
        return typedParams.containsKey(CommonParseFlag.VERBOSE);
    }

    public void outIfVerbose(String s) {
        if (!IsVerbose()) return;
        System.out.println(s);
    }

    // just check the value specified in the dictionary passed
    public boolean checkParamValue(CommonParseFlag ptrName, String expectedValue) {
        return typedParams.containsKey(ptrName) &&
                typedParams.get(ptrName).equalsIgnoreCase(
                        expectedValue);
    }

    private void GetCommonFlags() {
        rawParams.entrySet().forEach(entry -> {
            flagByName.entrySet().forEach(fl -> {
                if (fl.getKey().equals(entry.getKey()))
                    typedParams.put(fl.getValue(), entry.getValue());
            });
        });
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
