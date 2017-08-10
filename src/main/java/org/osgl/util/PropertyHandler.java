package org.osgl.util;

import org.osgl.Lang;

public interface PropertyHandler {
    void setObjectFactory(Lang.Function<Class<?>, Object> factory);
    void setStringValueResolver(Lang.Func2<String, Class<?>, ?> stringValueResolver);
}
