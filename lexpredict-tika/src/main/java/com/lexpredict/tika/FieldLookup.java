package com.lexpredict.tika;

import java.lang.reflect.Field;

public class FieldLookup {

    // find field in passed class or one of his ancestors
    public static Field findField(Class<?> cls, String fieldName) {
        while (true) {
            try {
                return cls.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // pass
            }
            cls = cls.getSuperclass();
            if (cls == null) break;
        }
        return null;
    }
}
