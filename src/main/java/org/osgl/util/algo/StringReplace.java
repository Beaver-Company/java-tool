package org.osgl.util.algo;

import org.osgl.$;
import org.osgl.Lang;
import org.osgl.OsglConfig;
import org.osgl.exception.NotAppliedException;

/**
 * Base class for implementing algorithm that perform replacement on {@link char[]}
 */
public abstract class StringReplace implements  $.Func3<char[], char[], char[], char[]> {

    @Override
    public final char[] apply(char[] text, char[] target, char[] replacement) throws NotAppliedException, Lang.Break {
        return replace(text, target, replacement);
    }

    /**
     * Sub class shall implement the replacement logic in this method
     * @param text the text in which search string will be replaced
     * @param target the target string to be replaced
     * @param replacement the replacement string
     * @return result of the replacement
     */
    protected abstract char[] replace(char[] text, char[] target, char[] replacement);

    public static class SimpleStringReplace extends StringReplace {

        private final StringSearch searcher;

        public SimpleStringReplace(StringSearch searcher) {
            this.searcher = $.requireNotNull(searcher);
        }

        public SimpleStringReplace() {
            this(OsglConfig.DEF_STRING_SEARCH);
        }

        @Override
        protected char[] replace(char[] text, char[] target, char[] replacement) {
            StringSearch searcher = this.searcher;
            StringBuilder buf = new StringBuilder();
            int textLen = text.length, targetLen = target.length, i = 0, j = 0;
            if (textLen == 0 || targetLen == 0) {
                return text;
            }
            do {
                i = searcher.search(text, target, i);
                if (i < 0) {
                    break;
                }
                if (i > j) {
                    buf.append(text, j, i - j);
                }
                buf.append(replacement);
                i += targetLen;
                j = i;
            } while (true);
            if (0 == j) {
                return text;
            }
            if (textLen > j) {
                buf.append(text, j, textLen - j);
            }
            int bufLen = buf.length();
            char[] retval = new char[bufLen];
            buf.getChars(0, bufLen, retval, 0);
            return retval;
        }
    }
}
