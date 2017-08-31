package org.osgl.util;

import static org.osgl.util.S.F.wrapper;

import org.junit.Test;
import org.osgl.$;

import java.io.File;

public class STest extends UtilTestBase {

    @Test
    public void testConstantsDefinition() {
        eq(new String[0], S.EMPTY_ARRAY);
        eq("\u0000", S.HSEP);
        eq("[,;:\\s]+", S.COMMON_SEP_PATTERN.pattern());
        eq(File.separator, S.FILE_SEP);
        eq(File.pathSeparator, S.PATH_SEP);
    }

    @Test
    public void testAppend() {
        eq(S.str("a").append("b"), S.str("ab"));
    }

    @Test
    public void testPrepend() {
        eq(S.str("a").prepend("b"), S.str("ba"));
    }

    @Test
    public void testF_startsWith() {
        String s = "foo.bar";
        yes(S.F.startsWith("foo").apply(s));
        yes(S.F.endsWith("bar").apply(s));
        no(S.F.endsWith("foo").apply(s));
        no(S.F.startsWith("bar").apply(s));
    }

    @Test
    public void testTimes() {
        String s = "abc";
        eq(S.times(s, 0), "");
        eq(S.times(s, 1), s);
        eq(S.times(s, 2), s + s);
        eq(S.join(",", s, 2), s + "," + s);

        Str s0 = Str.of(s);
        eq(s0.times(2).toString(), s + s);
    }

    @Test
    public void testEnsureEndsWith() {
        eq("abc/", S.ensureEndsWith("abc", "/"));
        eq("abc/", S.ensureEndsWith("abc/", "/"));

        eq("abc/", S.ensureEndsWith("abc", '/'));
        eq("abc/", S.ensureEndsWith("abc/", '/'));
    }

    @Test
    public void testEnsureStartsWith() {
        eq("/abc", S.ensureStartsWith("abc", "/"));
        eq("/abc", S.ensureStartsWith("/abc", "/"));

        eq("/abc", S.ensureStartsWith("abc", '/'));
        eq("/abc", S.ensureStartsWith("/abc", '/'));
    }

    @Test
    public void testPathConcat() {
        eq("foo/bar", S.pathConcat("foo", '/', "bar"));
        eq("foo/bar", S.pathConcat("foo/", '/', "bar"));
        eq("foo/bar", S.pathConcat("foo", '/', "/bar"));
        eq("foo/bar", S.pathConcat("foo/", '/', "/bar"));
    }

    @Test
    public void testConcat() {
        eq("ab", S.concat("a", "b"));
        eq("abc", S.concat("a", "b", "c"));
        eq("abcd", S.concat("a", "b", "c", "d"));
        eq("abcde", S.concat("a", "b", "c", "d", "e"));
        eq("abcdef", S.concat("a", "b", "c", "d", "e", "f"));
        eq("abcdefg", S.concat("a", "b", "c", "d", "e", "f", "g"));
        eq("abcdefgh", S.concat("a", "b", "c", "d", "e", "f", "g", "h"));
    }

    @Test
    public void testDos2Unix() {
        String origin = "abc\n\rxyz\n\r";
        eq("abc\nxyz\n", S.dos2unix(origin));
        origin = "abc\nxyz\n";
        eq(origin, S.dos2unix(origin));
    }

    @Test
    public void testUnix2dos() {
        String origin = "abc\nxyz\n";
        eq("abc\n\rxyz\n\r", S.unix2dos(origin));
        origin = "abc\n\rxyz\n\r";
        eq(origin, S.unix2dos(origin));
    }

    @Test
    public void testBinarySplitNormalCase() {
        S.Binary retval = S.binarySplit("abc.123", '.');
        eq("abc", retval._1);
        eq("123", retval._2);
    }

    @Test
    public void testBinarySplitSeparatorNotFound() {
        S.T2 retval = S.binarySplit("abc", '.');
        eq("abc", retval.left());
        eq("", retval.right());
    }

    @Test
    public void testBinarySplitMultipleSeparators() {
        S.Binary retval = S.binarySplit("abc..123", '.');
        eq("abc", retval.left());
        eq(".123", retval.right());

        retval = S.binarySplit("abc.1.23", '.');
        eq("abc", retval.first());
        eq("1.23", retval.last());
    }

    @Test
    public void testBinarySplitSepartorAtBeginning() {
        S.T2 retval = S.binarySplit(".123", '.');
        eq("", retval._1);
        eq("123", retval._2);
    }

    @Test
    public void testBinarySplitSeparatorAtEnding() {
        S.T2 retval = S.binarySplit("abc.", '.');
        eq("abc", retval._1);
        eq("", retval.second());
    }


    @Test
    public void testTripleSplitNormalCase() {
        S.T3 retval = S.tripleSplit("abc.123.xyz", '.');
        eq("abc", retval._1);
        eq("123", retval._2);
        eq("xyz", retval.third());
    }

    @Test
    public void testTripleSplitNoEnoughSeparators() {
        S.T3 retval = S.tripleSplit("abc", '.');
        eq("abc", retval.first());
        eq("", retval.second());
        eq("", retval.last());

        retval = S.tripleSplit("abc.xyz", '.');
        eq("abc", retval.first());
        eq("xyz", retval.second());
        eq("", retval.last());
    }

    @Test
    public void testTripleSplitMultipleSeparators() {
        S.T3 retval = S.tripleSplit("abc..123", '.');
        eq("abc", retval.first());
        eq("", retval.second());
        eq("123", retval.last());

        retval = S.tripleSplit("abc.1.1.23", '.');
        eq("abc", retval._1);
        eq("1", retval._2);
        eq("1.23", retval._3);
    }

    @Test
    public void testFastSplitNormalCase() {
        S.List result = S.fastSplit("abc.123.xyz", ".");
        eq(result, C.List("abc", "123", "xyz"));

        result = S.fastSplit("abc..123..xyz", "..");
        eq(result, C.List("abc", "123", "xyz"));
    }

    @Test
    public void testFastSplitTrimSeparator() {
        S.List result = S.fastSplit(".abc..123...xyz.", ".");
        eq(result, C.List("abc", "123", "xyz"));
    }

    @Test
    public void testFastSplitNoSeparator() {
        eq(C.List("abc"), S.fastSplit("abc", "."));
    }


    @Test
    public void testCharSplitNormalCase() {
        S.List result = S.split("abc.123.xyz", '.');
        eq(result, C.List("abc", "123", "xyz"));

        result = S.split("abc..123..xyz", '.');
        eq(result, C.List("abc", "123", "xyz"));
    }

    @Test
    public void testCharSplitTrimSeparator() {
        S.List result = S.split(".abc..123...xyz.", '.');
        eq(result, C.List("abc", "123", "xyz"));
    }

    @Test
    public void testCharSplitNoSeparator() {
        eq(C.List("abc"), S.split("abc", '.'));
    }

    @Test
    public void testWrap() {
        String content = S.random();
        String expected = "[" + content + "]";
        eq(expected, S.wrap(content).with(S.SQUARE_BRACKETS));
        eq(expected, S.wrap(content).with("[", "]"));

        expected = "'" + content + "'";
        eq(expected, S.wrap(content).with("'"));
    }

    @Test
    public void testStrip() {
        String content = S.random();
        String s = "[" + content + "]";
        eq(content, S.strip(s).of(S.SQUARE_BRACKETS));
        eq(content, S.strip(s).of(S.pair("[", "]")));
        eq(content, S.strip(s).of("[", "]"));

        s = "/" + content + "/";
        eq(content, S.strip(s).of("/"));
    }

    @Test
    public void testIs() {
        no(S.is("abc").blank());
        yes(S.is(null).empty());
        yes(S.is("").empty());
        no(S.is(" ").empty());
        yes(S.is(" ").blank());
        yes(S.is("[abc]").startWith("["));
        no(S.is("[abc]").endWith(".json"));
        yes(S.is("[abc]").wrappedWith("[", "]"));
        yes(S.is("[abc]").wrappedWith(S.SQUARE_BRACKETS));
    }

    @Test
    public void testEnsure() {
        eq("[abc]", S.ensure("abc").wrappedWith(S.SQUARE_BRACKETS));
        eq("(xyz)", S.ensure("xyz").wrappedWith(S.PARENTHESES));
        eq("xyz", S.ensure("[xyz]").strippedOff(S.BRACKETS));
        eq("(xyz)", S.ensure("xyz").wrappedWith($.Pair("(", ")")));
        eq("|abc|", S.ensure("abc").wrappedWith("|"));
        eq("[abc]", S.ensure("[abc").wrappedWith(S.SQUARE_BRACKETS));
        eq("(xyz)", S.ensure("xyz)").wrappedWith(S.PARENTHESES));
        eq("|abc|", S.ensure("|abc|").wrappedWith("|"));
        eq("abc", S.ensure("|abc|").strippedOff("|"));
        eq("abc.json", S.ensure("abc").endWith(".json"));
    }

    /**
     * This only test the simple case for S replace API
     *
     * The more sophisticated replace test cases can be found in
     * {@link org.osgl.util.algo.StringReplaceTestBase}
     */
    @Test
    public void testReplace() {
        final String text = "I am a good person";

        eq("I am a good man",
                text.replace("person", "man"));
        eq("I am a good man",
                S.have(text).replace("person").with("man"));
        eq("I am a good man",
                S.replace("person").in(text).with("man"));

        eq("I am a **good** person",
                text.replace("good", "**good**"));
        eq("I am a **good** person",
                S.have(text).replace("good").with(wrapper("**")));
        eq("I am a **good** person",
                S.wrap("good").in(text).with("**"));

        eq("I am a [good] person",
                S.have(text).replace("good").with(wrapper(S.BRACKETS)));
        eq("I am a [good] person",
                S.wrap("good").in(text).with(S.BRACKETS));
    }
}
