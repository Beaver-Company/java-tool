package org.osgl.util.algo;

import org.osgl.$;
import org.osgl.Lang;
import org.osgl.OsglConfig;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.S;

/**
 * Base class for implementing algorithm that perform replacement on {@link char[]}
 */
public abstract class StringReplace implements $.Func3<char[], char[], char[], char[]> {

    /**
     * Apply the function to replace all target in text with replacement and return a
     * `char[]` contains the result with all target replaced with replacement
     *
     * @param text the text
     * @param target the target to search
     * @param replacement the replacement of target
     * @return the replaced result as described above
     * @throws NotAppliedException
     * @throws Lang.Break
     */
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

    public static StringReplace wrap(final $.Func3<char[], char[], char[], char[]> replaceLogic) {
        return $.requireNotNull(replaceLogic) instanceof StringReplace ? (StringReplace) replaceLogic : new StringReplace() {
            @Override
            protected char[] replace(char[] text, char[] target, char[] replacement) {
                return replaceLogic.apply(text, target, replacement);
            }
        };
    }

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
            S.Buffer buf;
            int textLen = text.length, targetLen = target.length, i = 0, j = 0;
            if (textLen == 0 || targetLen == 0) {
                return text;
            }
            i = searcher.search(text, target, 0);
            if (i < 0) {
                return text;
            }
            buf = S.buffer();
            if (i > j) {
                buf.append(text, j, i - j);
            }
            buf.append(replacement);
            i += targetLen;
            j = i;
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
            if (textLen > j) {
                buf.append(text, j, textLen - j);
            }
            int len = buf.length();
            char[] result = new char[len];
            buf.getChars(0, len, result, 0);
            return result;
        }
    }
}
