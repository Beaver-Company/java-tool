package org.osgl;

import org.junit.Test;
import org.osgl.cache.CacheService;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.C;
import org.osgl.util.N;
import org.osgl.util.S;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class PropertyTest extends TestBase {

    public static enum Color {
        R, G, B
    }

    public static class Bar {
        String s;
        private boolean b;
        private Map<Color, String> colors = C.Mutable.Map(Color.R, "red", Color.G, "green");
        Bar() {}
        Bar(String s, boolean b) {
            this.s = s;
            this.b = b;
        }

        public String s() {
            return s;
        }
        public boolean isB() {
            return b;
        }
    }

    public static class FooBase {
        private String baseId;
        public FooBase(String baseId) {
            this.baseId = baseId;
        }
        public String getBaseId() {
            return baseId;
        }
    }

    public static class Foo extends FooBase {
        private String s;
        private int i;
        private Bar bar;
        private List<Bar> barList;
        Foo() {
            super(S.random());
        }
        Foo(String s1, int i, String s2, boolean b) {
            super(s1);
            s = s1;
            this.i = i;
            bar = new Bar(s2, b);
        }
        public String getS() {
            return s;
        }
        public void addBar(Bar bar) {
            if (barList == null) {
                barList = C.Mutable.List();
            }
            barList.add(bar);
        }
    }

    @Test
    public void testGetProperty() {
        String s1 = S.random();
        String s2 = S.random();
        int i = N.randInt();
        boolean b = false;
        Foo foo = new Foo(s1, i, s2, b);
        eq(s1, $.getProperty(foo, "s"));
        eq(s2, $.getProperty(foo, "bar.s"));
        eq(s1, $.getProperty(foo, "baseId"));
        eq(i, $.getProperty(foo, "i"));
        eq(false, $.getProperty(foo, "bar/b"));
        eq(false, $.getProperty(foo, "bar.b"));
        eq(null, $.getProperty(foo, "barList"));
        Bar bar = new Bar("bar", true);
        foo.addBar(bar);
        eq(bar, ((List)$.getProperty(foo, "barList")).get(0));
        eq(bar.s(), $.getProperty(foo, "barList.0.s"));
        eq("red", $.getProperty(foo, "barList[0][colors][R]"));
    }

    @Test
    public void testSetProperty() {
        Foo foo = new Foo();
        String s1 = S.random();
        $.setProperty(foo, s1, "s");
        eq(s1, foo.getS());
        String s2 = S.random();
        $.setProperty(foo, s2, "bar.s");
        eq(s2, foo.bar.s);
        boolean b = !foo.bar.b;
        $.setProperty(foo, b, "bar.b");
        eq(b, foo.bar.b);
        String s3 = S.random();
        $.setProperty(foo, s3, "barList[][s]");
        eq(s3, foo.barList.get(0).s());
    }

    @Test
    public void testGetPropertyWithCache() {
        final C.Map<String, Serializable> map = C.Mutable.Map();
        Lang.F1<String, Serializable> getter = new Lang.F1<String, Serializable>() {
            @Override
            public Serializable apply(String s) throws NotAppliedException, Lang.Break {
                return map.get(s);
            }
        };
        Lang.F2<String, Serializable, Object> setter = new Lang.F2<String, Serializable, Object>() {
            @Override
            public Object apply(String s, Serializable serializable) throws NotAppliedException, Lang.Break {
                map.put(s, serializable);
                return null;
            }
        };
        CacheService cache = new CacheService() {
            private Map<String, Object> map = C.Mutable.Map();
            @Override
            public void put(String key, Object value, int ttl) {
                map.put(key, value);
            }

            @Override
            public void put(String key, Object value) {
                map.put(key, value);
            }

            @Override
            public void evict(String key) {
                map.remove(key);
            }

            @Override
            public <T> T get(String key) {
                return (T) map.get(key);
            }

            @Override
            public void clear() {
                map.clear();
            }

            @Override
            public void setDefaultTTL(int ttl) {

            }

            @Override
            public void shutdown() {
                clear();
            }

            @Override
            public void startup() {

            }
        };

        String s1 = S.random();
        String s2 = S.random();
        int i = N.randInt();
        boolean b = false;
        Foo foo = new Foo(s1, i, s2, b);

        eq(s1, $.getProperty(cache, foo, "s"));
        eq(s2, $.getProperty(cache, foo, "bar.s"));
        eq(i, $.getProperty(cache, foo, "i"));
        eq(false, $.getProperty(cache, foo, "bar/b"));

        eq(s1, $.getProperty(cache, foo, "s"));
        eq(s2, $.getProperty(cache, foo, "bar.s"));
        eq(i, $.getProperty(cache, foo, "i"));
        eq(false, $.getProperty(cache, foo, "bar/b"));
    }

}
