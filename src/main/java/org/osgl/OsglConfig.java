package org.osgl;

import org.osgl.util.algo.StringReplace;
import org.osgl.util.algo.StringSearch;

/**
 * Make OSGL tool configurable
 */
public class OsglConfig {

    // String manipulation

    /**
     * Default string search logic
     */
    public static StringSearch DEF_STRING_SEARCH = new StringSearch.SimpleStringSearch();

    /**
     * Default string replace logic
     */
    public static StringReplace DEF_STRING_REPLACE = new StringReplace.SimpleStringReplace();

}
