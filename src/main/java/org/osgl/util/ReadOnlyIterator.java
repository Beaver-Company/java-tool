package org.osgl.util;

abstract class ReadOnlyIterator<T> extends OsglIteratorBase<T> {
    @Override
    public final void remove() {
        throw new UnsupportedOperationException();
    }
}
