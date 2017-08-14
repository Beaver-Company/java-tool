package org.osgl.util.algo;

import org.junit.Test;
import org.osgl.$;
import org.osgl.TestBase;

public abstract class StringReplaceTestBase<LOGIC extends StringReplace> extends TestBase {
    private LOGIC replacer;
    public StringReplaceTestBase(LOGIC replacer) {
        this.replacer = $.requireNotNull(replacer);
    }

    protected char[] text;
    protected char[] target;
    protected char[] replacement;

    @Test
    public void inCaseNoTargetFound() {
        text("abcxabcyabcz").target("abcd").replacement("AAAA").verify();
    }

    @Test
    public void matchAtBeginning() {
        text("abcxabcyabcz").target("abc").replacement("AA").verify();
    }

    @Test
    public void matchAtEnding() {
        text("abcxabczyabcz").target("cz").replacement("AA").verify();
    }

    @Test
    public void doubleBytesCharacters() {
        text("你好 osgl 还有 act").target("还有").replacement("and").verify();
    }

    @Test
    public void continousPatternIssue() {
        text("Some aaa to be replaced").target("aa").replacement("b").verify();
    }

    protected StringReplaceTestBase text(String text) {
        this.text = text.toCharArray();
        return this;
    }

    protected StringReplaceTestBase target(String target) {
        this.target = target.toCharArray();
        return this;
    }

    public StringReplaceTestBase replacement(String replacement) {
        this.replacement = replacement.toCharArray();
        return this;
    }

    protected void verify() {
        eq(String.valueOf(text).replace(String.valueOf(target), String.valueOf(replacement)),
                String.valueOf(replacer.replace(text, target, replacement)));
    }

    public static void main(String[] args) {
        System.out.println("aaa".replace("aa", "b"));
    }
}
