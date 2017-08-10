package org.osgl.util;

import org.osgl.$;
import org.osgl.Lang;

abstract class PropertyHandlerBase implements PropertyHandler {
    protected Lang.Function<Class<?>, Object> objectFactory;
    protected Lang.Func2<String, Class<?>, ?> stringValueResolver;
    protected PropertyGetter.NullValuePolicy nullValuePolicy;

    PropertyHandlerBase() {
        this(SimpleObjectFactory.INSTANCE, SimpleStringValueResolver.INSTANCE);
    }

    PropertyHandlerBase(PropertyGetter.NullValuePolicy nullValuePolicy) {
        this(SimpleObjectFactory.INSTANCE, SimpleStringValueResolver.INSTANCE, nullValuePolicy);
    }

    PropertyHandlerBase(Lang.Function<Class<?>, Object> objectFactory, Lang.Func2<String, Class<?>, ?> stringValueResolver) {
        setObjectFactory(objectFactory);
        setStringValueResolver(stringValueResolver);
        setNullValuePolicy(PropertyGetter.NullValuePolicy.RETURN_NULL);
    }

    PropertyHandlerBase(Lang.Function<Class<?>, Object> objectFactory,
                        Lang.Func2<String, Class<?>, ?> stringValueResolver,
                        PropertyGetter.NullValuePolicy nullValuePolicy) {
        setObjectFactory(objectFactory);
        setStringValueResolver(stringValueResolver);
        if (null == nullValuePolicy) {
            nullValuePolicy = PropertyGetter.NullValuePolicy.RETURN_NULL;
        }
        setNullValuePolicy(nullValuePolicy);
    }

    @Override
    public void setObjectFactory(Lang.Function<Class<?>, Object> factory) {
        this.objectFactory = $.ensureNotNull(factory);
    }

    @Override
    public void setStringValueResolver(Lang.Func2<String, Class<?>, ?> stringValueResolver) {
        this.stringValueResolver = $.ensureNotNull(stringValueResolver);
    }

    public void setNullValuePolicy(PropertyGetter.NullValuePolicy nvp) {
        this.nullValuePolicy = $.ensureNotNull(nvp);
    }
}
