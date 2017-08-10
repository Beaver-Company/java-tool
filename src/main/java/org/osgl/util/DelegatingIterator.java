package org.osgl.util;

import java.util.Iterator;

public class DelegatingIterator<T> extends OsglIteratorBase<T> implements Iterator<T> {
    private final Iterator<? extends T> itr_;
    protected final boolean readOnly;

    DelegatingIterator(Iterator<? extends T> itr, boolean readOnly) {
        this.itr_ = itr;
        this.readOnly = readOnly;
    }

    protected Iterator<? extends T> itr() {
        return itr_;
    }

    protected final void mutableOperation() {
        if (readOnly) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean hasNext() {
        return itr_.hasNext();
    }

    @Override
    public T next() {
        return itr_.next();
    }

    @Override
    public void remove() {
        mutableOperation();
        itr_.remove();
    }

    public static <T> Iterator<T> of(Iterator<? extends T> iterator, boolean readOnly) {
        return new DelegatingIterator<T>(iterator, readOnly);
    }
}
