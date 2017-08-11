package org.osgl.util;

import org.osgl.$;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.EnumSet;

public abstract class SetBase<T> extends AbstractSet<T> implements C.Set<T> {

    // --- Featured methods

    volatile private EnumSet<C.Feature> features_;

    protected final EnumSet<C.Feature> features_() {
        if (null == features_) {
            synchronized (this) {
                if (null == features_) {
                    features_ = initFeatures();
                    assert(null != features_);
                }
            }
        }
        return features_;
    }

    /**
     * Sub class should override this method to provide initial feature
     * set for the feature based instance
     *
     * @return the initial feature set configuration
     */
    abstract protected EnumSet<C.Feature> initFeatures();

    @Override
    public final EnumSet<C.Feature> features() {
        return EnumSet.copyOf(features_());
    }

    @Override
    public final boolean is(C.Feature feature) {
        return features_().contains(feature);
    }

    protected SetBase<T> setFeature(C.Feature feature) {
        features_().add(feature);
        return this;
    }

    protected SetBase<T> unsetFeature(C.Feature feature) {
        features_().remove(feature);
        return this;
    }

    @Override
    public C.Set<T> parallel() {
        setFeature(C.Feature.PARALLEL);
        return this;
    }

    @Override
    public C.Set<T> sequential() {
        unsetFeature(C.Feature.PARALLEL);
        return this;
    }

    @Override
    public C.Set<T> lazy() {
        setFeature(C.Feature.LAZY);
        return this;
    }

    @Override
    public C.Set<T> eager() {
        unsetFeature(C.Feature.LAZY);
        return this;
    }

    // --- eof Featured methods

    // --- Traversal methods

    @Override
    public boolean allMatch($.Function<? super T, Boolean> predicate) {
        return C.allMatch(this, predicate);
    }

    @Override
    public boolean anyMatch($.Function<? super T, Boolean> predicate) {
        return C.anyMatch(this, predicate);
    }

    @Override
    public boolean noneMatch($.Function<? super T, Boolean> predicate) {
        return C.noneMatch(this, predicate);
    }

    @Override
    public $.Option<T> findOne(final $.Function<? super T, Boolean> predicate) {
        return C.findOne(this, predicate);
    }

    /**
     * Sub class could override this method to implement iterating in parallel.
     *
     * <p>The iterating support partial function visitor by ignoring the
     * {@link org.osgl.exception.NotAppliedException} thrown out by visitor's apply
     * method call</p>
     *
     * @param visitor the visitor
     * @throws $.Break if visitor needs to terminate the iteration
     */
    public SetBase<T> forEach($.Visitor<? super T> visitor) throws $.Break {
        C.forEach(this, visitor);
        return this;
    }

    @Override
    public SetBase<T> each($.Visitor<? super T> visitor) {
        return forEach(visitor);
    }

    @Override
    public C.Set<T> filter($.Function<? super T, Boolean> predicate) {
        boolean mutable = isMutable();
        int sz = size();
        if (0 == sz) {
            return mutable ? C.Mutable.<T>Set() : C.<T>Set();
        }
        if (mutable) {
            C.Set<T> set = C.Mutable.Set();
            forEach($.visitor($.predicate(predicate).ifThen(C.F.addTo(set))));
            return set;
        } else {
            ListBuilder<T> lb = new ListBuilder<>(sz);
            forEach($.visitor($.predicate(predicate).ifThen(C.F.addTo(lb))));
            return lb.toSet();
        }
    }

    @Override
    public SetBase<T> accept($.Visitor<? super T> visitor) {
        return forEach(visitor);
    }

    @Override
    public <R> C.Sequence<R> map($.Function<? super T, ? extends R> mapper) {
        return C.transform(this, mapper);
    }

    @Override
    public <R> C.Sequence<R> flatMap($.Function<? super T, ? extends Iterable<? extends R>> mapper) {
        return C.flatMap(this, mapper);
    }

    @Override
    public <R> C.Sequence<R> extract(final String property) {
        return C.extract(this, property);
    }

    @Override
    public <R> R reduce(R identity, $.Func2<R, T, R> accumulator) {
        return C.reduce(iterator(), identity, accumulator);
    }

    @Override
    public $.Option<T> reduce($.Func2<T, T, T> accumulator) {
        return C.reduce(iterator(), accumulator);
    }

    // --- eof Traversal methods

     // utilities
    protected final boolean isLazy() {
        return is(C.Feature.LAZY);
    }

    protected final boolean isImmutable() {
        return is(C.Feature.IMMUTABLE);
    }

    protected final boolean isReadOnly() {
        return is(C.Feature.READONLY);
    }

    protected final boolean isMutable() {
        return !isImmutable() && !isReadOnly();
    }


    @Override
    public C.Set<T> onlyIn(Collection<? extends T> col) {
        C.Set<T> others = C.Mutable.Set(col);
        others.removeAll(this);
        if (isImmutable()) {
            return ImmutableSet.of(others);
        }
        return others;
    }

    @Override
    public C.Set<T> complement(Collection<? extends T> col) {
        return onlyIn(col);
    }

    @Override
    public C.Set<T> withIn(Collection<? extends T> col) {
        C.Set<T> others = C.Mutable.Set(col);
        others.retainAll(this);
        if (isImmutable()) {
            return ImmutableSet.of(others);
        }
        return others;
    }

    @Override
    public C.Set<T> intersection(Collection<? extends T> col) {
        return withIn(col);
    }

    @Override
    public C.Set<T> without(Collection<? super T> col) {
        C.Set<T> copy = C.Mutable.Set(this);
        copy.removeAll(col);
        if (isImmutable()) {
            return ImmutableSet.of(copy);
        }
        return copy;
    }

    @Override
    public C.Set<T> difference(Collection<? super T> col) {
        return without(col);
    }

    @Override
    public C.Set<T> with(Collection<? extends T> col) {
        C.Set<T> copy = C.Mutable.Set(this);
        copy.addAll(col);
        if (isImmutable()) {
            return ImmutableSet.of(copy);
        }
        return copy;
    }

    @Override
    public C.Set<T> with(T element) {
        if (this.isMutable()) {
            this.add(element);
            return this;
        }
        C.Set<T> copy = C.Mutable.Set(this);
        copy.add(element);
        return C.Set(copy);
    }

    @Override
    public C.Set<T> with(T... elements) {
        if (this.isMutable()) {
            this.addAll(C.List(elements));
            return this;
        }
        C.Set<T> copy = C.Mutable.Set(this);
        copy.addAll(C.List(elements));
        return C.Set(copy);
    }

    @Override
    public C.Set<T> without(T element) {
        if (!contains(element)) {
            return this;
        }
        if (this.isMutable()) {
            this.remove(element);
            return this;
        }
        C.Set<T> copy = C.Mutable.Set(this);
        copy.remove(element);
        return C.Set(copy);
    }

    @Override
    public C.Set<T> without(T... elements) {
        if (elements.length == 0) {
            return this;
        }
        if (this.isMutable()) {
            this.removeAll(C.List(elements));
            return this;
        }
        C.Set<T> copy = C.Mutable.Set(this);
        copy.removeAll(C.List(elements));
        return C.Set(copy);
    }




}
