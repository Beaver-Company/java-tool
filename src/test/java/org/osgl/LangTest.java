package org.osgl;

import org.junit.Test;
import org.osgl.util.C;
import org.osgl.util.S;
import org.osgl.util.converter.TypeConverterRegistry;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class LangTest extends TestBase {

    @Test
    public void testToString2() {
        String[][] sa = {{"foo", "bar"}, {"1", "2"}};
        eq("[[foo, bar], [1, 2]]", $.asString2(sa));
    }

    @Test
    public void testRandom() {
        C.Range<Integer> r = C.range(10, 100);
        for (int i = 0; i < 100; ++i) {
            int n = $.random(r);
            yes(n >= 10);
            yes(n < 100);
        }
    }

    @Test
    public void testPredicateOr() {
        C.List<String> l = C.List("a.xml", "b.html", "c.txt", "d.txt");
        l = l.filter(S.F.endsWith(".xml").or(S.F.endsWith(".html")));
        yes(l.contains("a.xml"));
        yes(l.contains("b.html"));
        no(l.contains("c.txt"));
        no(l.contains("d.txt"));
    }

    private static class Foo {
        private String f1;
        private static String fs1;
    }

    private static class Bar extends Foo {
        private String f1;
        private int f2;
    }

    @Test
    public void testFieldsOf() {
        List<Field> fields = $.fieldsOf(Bar.class, false);
        eq(4, fields.size());
        eq(4, new HashSet<Field>(fields).size());
        fields = $.fieldsOf(Bar.class, true);
        eq(3, fields.size());
        eq(3, new HashSet<Field>(fields).size());
    }

    enum Code {
        AB, bc, Red;
    }

    @Test
    public void testAsEnum() {
        assertSame(Code.AB, $.asEnum(Code.class, "ab"));
        assertSame(Code.bc, $.asEnum(Code.class, "bc"));
        assertNull($.asEnum(Code.class, "abc"));
        assertNull($.asEnum(Code.class, null));

        assertSame(Code.AB, $.asEnum(Code.class, "AB", true));
        assertNull($.asEnum(Code.class, "ab", true));
    }

    @Test
    public void testConvert() {
        int n = 600;
        String s = "60";
        eq((byte) 600, $.convert(n).to(Byte.class));
        eq((byte) 60, $.convert(s).to(Byte.class));
    }

    @Test
    public void testConvertEnum() {
        eq(Code.AB, $.convert("AB").to(Code.class));
        eq(Code.AB, $.convert("ab").caseInsensitivie().to(Code.class));
    }

    @Test
    public void testConvertNullValue() {
        eq(0, $.convert(null).toInt());
        assertNull($.convert(null).toInteger());
        assertNull($.convert(null).to(Date.class));
    }

    @Test
    public void testConvertNullWithDef() {
        eq(5, $.convert(null).defaultTo(5).toInt());
        eq(2, $.convert("2").defaultTo(5).toInt());
    }

    @Test
    public void testConvertDate() throws Exception {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat();
        Date expected = format.parse(format.format(date)); // calibrate the date
        eq(expected, $.convert(format.format(date)).toDate());

        String pattern = "yyyy-MM-dd";
        format = new SimpleDateFormat(pattern);
        String dateStr = format.format(date);
        expected = format.parse(dateStr); // calibrate the date
        eq(expected, $.convert(dateStr).hint(pattern).toDate());
    }

    public static class MyFrom {
        public String id;

        public MyFrom(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MyFrom myFrom = (MyFrom) o;
            return id != null ? id.equals(myFrom.id) : myFrom.id == null;
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }
    }

    public static class MyTo {
        public String id;

        public MyTo(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MyTo MyTo = (MyTo) o;
            return id != null ? id.equals(MyTo.id) : MyTo.id == null;
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }
    }

    static class MyConverter extends $.TypeConverter<MyFrom, MyTo> {
        @Override
        public MyTo convert(MyFrom myFrom) {
            return new MyTo(myFrom.id);
        }
    }


    @Test
    public void testConvertExtension() {
        TypeConverterRegistry.INSTANCE.register(new MyConverter());
        String id = S.random();
        eq(new MyTo(id), $.convert(new MyFrom(id)).to(MyTo.class));
    }

}
