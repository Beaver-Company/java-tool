package org.osgl.util.algo;

import org.junit.Test;
import org.osgl.$;
import org.osgl.TestBase;

public abstract class StringSearchTestBase<SEARCH extends StringSearch> extends TestBase {

    private SEARCH logic;

    public StringSearchTestBase(SEARCH logic) {
        this.logic = $.requireNotNull(logic);
    }

    protected char[] text;
    protected char[] target;
    protected int from;

    @Test
    public void targetIsLongerThanText() {
        text("abc").target("abcd").verify();
    }

    @Test
    public void noMatch() {
        text("abcde").target("acd").verify();
    }

    @Test
    public void matchAtBeginning() {
        text("abcde").target("abc").verify();
    }

    @Test
    public void matchAtEnd() {
        text("abcde").target("de").verify();
    }

    @Test
    public void matchAtMiddle() {
        text("abcde").target("cd").verify();
    }

    @Test
    public void noMatchWithFromSpecified() {
        text("abcde").target("bcd").from(2).verify();
    }

    @Test
    public void matchAtFrom() {
        text("abcde").target("bcd").from(1).verify();
    }

    @Test
    public void matchAtMiddleWithFromSpecified() {
        text("abcde").target("cd").from(1).verify();
    }

    @Test
    public void doubleByteCharSearch() {
        text("你好 osgl 还有 act").target("还有").verify();
        text("你好 osgl 还有 act").target("osgl").verify();
        text("你好 osgl 还有 act").target("还有").from(3).verify();
    }

    protected final StringSearchTestBase text(String text) {
        this.text = text.toCharArray();
        return this;
    }

    protected final StringSearchTestBase target(String target) {
        this.target = target.toCharArray();
        return this;
    }

    protected final StringSearchTestBase from(int from) {
        this.from = from;
        return this;
    }

    protected int doJob() {
        return logic.search(text, target, from);
    }

    private void verify() {
        int id = doJob();
        int expected = String.valueOf(text).indexOf(String.valueOf(target), from);
        if (id < 0 & expected < 0) {
            return;
        }
        eq(expected, id);
    }
}
