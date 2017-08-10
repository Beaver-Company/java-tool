package org.osgl.util;

import org.osgl.$;

import java.util.Enumeration;
import java.util.Iterator;

class EnumerationIterator<T> extends OsglIteratorBase<T> implements Iterator<T> {
    private Enumeration<? extends T> e;

    EnumerationIterator(Enumeration<? extends T> enumeration) {
        e = $.ensureNotNull(enumeration);
    }

    @Override
    public boolean hasNext() {
        return e.hasMoreElements();
    }

    @Override
    public T next() {
        return e.nextElement();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
