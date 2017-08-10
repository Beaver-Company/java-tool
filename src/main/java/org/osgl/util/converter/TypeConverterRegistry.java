package org.osgl.util.converter;

import org.osgl.$;

import java.util.*;

public class TypeConverterRegistry {

    public static final TypeConverterRegistry INSTANCE = new TypeConverterRegistry();

    private SortedMap<$.Pair<Class, Class>, $.TypeConverter> paths = new TreeMap<>();

    public <FROM, TO> $.TypeConverter<FROM, TO> get(Class<FROM> fromType, Class<TO> toType) {
        return $.cast(paths.get($.Pair(fromType, toType)));
    }

    public TypeConverterRegistry register($.TypeConverter typeConverter) {
        for ($.Pair<Class, Class> key : allKeyOf(typeConverter)) {
            if (!paths.containsKey(key)) {
                paths.put(key, typeConverter);
            }
        }
        buildPaths(typeConverter);
        return this;
    }

    private $.Pair<Class, Class> keyOf($.TypeConverter typeConverter) {
        return $.Pair(typeConverter.fromType, typeConverter.toType);
    }

    private Set<$.Pair<Class, Class>> allKeyOf($.TypeConverter typeConverter) {
        Set<$.Pair<Class, Class>> set = new HashSet<>();
        Class fromType = typeConverter.fromType;
        Class toType = typeConverter.toType;
        do {
            set.add($.Pair(fromType, toType));
            toType = toType.getSuperclass();
        } while (Object.class != toType);
        Class objectClass = Object.class;
        set.add($.Pair(fromType, objectClass));
        return set;
    }

    private void buildPaths($.TypeConverter typeConverter) {
        Class fromType = typeConverter.fromType;
        for ($.TypeConverter upstream : upstreams(fromType)) {
            $.TypeConverter chained = new ChainedConverter(upstream, typeConverter);
            $.Pair<Class, Class> key = keyOf(chained);
            $.TypeConverter current = paths.get(chained);
            if (isShorterPath(chained, current)) {
                paths.put(key, chained);
            }
        }
    }

    private List<$.TypeConverter> upstreams(Class toType) {
        List<$.TypeConverter> list = new ArrayList<>();
        for (Map.Entry<$.Pair<Class, Class>, $.TypeConverter> entry : paths.entrySet()) {
            if (toType.isAssignableFrom(entry.getKey().right())) {
                list.add(entry.getValue());
            }
        }
        return list;
    }


    private static class ChainedConverter extends $.TypeConverter {

        private final $.TypeConverter upstream;
        private final $.TypeConverter downstream;

        public ChainedConverter($.TypeConverter upstream, $.TypeConverter downStream) {
            super(upstream.fromType, downStream.toType);
            this.upstream = upstream;
            this.downstream = downStream;
        }

        @Override
        public Object convert(Object o) {
            return downstream.convert(upstream.convert(o));
        }
    }

    private static boolean isShorterPath($.TypeConverter left, $.TypeConverter right) {
        int leftHops = hops(left), rightHops = hops(right);
        return leftHops < rightHops;
    }

    private static int hops($.TypeConverter typeConverter) {
        if (!(typeConverter instanceof ChainedConverter)) {
            return 1;
        }
        ChainedConverter chainedConverter = $.cast(typeConverter);
        return hops(chainedConverter.upstream) + hops(chainedConverter.downstream);
    }

}