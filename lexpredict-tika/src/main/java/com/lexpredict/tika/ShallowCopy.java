package com.lexpredict.tika;

import java.lang.reflect.Field;

public class ShallowCopy {
        public static void copyFields(Object from, Object to) {
            Field[] fields = from.getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                    Field fieldFrom = from.getClass().getDeclaredField(field.getName());
                    Object value = fieldFrom.get(from);
                    to.getClass().getDeclaredField(field.getName()).set(to, value);

                } catch (IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        }
}
