package org.osgl.util;

import org.osgl.Lang;
import org.osgl.exception.NotAppliedException;

import java.util.List;
import java.util.Map;

public class SimpleObjectFactory extends Lang.F1<Class<?>, Object> {

    public static final SimpleObjectFactory INSTANCE = new SimpleObjectFactory();

    @Override
    public Object apply(Class<?> aClass) throws NotAppliedException, Lang.Break {
        if (List.class.isAssignableFrom(aClass)) {
            return C.Mutable.List();
        } else if (Map.class.isAssignableFrom(aClass)) {
            return C.Mutable.Map();
        }
        return Lang.newInstance(aClass);
    }
}
