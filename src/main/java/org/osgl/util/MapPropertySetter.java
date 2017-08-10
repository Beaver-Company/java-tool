package org.osgl.util;

import org.osgl.Lang;

import java.util.Map;

/**
 * Implement {@link PropertySetter} on {@link java.util.Map} type entity specifically
 */
public class MapPropertySetter extends MapPropertyHandler implements PropertySetter {

    public MapPropertySetter(Class<?> keyType, Class<?> valType) {
        super(keyType, valType);
    }

    MapPropertySetter(Lang.Function<Class<?>, Object> objectFactory,
                      Lang.Func2<String, Class<?>, ?> stringValueResolver,
                      Class<?> keyType,
                      Class<?> valType) {
        super(objectFactory, stringValueResolver, keyType, valType);
        setNullValuePolicy(PropertyGetter.NullValuePolicy.CREATE_NEW);
    }

    @Override
    public void set(Object entity, Object value, Object index) {
        Map map = (Map) entity;
        Object key = keyFrom(index);
        map.put(key, value);
    }

}
