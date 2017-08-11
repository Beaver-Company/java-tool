package org.osgl.util;

import java.io.Serializable;
import java.util.*;

class DelegatingSet<T> extends SetBase<T> implements Serializable {

    protected Set<T> data;

    DelegatingSet(Collection<? extends T> c) {
        data = new HashSet<>(c);
    }

    DelegatingSet() {
        data = new HashSet<T>();
    }

    DelegatingSet(Collection<? extends T> c, boolean immutable) {
        if (c instanceof C.Set) {
            C.Set<T> set = (C.Set<T>) c;
            boolean setIsImmutable = set.is(C.Feature.IMMUTABLE);
            if (immutable && setIsImmutable) {
                data = set;
            } else {
                if (immutable) {
                    data = Collections.unmodifiableSet(set);
                } else {
                    data = new HashSet<T>(set);
                }
            }
        } else if (c instanceof java.util.Set) {
            Set<? extends T> set = (Set<? extends T>)c;
            if (immutable) {
                data = Collections.unmodifiableSet(set);
            } else {
                data = new HashSet<T>(set);
            }
        } else {
            Set<T> set = new HashSet<T>(c);
            if (immutable) {
                data = Collections.unmodifiableSet(set);
            } else {
                data = set;
            }
        }
    }

    @Override
    protected EnumSet<C.Feature> initFeatures() {
        EnumSet<C.Feature> fs = (data instanceof C.Set) ?
                ((C.Set<T>)data).features()
                : EnumSet.of(C.Feature.LIMITED);
        return fs;
    }

    @Override
    public Iterator<T> iterator() {
        return data.iterator();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean contains(Object o) {
        return data.contains(o);
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public Object[] toArray() {
        return data.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return data.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return data.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return data.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return data.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return data.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return data.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return data.removeAll(c);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public String toString() {
        return data.toString();
    }

    @Override
    public int hashCode() {
        return data.hashCode() + DelegatingSet.class.hashCode();
    }

}
