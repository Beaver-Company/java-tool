package org.osgl.util;

import java.util.Iterator;

public abstract class OsglIteratorBase<T> implements Iterator<T> {
    public final Iterable<T> toIterable() {
        return C.toIterable(this);
    }
}
