package com.lexpredict.tika;

import java.lang.reflect.Field;

public class ShallowCopy {
        public static void copyFields(Object from, Object to) {
            Field[] fields = from.getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                    Field fieldFrom = from.getClass().getDeclaredField(field.getName());
                    if (java.lang.reflect.Modifier.isStatic(fieldFrom.getModifiers()))
                        continue;

                    boolean wasAccessed = fieldFrom.isAccessible();
                    fieldFrom.setAccessible(true);
                    Object value = fieldFrom.get(from);
                    fieldFrom.setAccessible(wasAccessed);

                    Field fieldTo = to.getClass().getDeclaredField(field.getName());
                    fieldTo.setAccessible(true);
                    fieldTo.set(to, value);
                    fieldTo.setAccessible(wasAccessed);

                } catch (IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        }
}
