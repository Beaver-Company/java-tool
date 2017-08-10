package org.osgl.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SingletonIterator<T> extends OsglIteratorBase<T> implements Iterator<T> {
    private final T t_;
    private volatile boolean consumed_;

    public SingletonIterator(T t) {
        t_ = t;
    }

    @Override
    public boolean hasNext() {
        return !consumed_;
    }

    @Override
    public T next() {
        if (consumed_) {
            throw new NoSuchElementException();
        }
        consumed_ = true;
        return t_;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
