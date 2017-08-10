package org.osgl.util;

import java.lang.reflect.Array;
import java.util.Iterator;

class RefectionArrayIterator<T> extends OsglIteratorBase<T> implements Iterator<T> {
    private final Object array;
    private int currentIndex = 0;

    RefectionArrayIterator(Object array) {
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("not an array");
        } else {
            this.array = array;
        }
    }

    public boolean hasNext() {
        return this.currentIndex < Array.getLength(this.array);
    }

    public T next() {
        return (T) Array.get(this.array, this.currentIndex++);
    }

    public void remove() {
        throw new UnsupportedOperationException("cannot remove items from an array");
    }

}
