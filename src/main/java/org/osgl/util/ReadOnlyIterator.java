package org.osgl.util;

abstract class ReadOnlyIterator<T> extends IteratorBase<T> {
    @Override
    public final void remove() {
        throw new UnsupportedOperationException();
    }
}
