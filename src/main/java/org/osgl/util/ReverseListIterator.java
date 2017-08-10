package org.osgl.util;

import java.util.ListIterator;

class ReverseListIterator<T> extends OsglIteratorBase<T> implements ListIterator<T> {
    private ListIterator<T> itr;
    ReverseListIterator(ListIterator<T> itr) {
        this.itr = itr;
    }

    @Override
    public boolean hasNext() {
        return itr.hasPrevious();
    }

    @Override
    public T next() {
        return itr.previous();
    }

    @Override
    public boolean hasPrevious() {
        return itr.hasNext();
    }

    @Override
    public T previous() {
        return itr.next();
    }

    @Override
    public int nextIndex() {
        return itr.previousIndex();
    }

    @Override
    public int previousIndex() {
        return itr.nextIndex();
    }

    @Override
    public void remove() {
        itr.remove();
    }

    @Override
    public void set(T t) {
        itr.set(t);
    }

    @Override
    public void add(T t) {
        //TODO fix me
        itr.add(t);
    }
}
