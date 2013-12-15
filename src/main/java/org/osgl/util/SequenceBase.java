package org.osgl.util;

import org.osgl._;

import java.util.EnumSet;
import java.util.NoSuchElementException;

/**
 * Provide default implementation to some {@link C.Sequence} interface.
 *
 * <p>The most of the method of this implementation is lazy without
 * regarding to the {@link C.Feature#LAZY} setting of this
 * sequence instance</p>
 */
public abstract class SequenceBase<T>
extends TraversableBase<T> implements C.Sequence<T> {

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

    protected final boolean isLimited() {
        return is(C.Feature.LIMITED);
    }

    @Override
    public C.Sequence<T> lazy() {
        return (C.Sequence<T>)super.lazy();
    }

    @Override
    public C.Sequence<T> eager() {
        return (C.Sequence<T>)super.eager();
    }

    @Override
    public C.Sequence<T> parallel() {
        return (C.Sequence<T>)super.parallel();
    }

    @Override
    public C.Sequence<T> sequential() {
        return (C.Sequence<T>)super.sequential();
    }

    @Override
    protected EnumSet<C.Feature> initFeatures() {
        return EnumSet.of(C.Feature.LAZY, C.Feature.READONLY);
    }

    @Override
    public C.Sequence<T> accept(_.Function<? super T, ?> visitor) {
        forEach(visitor);
        return this;
    }

    protected void forEachLeft(_.Function<? super T, ?> visitor) {
        forEach(visitor);
    }

    @Override
    public T first() throws NoSuchElementException {
        return iterator().next();
    }

    @Override
    public final T head() throws NoSuchElementException {
        return first();
    }

    @Override
    @SuppressWarnings("unchecked")
    public C.Sequence<T> acceptLeft(_.Function<? super T, ?> visitor) {
        forEachLeft(visitor);
        return this;
    }

    /**
     * Delegate to {@link TraversableBase#reduce(Object, org.osgl._.Func2)}
     * @param identity {@inheritDoc}
     * @param accumulator {@inheritDoc}
     * @param <R> {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public <R> R reduceLeft(R identity, _.Func2<R, T, R> accumulator) {
        return reduce(identity, accumulator);
    }

    /**
     * Delegate to {@link TraversableBase#reduce(org.osgl._.Func2)}
     * @param accumulator {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public _.Option<T> reduceLeft(_.Func2<T, T, T> accumulator) {
        return reduce(accumulator);
    }

    /**
     * Delegate to {@link TraversableBase#findOne(org.osgl._.Function)}
     * @param predicate the function map the element to Boolean
     * @return {@inheritDoc}
     */
    @Override
    public _.Option<T> findFirst(_.Function<? super T, Boolean> predicate) {
        return findOne(predicate);
    }

    @Override
    public C.Sequence<T> head(int n) {
        if (n == 0) {
            return Nil.seq();
        } else if (n < 0) {
            if (isLimited()) {
                return drop(size() + n);
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            if (isLimited() && n >= size()) {
                return this;
            }
            return IndexFilteredSeq.of(this, _.F.lessThan(n));
        }
    }

    @Override
    public C.Sequence<T> tail() throws UnsupportedOperationException {
        return IndexFilteredSeq.of(this, _.F.greaterThan(0));
    }

    @Override
    public C.Sequence<T> take(int n) {
        return head(n);
    }

    @Override
    public C.Sequence<T> takeWhile(_.Function<? super T, Boolean> predicate) {
        return FilteredSeq.of(this, predicate, FilteredIterator.Type.WHILE);
    }

    @Override
    public C.Sequence<T> drop(int n) throws IllegalArgumentException {
        if (n < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (n == 0) {
            return this;
        }
        return IndexFilteredSeq.of(this, _.F.gte(n));
    }

    @Override
    public C.Sequence<T> dropWhile(_.Function<? super T, Boolean> predicate) {
        return FilteredSeq.of(this, _.F.negate(predicate), FilteredIterator.Type.UNTIL);
    }

    @Override
    public C.Sequence<T> append(Iterable<? extends T> iterable) {
        return append(C.seq(iterable));
    }

    @Override
    public C.Sequence<T> append(C.Sequence<T> seq) {
        if (seq.isEmpty()) {
            return this;
        }
        return CompositeSeq.of(this, seq);
    }

    @Override
    public C.Sequence<T> append(T t) {
        return CompositeSeq.of(this, _.val(t));
    }

    @Override
    public C.Sequence<T> prepend(Iterable<? extends T> iterable) {
        if (!iterable.iterator().hasNext()) {
            return this;
        }
        return prepend(C.seq(iterable));
    }

    @Override
    public C.Sequence<T> prepend(C.Sequence<T> seq) {
        if (seq.isEmpty()) {
            return this;
        }
        return seq.append(this);
    }

    @Override
    public C.Sequence<T> prepend(T t) {
        return CompositeSeq.of(C.singletonList(t), this);
    }

    @Override
    public C.Sequence<T> filter(_.Function<? super T, Boolean> predicate) {
        return FilteredSeq.of(this, predicate);
    }

    @Override
    public <R> C.Sequence<R> map(_.Function<? super T, ? extends R> mapper) {
        return MappedSeq.of(this, mapper);
    }

    @Override
    public <R> C.Sequence<R> flatMap(_.Function<? super T, ? extends Iterable<? extends R>> mapper
    ) {
        return FlatMappedSeq.of(this, mapper);
    }

    @Override
    public <T2> C.Sequence<_.T2<T, T2>> zip(Iterable<T2> iterable) {
        return new ZippedSeq<T, T2>(this, iterable);
    }

    @Override
    public <T2> C.Sequence<_.T2<T, T2>> zipAll(Iterable<T2> iterable, T def1, T2 def2) {
        return new ZippedSeq<T, T2>(this, iterable, def1, def2);
    }

    @Override
    public C.Sequence<_.T2<T, Integer>> zipWithIndex() {
        return new ZippedSeq<T, Integer>(this, new IndexIterable(this));
    }
}