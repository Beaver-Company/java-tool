package org.osgl.util.algo;

import org.osgl.$;
import org.osgl.Lang;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.S;

public abstract class StringSearch implements $.Func3<char[], char[], Integer, Integer> {
    /**
     * Search a char array in another char array with from index specified
     *
     * @param text the char array to be searched
     * @param target the pattern to be searched across the text
     * @param from the from index it shall be non-negative number. If from index is negative number then
     *             it starts from `0`; if `from` is greater than or equals to the text length then `-1`
     *             will be returned
     * @return index of the search pattern in the text. If the search pattern is found
     *         in the text region specified then it returns it's index in the text; otherwise
     *         an negative number, typically `-1` is returned
     */
    protected abstract int search(char[] text, char[] target, int from);

    @Override
    public final Integer apply(char[] text, char[] target, Integer from) throws NotAppliedException, Lang.Break {
        return search(text, target, from);
    }

    /**
     * Implement a simple string search algorithm
     */
    public static class SimpleStringSearch extends StringSearch {
        @Override
        protected int search(char[] text, char[] target, int from) {
            if (from < 0) {
                from = 0;
            }
            int txtLen = text.length, targetLen = target.length;
            if (txtLen - from < targetLen) {
                return -1;
            }
            int id = S.indexOf(text, 0, txtLen, target, 0, targetLen, from);
            return id < 0 ? id : id;
        }
    }
}
