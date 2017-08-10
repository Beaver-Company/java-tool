package org.osgl.util;

import org.osgl.$;
import org.osgl.Osgl;

class MapPropertyHandler extends PropertyHandlerBase {

    protected final Class<?> keyType;
    protected final Class<?> valType;

    public MapPropertyHandler(Class<?> keyType, Class<?> valType) {
        this.keyType = $.ensureNotNull(keyType);
        this.valType = $.ensureNotNull(valType);
    }

    public MapPropertyHandler(PropertyGetter.NullValuePolicy nullValuePolicy,
                              Class<?> keyType,
                              Class<?> valType) {
        super(nullValuePolicy);
        this.keyType = $.ensureNotNull(keyType);
        this.valType = $.ensureNotNull(valType);
    }

    public MapPropertyHandler(Osgl.Function<Class<?>, Object> objectFactory,
                              Osgl.Func2<String, Class<?>, ?> stringValueResolver,
                              Class<?> keyType,
                              Class<?> valType) {
        super(objectFactory, stringValueResolver);
        this.keyType = $.ensureNotNull(keyType);
        this.valType = $.ensureNotNull(valType);
    }

    public MapPropertyHandler(Osgl.Function<Class<?>, Object> objectFactory,
                              Osgl.Func2<String, Class<?>, ?> stringValueResolver,
                              PropertyGetter.NullValuePolicy nullValuePolicy,
                              Class<?> keyType,
                              Class<?> valType) {
        super(objectFactory, stringValueResolver, nullValuePolicy);
        this.keyType = $.ensureNotNull(keyType);
        this.valType = $.ensureNotNull(valType);
    }

    protected Object keyFrom(Object index) {
        if (keyType.isAssignableFrom(index.getClass())) {
            return index;
        } else {
            return stringValueResolver.apply(S.string(index), keyType);
        }
    }
}
