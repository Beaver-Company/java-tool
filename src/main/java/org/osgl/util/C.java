/*
 * Copyright (C) 2013 The Java Tool project
 * Gelin Luo <greenlaw110(at)gmail.com>
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.osgl.util;

import org.osgl.$;
import org.osgl.Lang;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.algo.Algorithms;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * The namespace for OSGL collection utilities
 */
public class C {

    C() {
    }

    /**
     * The character enum for a data structure
     */
    public enum Feature {
        /**
         * Indicate whether a structure is immutable
         */
        IMMUTABLE,

        /**
         * Indicate the client cannot modify the structure.
         * However a readonly structure might not be immutable.
         * For example a view of a backing structure is readonly
         * while the backing structure is immutable
         */
        READONLY,

        /**
         * Indicate whether a list structure support random access
         */
        RANDOM_ACCESS,

        /**
         * Indicate whether the structure is limited
         */
        LIMITED,

        /**
         * Indicate whether the structure supports lazy evaluation
         */
        LAZY,

        /**
         * Indicate whether the structure support parallel operation
         */
        PARALLEL,

        /**
         * Indicate whether this structure is ordered. E.g. a
         * {@link List} and {@link java.util.LinkedHashSet} is ordered
         * structure, while {@link java.util.HashSet} might not be
         * ordered
         */
        ORDERED,

        /**
         * Indicate whether this structure is sorted
         */
        SORTED
    }

    /**
     * Define a type that holds a set of {@link org.osgl.util.C.Feature}
     */
    public interface Featured {
        /**
         * Get all characteristics in {@link EnumSet}
         *
         * @return an {@code EnumSet} of all characteristics hold by this object
         * @since 0.2
         */
        EnumSet<Feature> features();

        /**
         * Check if this object has a certain {@link org.osgl.util.C.Feature}
         *
         * @param c the characteristic to be tested
         * @return {@code true} if this object has the characteristic, or {@code false} otherwise
         * @since 0.2
         */
        boolean is(Feature c);

        @SuppressWarnings("unused")
        class Factory {
            public static Featured identity(final EnumSet<Feature> predefined) {
                return new Featured() {
                    @Override
                    public EnumSet<Feature> features() {
                        return predefined;
                    }

                    @Override
                    public boolean is(Feature c) {
                        return predefined.contains(c);
                    }
                };
            }
        }
    }

    /**
     * Define a traversable structure with functional programming support,
     * including Map, reduce etc.
     *
     * @param <T> The element type
     */
    public interface Traversable<T> extends Iterable<T>, Featured {

        /**
         * Returns this traversable and try to turn on
         * {@link C.Feature#PARALLEL}. If this traversable does not
         * support {@link org.osgl.util.C.Feature#PARALLEL} then
         * return this traversable directly without any state change
         *
         * @return this reference with parallel turned on if parallel
         * is supported
         */
        Traversable<T> parallel();

        /**
         * Returns this traversable and turn off
         * {@link C.Feature#PARALLEL}
         *
         * @return this reference with parallel turned off
         */
        Traversable<T> sequential();

        /**
         * Returns this traversable and try to turn on {@link C.Feature#LAZY}.
         * If lazy is not supported then return this traversable directly without
         * any state change
         *
         * @return this reference with lazy turned on if it is supported
         */
        Traversable<T> lazy();

        /**
         * Returns this traversable and turn off {@link C.Feature#LAZY}
         *
         * @return this reference with lazy turned off
         */
        Traversable<T> eager();

        /**
         * Is this traversal empty?
         *
         * @return {@code true} if the traversal is empty or {@code false} otherwise
         * @since 0.2
         */
        boolean isEmpty();

        /**
         * Return the size of this traversal
         *
         * @return the size of the structure
         * @throws UnsupportedOperationException if this structure does not support this method
         * @since 0.2
         */
        int size() throws UnsupportedOperationException;


        /**
         * Returns an new traversable with a mapper function specified. The element in the new traversal is the result of the
         * mapper function applied to this traversal element.
         * <pre>
         *     Traversable traversable = C.list(23, _.NONE, null);
         *     assertEquals(C.list(true, false, false), traversal.Map(_.F.NOT_NULL));
         *     assertEquals(C.list("23", "", ""), traversal.Map(_.F.AS_STRING));
         * </pre>
         * <p>For Lazy Traversable, it must use lazy evaluation for this method.
         * Otherwise it is up to implementation to decide whether use lazy
         * evaluation or not</p>
         *
         * @param mapper the function that applied to element in this traversal and returns element in the result traversal
         * @param <R>    the element type of the new traversal
         * @return the new traversal contains results of the mapper function applied to this traversal
         * @since 0.2
         */
        <R> Traversable<R> map($.Function<? super T, ? extends R> mapper);

        /**
         * Returns a traversable consisting of the results of replacing each element of this
         * stream with the contents of the iterable produced by applying the provided mapping
         * function to each element. If the result of the mapping function is {@code null},
         * this is treated as if the result is an empty traversable.
         *
         * @param mapper the function produce an iterable when applied to an element
         * @param <R>    the element type of the the new traversable
         * @return the new traversable
         * @since 0.2
         */
        <R> Traversable<R> flatMap($.Function<? super T, ? extends Iterable<? extends R>> mapper);

        /**
         * Returns an new traversable that contains all elements in the current traversable
         * except that does not pass the test of the filter function specified.
         * <pre>
         *     Traversable traversable = C.list(-1, 0, 1, -3, 7);
         *     Traversable filtered = traversable.filter(_.F.gt(0));
         *     assertTrue(filtered.contains(1));
         *     assertFalse(filtered.contains(-3));
         * </pre>
         *
         * @param predicate the function that test if the element in the traversable should be
         *                  kept in the resulting traversable. When applying the filter function
         *                  to the element, if the result is {@code true} then the element will
         *                  be kept in the resulting traversable.
         * @return the new traversable consists of elements passed the filter function test
         * @since 0.2
         */
        Traversable<T> filter($.Function<? super T, Boolean> predicate);

        /**
         * Performs a reduction on the elements in this traversable, using the provided
         * identity and accumulating function. This might be equivalent to:
         * <pre>
         *      R result = identity;
         *      for (T element: this traversable) {
         *          result = accumulator.apply(result, element);
         *      }
         *      return result;
         * </pre>
         * <p>The above shows a typical left side reduce. However depending on the
         * implementation, it might choose another way to do the reduction, including
         * reduction in a parallel way</p>
         *
         * @param identity    the identity value for the accumulating function
         * @param accumulator the function the combine two values
         * @param <R>         the type of identity and the return value
         * @return the result of reduction
         * @since 0.2
         */
        <R> R reduce(R identity, $.Func2<R, T, R> accumulator);

        /**
         * Performs a reduction on the elements in this traversable, using provided accumulating
         * function. This might be equivalent to:
         * <pre>
         *      boolean found = false;
         *      T result = null;
         *      for (T element: this traversable) {
         *          if (found) {
         *              result = accumulator.apply(result, element);
         *          } else {
         *              found = true;
         *              result = element;
         *          }
         *      }
         *      return found ? _.some(result) : _.none();
         * </pre>
         * <p>The above shows a typical left side reduction. However depending on the
         * implementation, it might choose another way to do the reduction, including
         * reduction in a parallel way</p>
         *
         * @param accumulator the function takes previous accumulating
         *                    result and the current element being
         *                    iterated
         * @return an option describing the accumulating result or {@link $#none()} if
         * the structure is empty
         * @since 0.2
         */
        $.Option<T> reduce($.Func2<T, T, T> accumulator);

        /**
         * Check if all elements match the predicate specified
         *
         * @param predicate the function to test the element
         * @return {@code true} if all elements match the predicate
         * @since 0.2
         */
        boolean allMatch($.Function<? super T, Boolean> predicate);

        /**
         * Check if any elements matches the predicate specified
         *
         * @param predicate the function to test the element
         * @return {@code true} if any element matches the predicate
         * @since 0.2
         */
        boolean anyMatch($.Function<? super T, Boolean> predicate);

        /**
         * Check if no elements matches the predicate specified. This should be
         * equivalent to:
         * <pre>
         *      this.allMatch(_.F.negate(predicate));
         * </pre>
         *
         * @param predicate the function to test the element
         * @return {@code true} if none element matches the predicate
         * @since 0.2
         */
        boolean noneMatch($.Function<? super T, Boolean> predicate);

        /**
         * Returns an element that matches the predicate specified. The interface
         * does not indicate if it should be the first element matches the predicate
         * be returned or in case of parallel computing, whatever element matches
         * found first is returned. It's all up to the implementation to refine the
         * semantic of this method
         *
         * @param predicate the function Map element to Boolean
         * @return an element in this traversal that matches the predicate or
         * {@link $#NONE} if no element matches
         * @since 0.2
         */
        $.Option<T> findOne($.Function<? super T, Boolean> predicate);

        /**
         * Iterate this {@code Traversable} with a visitor function. This method
         * does not specify the approach to iterate through this structure. The
         * implementation might choose iterate from left to right, or vice versa.
         * It might even choose to split the structure into multiple parts, and
         * iterate through them in parallel
         *
         * @param visitor a function that apply to element in this
         *                {@code Traversable}. The return value
         *                of the function is ignored
         * @return this {@code Traversable} instance for chained call
         * @since 0.2
         */
        Traversable<T> accept($.Visitor<? super T> visitor);

        /**
         * Alias of {@link #accept(Lang.Visitor)}
         *
         * @param visitor the visitor to tranverse the elements
         * @return this {@code Traversable} instance
         */
        Traversable<T> each($.Visitor<? super T> visitor);

        /**
         * Alias of {@link #accept(Lang.Visitor)}
         *
         * @param visitor the visitor function
         * @return this {@code Traversable} instance
         */
        Traversable<T> forEach($.Visitor<? super T> visitor);
    }

    public interface Sequence<T>
            extends Traversable<T> {

        /**
         * Returns this traversable and make sure {@link C.Feature#PARALLEL} is set
         *
         * @return this reference with parallel turned on
         */
        Sequence<T> parallel();

        /**
         * Returns this traversable and make sure {@link C.Feature#PARALLEL} is unset
         *
         * @return this reference with parallel turned off
         */
        Sequence<T> sequential();

        /**
         * Returns this traversable and make sure {@link C.Feature#LAZY} is set
         *
         * @return this reference with lazy turned on
         */
        Sequence<T> lazy();

        /**
         * Returns this traversable and make sure {@link C.Feature#LAZY} is unset
         *
         * @return this reference with lazy turned off
         */
        Sequence<T> eager();

        /**
         * Alias of {@link #head()}
         *
         * @return the first element in the sequence
         * @since 0.2
         */
        T first() throws NoSuchElementException;

        /**
         * Returns an {@link $.Option} of the first element in the
         * {@code Sequence} or {@link $#NONE} if the {@code Sequence} is empty
         *
         * @return the first element from the {@code Sequence}
         * @throws NoSuchElementException if the {@code Sequence} is empty
         * @see #tail()
         * @see #first()
         * @since 0.2
         */
        T head() throws NoSuchElementException;

        /**
         * Alias of {@link #take(int)}
         *
         * @param n the number of elements to be taken into the return sequence
         * @return the first {@code n} element in the sequence
         * @since 0.2
         */
        Sequence<T> head(int n);

        /**
         * Returns the rest part of the {@code Sequence} except the first element
         *
         * @return a {@code Sequence} without the first element
         * @throws UnsupportedOperationException if the {@code Sequence} is empty
         * @see #head()
         * @see ReversibleSequence#tail(int)
         * @since 0.2
         */
        Sequence<T> tail() throws UnsupportedOperationException;

        /**
         * Returns a {@code Sequence} consisting the first {@code n} elements from this {@code Sequence} if
         * number {@code n} is positive and the {@code Sequence} contains more than {@code n} elements
         * <p>If this {@code Sequence} contains less than {@code n} elements, then a {@code Sequence} consisting
         * the whole elements of this {@code Sequence} is returned. Note it might return this {@code Sequence}
         * itself if the {@code Sequence} is immutable.</p>
         * <p>If the number {@code n} is zero, then an empty {@code Sequence} is returned in reverse
         * order</p>
         * <p>If the number {@code n} is negative, then the last {@code -n} elements from this
         * {@code Sequence} is returned in an new {@code Sequence}, or throw {@link UnsupportedOperationException}
         * if this operation is not supported</p>
         * <pre>
         *     Sequence seq = C.list(1, 2, 3, 4);
         *     assertEquals(C.list(1, 2), seq.take(2));
         *     assertEquals(C.list(1, 2, 3, 4), seq.take(100));
         *     assertEquals(C.list(), seq.take(0));
         *     assertEquals(C.list(3, 4), seq.take(-2));
         *     assertEquals(C.list(1, 2, 3, 4), seq.take(-200));
         * </pre>
         *
         * @param n specify the number of elements to be taken from the head of this {@code Sequence}
         * @return a {@code Sequence} consisting of the first {@code n} elements of this {@code Sequence}
         * @see #head(int)
         * @since 0.2
         */
        Sequence<T> take(int n);

        /**
         * Returns an new {@code Sequence} that takes the head of this {@code Sequence} until the predicate
         * evaluate to {@code false}:
         * <pre>
         *     C.Sequence seq = C.list(1, 2, 3, 4, 5, 4, 3, 2, 1);
         *     assertEquals(C.list(C.list(1, 2, 3), seq.takeWhile(_.F.lt(4)));
         *     assertEquals(C.list(C.list(1, 2, 3, 3, 2, 1), seq.filter(_.F.lt(4)));
         * </pre>
         *
         * @param predicate specify which the elements in this {@code Sequence} will put into the new
         *                  {@code Sequence}
         * @return the new {@code Sequence}
         * @since 0.2
         */
        Sequence<T> takeWhile($.Function<? super T, Boolean> predicate);

        /**
         * Returns a {@code Sequence} consisting of the elements from this {@code Sequence} except the first {@code n}
         * if number {@code n} is positive and the {@code Sequence} contains more than {@code n} elements
         * <p>If this {@code Sequence} contains less than {@code n} elements, then an empty {@code Sequence}
         * is returned</p>
         * <p>If the number {@code n} is zero, then a copy of this {@code Sequence} or this {@code Sequence}
         * itself is returned depending on the implementation</p>
         * <p>If the number {@code n} is negative, then either {@link IllegalArgumentException} should
         * be thrown out if this sequence is not {@link org.osgl.util.C.Feature#LIMITED} or it drop
         * {@code -n} element starts from the tail side</p>
         * <pre>
         *     C.Sequence seq = C.list(1, 2, 3, 4, 5);
         *     assertEquals(C.list(3, 4, 5), seq.drop(2));
         *     assertEquals(C.list(1, 2, 3, 4, 5), seq.drop(0));
         *     assertEquals(C.list(), seq.drop(100));
         * </pre>
         * <p>Note this method does NOT modify the current sequence, instead it returns an new sequence structure
         * containing the elements as required</p>
         *
         * @param n specify the number of elements to be taken from the head of this {@code Sequence}
         *          must not less than 0
         * @return a {@code Sequence} consisting of the elements of this {@code Sequence} except the first {@code n} ones
         * @since 0.2
         */
        Sequence<T> drop(int n) throws IllegalArgumentException;

        /**
         * Returns a {@code Sequence} consisting of the elements from this sequence with leading elements
         * dropped until the predicate returns {@code true}
         * <pre>
         *      Sequence seq = C.list(1, 2, 3, 4, 3, 2, 1);
         *      assertTrue(C.list(), seq.dropWhile(_.F.gt(100)));
         *      assertTrue(C.list(4, 3, 2, 1), seq.dropWhile(_.F.lt(3)));
         * </pre>
         * <p>Note this method does NOT modify the current sequence, instead it returns an new sequence structure
         * containing the elements as required</p>
         *
         * @param predicate the function that check if drop operation should stop
         * @return the sequence after applying the drop operations
         * @since 0.2
         */
        Sequence<T> dropWhile($.Function<? super T, Boolean> predicate);

        /**
         * Returns a sequence consists of all elements of this sequence
         * followed by all elements of the specified iterable.
         * <p>An {@link C.Feature#IMMUTABLE immutable} Sequence must
         * return an new Sequence; while a mutable Sequence implementation
         * might append specified seq to {@code this} sequence instance
         * directly</p>
         *
         * @param iterable the iterable in which elements will be append to this sequence
         * @return the sequence after append the iterable
         */
        Sequence<T> append(Iterable<? extends T> iterable);

        /**
         * Returns a sequence consists of all elements of this sequence
         * followed by all elements of the specified sequence.
         * <p>An {@link C.Feature#IMMUTABLE immutable} Sequence must
         * return an new Sequence; while a mutable Sequence implementation
         * might append specified seq to {@code this} sequence instance
         * directly</p>
         *
         * @param seq the sequence to be appended
         * @return a sequence consists of elements of both sequences
         * @since 0.2
         */
        Sequence<T> append(Sequence<? extends T> seq);

        /**
         * Returns a sequence consists of all elements of this sequence
         * followed by all elements of the specified iterator.
         * <p>An {@link C.Feature#IMMUTABLE immutable} Sequence must
         * return an new Sequence; while a mutable Sequence implementation
         * might append specified seq to {@code this} sequence instance
         * directly</p>
         *
         * @param iterator the iterator in which elements will be append to the returned sequence
         * @return a sequence consists of elements of this sequence and the elements in the iterator
         * @since 0.9
         */
        Sequence<T> append(Iterator<? extends T> iterator);

        /**
         * Returns a sequence consists of all elements of this sequence
         * followed by all elements of the specified enumeration.
         * <p>An {@link C.Feature#IMMUTABLE immutable} Sequence must
         * return an new Sequence; while a mutable Sequence implementation
         * might append specified seq to {@code this} sequence instance
         * directly</p>
         *
         * @param enumeration the enumeration in which elements will be append to the returned sequence
         * @return a sequence consists of elements of this sequence and the elements in the iterator
         * @since 0.9
         */
        Sequence<T> append(Enumeration<? extends T> enumeration);

        /**
         * Returns a sequence consists of all elements of this sequence
         * followed by the element specified.
         * <p>an {@link C.Feature#IMMUTABLE immutable} Sequence must
         * return an new Sequence; while a mutable Sequence implementation
         * might append the element to {@code this} sequence instance
         * directly</p>
         *
         * @param t the element to be appended to this sequence
         * @return a sequence consists of elements of this sequence
         * and the element {@code t}
         * @since 0.2
         */
        Sequence<T> append(T t);

        /**
         * Returns a sequence consists of all elements of the iterable specified
         * followed by all elements of this sequence
         * <p>An {@link C.Feature#IMMUTABLE immutable} Sequence must
         * return an new Sequence; while a mutable Sequence implementation
         * might prepend specified seq to {@code this} sequence instance
         * directly</p>
         *
         * @param iterable the iterable to be prepended
         * @return a sequence consists of elements of both sequences
         * @since 0.2
         */
        Sequence<T> prepend(Iterable<? extends T> iterable);

        /**
         * Returns a sequence consists of all elements of the iterator specified
         * followed by all elements of this sequence
         * <p>An {@link C.Feature#IMMUTABLE immutable} Sequence must
         * return an new Sequence; while a mutable Sequence implementation
         * might prepend specified seq to {@code this} sequence instance
         * directly</p>
         *
         * @param iterator the iterator to be prepended
         * @return a sequence consists of elements of both sequences
         * @since 0.2
         */
        Sequence<T> prepend(Iterator<? extends T> iterator);

        /**
         * Returns a sequence consists of all elements of the enumeration specified
         * followed by all elements of this sequence
         * <p>An {@link C.Feature#IMMUTABLE immutable} Sequence must
         * return an new Sequence; while a mutable Sequence implementation
         * might prepend specified seq to {@code this} sequence instance
         * directly</p>
         *
         * @param enumeration the enumeration to be prepended
         * @return a sequence consists of elements of both sequences
         * @since 0.2
         */
        Sequence<T> prepend(Enumeration<? extends T> enumeration);

        /**
         * Returns a sequence consists of all elements of the sequence specified
         * followed by all elements of this sequence
         * <p>An {@link C.Feature#IMMUTABLE immutable} Sequence must
         * return an new Sequence; while a mutable Sequence implementation
         * might prepend specified seq to {@code this} sequence instance
         * directly</p>
         *
         * @param seq the sequence to be prepended
         * @return a sequence consists of elements of both sequences
         * @since 0.2
         */
        Sequence<T> prepend(Sequence<? extends T> seq);

        /**
         * Returns a sequence consists of the element specified followed by
         * all elements of this sequence.
         * <p>an {@link C.Feature#IMMUTABLE immutable} Sequence must
         * return an new Sequence; while a mutable Sequence implementation
         * might append the element to {@code this} sequence instance
         * directly</p>
         *
         * @param t the element to be appended to this sequence
         * @return the sequence consists of {@code t} followed
         * by all elements in this sequence
         * @since 0.2
         */
        Sequence<T> prepend(T t);


        /**
         * {@inheritDoc}
         *
         * @param mapper {@inheritDoc}
         * @param <R>    {@inheritDoc}
         * @return a Sequence of {@code R} that are mapped from this sequence
         * @since 0.2
         */
        @Override
        <R> Sequence<R> map($.Function<? super T, ? extends R> mapper);

        /**
         * {@inheritDoc}
         *
         * @param mapper {@inheritDoc}
         * @param <R>    {@inheritDoc}
         * @return a Sequence of {@code R} type element that are mapped from this sequences
         * @since 0.2
         */
        @Override
        <R> Sequence<R> flatMap($.Function<? super T, ? extends Iterable<? extends R>> mapper);

        /**
         * {@inheritDoc}
         *
         * @param predicate {@inheritDoc}
         * @return An new {@code Sequence} consists of elements that passed the predicate
         * @since 0.2
         */
        @Override
        Sequence<T> filter(final $.Function<? super T, Boolean> predicate);

        /**
         * {@inheritDoc}
         * This method does not specify how to run the accumulator. It might be
         * {@link C.Sequence#reduceLeft(Object, Lang.Func2)} or
         * {@link ReversibleSequence#reduceRight(Object, Lang.Func2)}, or
         * even run reduction in parallel, it all depending on the implementation.
         * <p>For a guaranteed reduce from left to right, use
         * {@link C.Sequence#reduceLeft(Object, Lang.Func2)}  instead</p>
         *
         * @param identity    {@inheritDoc}
         * @param accumulator {@inheritDoc}
         * @param <R>         {@inheritDoc}
         * @return {@inheritDoc}
         * @since 0.2
         */
        @Override
        <R> R reduce(R identity, $.Func2<R, T, R> accumulator);

        /**
         * Run reduction from header side. This is equivalent to:
         * <pre>
         *      R result = identity;
         *      for (T element: this sequence) {
         *          result = accumulator.apply(result, element);
         *      }
         *      return result;
         * </pre>
         *
         * @param identity    the identity value for the accumulating function
         * @param accumulator the function to accumulate two values
         * @param <R>         the aggregation result type
         * @return the reduced result
         * @since 0.2
         */
        <R> R reduceLeft(R identity, $.Func2<R, T, R> accumulator);

        /**
         * {@inheritDoc}
         * This method does not specify the approach to run reduction.
         * For a guaranteed reduction from head to tail, use
         * {@link #reduceLeft(Lang.Func2)} instead
         *
         * @param accumulator {@inheritDoc}
         * @return {@inheritDoc}
         * @since 0.2
         */
        @Override
        $.Option<T> reduce($.Func2<T, T, T> accumulator);

        /**
         * Run reduction from head to tail. This is equivalent to
         * <pre>
         *      if (isEmpty()) {
         *          return _.none();
         *      }
         *      T result = head();
         *      for (T element: this traversable.tail()) {
         *          result = accumulator.apply(result, element);
         *      }
         *      return _.some(result);
         * </pre>
         *
         * @param accumulator the function accumulate each element to the final result
         * @return an {@link $.Option} describing the accumulating result
         * @since 0.2
         */
        $.Option<T> reduceLeft($.Func2<T, T, T> accumulator);

        /**
         * Apply the predicate specified to the element of this sequence
         * from head to tail. Stop at the element that returns {@code true},
         * and returns an {@link $.Option} describing the element. If none
         * of the element applications in the sequence returns {@code true}
         * then {@link $#none()} is returned
         *
         * @param predicate the function Map the element to Boolean
         * @return an option describe the first element matches the
         * predicate or {@link $#none()}
         * @since 0.2
         */
        $.Option<T> findFirst($.Function<? super T, Boolean> predicate);

        Sequence<T> accept($.Visitor<? super T> visitor);

        Sequence<T> each($.Visitor<? super T> visitor);

        Sequence<T> forEach($.Visitor<? super T> visitor);

        /**
         * Iterate through this sequence from head to tail with
         * the visitor function specified
         *
         * @param visitor the function to visit elements in this sequence
         * @return this sequence
         * @see Traversable#accept(Lang.Visitor)
         * @see ReversibleSequence#acceptRight(Lang.Visitor)
         * @since 0.2
         */
        Sequence<T> acceptLeft($.Visitor<? super T> visitor);

        /**
         * Returns a sequence formed from this sequence and another iterable
         * collection by combining corresponding elements in pairs.
         * If one of the two collections is longer than the other,
         * its remaining elements are ignored.
         *
         * @param iterable the part B to be zipped with this sequence
         * @param <T2>     the type of the iterable
         * @return a new sequence containing pairs consisting of
         * corresponding elements of this sequence and that.
         * The length of the returned collection is the
         * minimum of the lengths of this sequence and that.
         */
        <T2> Sequence<? extends $.Binary<T, T2>> zip(Iterable<T2> iterable);

        /**
         * Returns a sequence formed from this sequence and another iterable
         * collection by combining corresponding elements in pairs.
         * If one of the two collections is longer than the other,
         * placeholder elements are used to extend the shorter collection
         * to the length of the longer.
         *
         * @param iterable the part B to be zipped with this sequence
         * @param <T2>     the type of the iterable
         * @param def1     the element to be used to fill up the result if
         *                 this sequence is shorter than that iterable
         * @param def2     the element to be used to fill up the result if
         *                 the iterable is shorter than this sequence
         * @return a new sequence containing pairs consisting of
         * corresponding elements of this sequence and that.
         * The length of the returned collection is the
         * maximum of the lengths of this sequence and that.
         */
        <T2> Sequence<? extends $.Binary<T, T2>> zipAll(Iterable<T2> iterable, T def1, T2 def2);

        /**
         * Zip this sequence with its indices
         *
         * @return A new list containing pairs consisting of all
         * elements of this list paired with their index.
         * Indices start at 0.
         */
        @SuppressWarnings("unused")
        Sequence<? extends $.Binary<T, Integer>> zipWithIndex();

        /**
         * Count the element occurence in this sequence
         *
         * @param t the element
         * @return the number of times the element be presented in this sequence
         */
        int count(T t);
    }

    /**
     * A bidirectional sequence which can be iterated from tail to head
     *
     * @param <T> the element type
     */
    public interface ReversibleSequence<T>
            extends Sequence<T> {

        /**
         * Returns this traversable and make sure {@link C.Feature#PARALLEL} is set
         *
         * @return this reference with parallel turned on
         */
        ReversibleSequence<T> parallel();

        /**
         * Returns this traversable and make sure {@link C.Feature#PARALLEL} is unset
         *
         * @return this reference with parallel turned off
         */
        ReversibleSequence<T> sequential();

        /**
         * Returns this traversable and make sure {@link C.Feature#LAZY} is set
         *
         * @return this reference with lazy turned on
         */
        ReversibleSequence<T> lazy();

        /**
         * Returns this traversable and make sure {@link C.Feature#LAZY} is unset
         *
         * @return this reference with lazy turned off
         */
        ReversibleSequence<T> eager();

        /**
         * {@inheritDoc}
         *
         * @param n {@inheritDoc}
         * @return an new reversible sequence contains the first
         * {@code n} elements in this sequence
         */
        @Override
        ReversibleSequence<T> head(int n);

        /**
         * {@inheritDoc}
         *
         * @return an new reversible sequence contains all elements
         * in this sequence except the first element
         */
        @Override
        ReversibleSequence<T> tail();

        /**
         * {@inheritDoc}
         *
         * @param n {@inheritDoc}
         * @return an new reversible sequence contains the first
         * {@code n} elements in this sequence
         */
        @Override
        ReversibleSequence<T> take(int n);

        /**
         * {@inheritDoc}
         *
         * @param predicate {@inheritDoc}
         * @return an new reversible sequence contains the elements
         * in this sequence until predicate evaluate to false
         */
        @Override
        ReversibleSequence<T> takeWhile($.Function<? super T, Boolean> predicate);

        /**
         * {@inheritDoc}
         *
         * @param n specify the number of elements to be taken from the head of this {@code Sequence} or
         *          the {@code -n} number of elements to be taken from the tail of this sequence if n is
         *          an negative number
         * @return a reversible sequence without the first {@code n} number of elements
         */
        @Override
        ReversibleSequence<T> drop(int n);

        @Override
        ReversibleSequence<T> dropWhile($.Function<? super T, Boolean> predicate);

        @Override
        ReversibleSequence<T> filter($.Function<? super T, Boolean> predicate);

        /**
         * {@inheritDoc}
         *
         * @param t {@inheritDoc}
         * @return a reversible sequence contains this seq's element
         * followed by {@code t}
         */
        @Override
        ReversibleSequence<T> append(T t);

        /**
         * Returns an new reversible sequence contains all elements
         * in this sequence followed by all elements in the specified
         * reverse sequence
         *
         * @param seq another reversible sequence
         * @return an new reversible sequence contains both seq's elements
         */
        ReversibleSequence<T> append(ReversibleSequence<T> seq);

        /**
         * {@inheritDoc}
         *
         * @param t {@inheritDoc}
         * @return a reversible sequence contains by {@code t}
         * followed this seq's element
         */
        @Override
        ReversibleSequence<T> prepend(T t);

        /**
         * Returns an new reversible sequence contains all elements
         * in specified reversible sequence followed by all elements
         * in this sequence
         *
         * @param seq another reversible sequence
         * @return an new reversible sequence contains both seq's elements
         */
        ReversibleSequence<T> prepend(ReversibleSequence<T> seq);

        /**
         * Returns the last element from this {@code Sequence}
         *
         * @return the last element
         * @throws UnsupportedOperationException if this {@code Sequence} is not limited
         * @throws NoSuchElementException        if the {@code Sequence} is empty
         * @see #isEmpty()
         * @see org.osgl.util.C.Feature#LIMITED
         * @see #is(org.osgl.util.C.Feature)
         * @since 0.2
         */
        T last() throws UnsupportedOperationException, NoSuchElementException;


        /**
         * Returns a {@code Sequence} consisting the last {@code n} elements from this {@code Sequence}
         * if number {@code n} is positive and the {@code Sequence} contains more than {@code n} elements
         * <p>If this {@code Sequence} contains less than {@code n} elements, then a {@code Sequence} consisting
         * the whole elements of this {@code Sequence} is returned. Note it might return this {@code Sequence}
         * itself if the {@code Sequence} is immutable.</p>
         * <p>If the number {@code n} is zero, then an empty {@code Sequence} is returned in reverse
         * order</p>
         * <p>If the number {@code n} is negative, then the first {@code -n} elements from this
         * {@code Sequence} is returned in an new {@code Sequence}</p>
         * <pre>
         *     Sequence seq = C1.list(1, 2, 3, 4);
         *     assertEquals(C1.list(3, 4), seq.tail(2));
         *     assertEquals(C1.list(1, 2, 3, 4), seq.tail(100));
         *     assertEquals(C1.list(), seq.tail(0));
         *     assertEquals(C1.list(1, 2, 3), seq.tail(-3));
         *     assertEquals(C1.list(1, 2, 3, 4), seq.tail(-200));
         * </pre>
         * <p>This method does not mutate the underline container</p>
         *
         * @param n specify the number of elements to be taken from the tail of this {@code Sequence}
         * @return a {@code Sequence} consisting of the last {@code n} elements from this {@code Sequence}
         * @throws UnsupportedOperationException if the traversal is unlimited or empty
         * @throws IndexOutOfBoundsException     if {@code n} is greater than the size of this {@code Sequence}
         * @see org.osgl.util.C.Feature#LIMITED
         * @see #is(org.osgl.util.C.Feature)
         * @since 0.2
         */
        ReversibleSequence<T> tail(int n) throws UnsupportedOperationException, IndexOutOfBoundsException;

        /**
         * Returns an new {@code Sequence} that reverse this {@code Sequence}.
         *
         * @return a reversed {@code Sequence}
         * @throws UnsupportedOperationException if this {@code Sequence} is unlimited
         * @see org.osgl.util.C.Feature#LIMITED
         * @see #is(org.osgl.util.C.Feature)
         * @since 0.2
         */
        ReversibleSequence<T> reverse() throws UnsupportedOperationException;

        /**
         * Returns an {@link Iterator} iterate the sequence from tail to head
         *
         * @return the iterator
         * @since 0.2
         */
        Iterator<T> reverseIterator();

        /**
         * Run reduction from tail side. This is equivalent to:
         * <pre>
         *      R result = identity;
         *      for (T element: this sequence.reverse()) {
         *          result = accumulator.apply(result, element);
         *      }
         *      return result;
         * </pre>
         *
         * @param identity    the initial value
         * @param accumulator the function performs accumulation from {@code T} an {@code R} to anthoer {@code R}
         * @param <R>         the accumulation result
         * @return the aggregation result
         * @see #reduce(Object, Lang.Func2)
         * @since 0.2
         */
        <R> R reduceRight(R identity, $.Func2<R, T, R> accumulator);

        /**
         * Run reduction from tail to head. This is equivalent to
         * <pre>
         *      if (isEmpty()) {
         *          return _.none();
         *      }
         *      T result = last();
         *      for (T element: this sequence.reverse.tail()) {
         *          result = accumulator.apply(result, element);
         *      }
         *      return _.some(result);
         * </pre>
         *
         * @param accumulator the function accumulate each element to the final result
         * @return an {@link $.Option} describing the accumulating result
         * @since 0.2
         */
        $.Option<T> reduceRight($.Func2<T, T, T> accumulator);


        /**
         * Apply the predicate specified to the element of this sequence
         * from tail to head. Stop at the element that returns {@code true},
         * and returns an {@link $.Option} describing the element. If none
         * of the element applications in the sequence returns {@code true}
         * then {@link $#none()} is returned
         *
         * @param predicate the function Map the element to Boolean
         * @return an option describe the first element matches the
         * predicate or {@link $#none()}
         * @since 0.2
         */
        $.Option<T> findLast($.Function<? super T, Boolean> predicate);

        @Override
        <R> ReversibleSequence<R> map($.Function<? super T, ? extends R> mapper);

        /**
         * {@inheritDoc}
         *
         * @param mapper {@inheritDoc}
         * @param <R>    {@inheritDoc}
         * @return a ReversibleSequence of {@code R} type element that are mapped from this sequences
         * @since 0.2
         */
        @Override
        <R> ReversibleSequence<R> flatMap($.Function<? super T, ? extends Iterable<? extends R>> mapper);

        ReversibleSequence<T> accept($.Visitor<? super T> visitor);


        ReversibleSequence<T> each($.Visitor<? super T> visitor);

        ReversibleSequence<T> forEach($.Visitor<? super T> visitor);

        ReversibleSequence<T> acceptLeft($.Visitor<? super T> visitor);

        /**
         * Iterate through this sequence from tail to head with the visitor function
         * specified
         *
         * @param visitor the function to visit elements in this sequence
         * @return this sequence
         * @see Traversable#accept(Lang.Visitor)
         * @see Sequence#acceptLeft(Lang.Visitor)
         * @since 0.2
         */
        ReversibleSequence<T> acceptRight($.Visitor<? super T> visitor);

        <T2> C.ReversibleSequence<$.Binary<T, T2>> zip(C.ReversibleSequence<T2> rseq);

        @SuppressWarnings("unused")
        <T2> C.ReversibleSequence<$.Binary<T, T2>> zipAll(C.ReversibleSequence<T2> rseq, T def1, T2 def2);

    }

    public static class Array<T> extends ReversibleSeqBase<T> implements ReversibleSequence<T> {
        @Override
        public Array<T> lazy() {
            super.lazy();
            return this;
        }

        @Override
        public Array<T> eager() {
            super.eager();
            return this;
        }

        @Override
        public Array<T> parallel() {
            super.parallel();
            return this;
        }

        @Override
        public Array<T> sequential() {
            super.sequential();
            return this;
        }

        T[] data;

        Array(T[] data) {
            E.NPE(data);
            this.data = data;
        }

        @Override
        public int size() throws UnsupportedOperationException {
            return data.length;
        }

        public boolean isEmpty() {
            return 0 == size();
        }

        public boolean isNotEmpty() {
            return 0 < size();
        }

        public T get(int idx) {
            return data[idx];
        }

        public Array<T> set(int idx, T val) {
            data[idx] = val;
            return this;
        }

        @Override
        public Iterator<T> iterator() {
            final int size = size();
            return new ReadOnlyIterator<T>() {
                int cursor = 0;

                @Override
                public boolean hasNext() {
                    return cursor < size;
                }

                @Override
                public T next() {
                    if (cursor >= size) {
                        throw new NoSuchElementException();
                    }
                    return data[cursor++];
                }
            };
        }

        @Override
        public Iterator<T> reverseIterator() {
            final int size = size();
            return new ReadOnlyIterator<T>() {
                int cursor = size - 1;

                @Override
                public boolean hasNext() {
                    return cursor < 0;
                }

                @Override
                public T next() {
                    if (cursor < 0) {
                        throw new NoSuchElementException();
                    }
                    return data[cursor--];
                }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        public ReversibleSequence<T> reverse() throws UnsupportedOperationException {
            if (isLazy()) {
                return ReversedRSeq.of(this);
            }
            if (isMutable()) {
                Algorithms.arrayReverseInplace().reverse(data, 0, data.length);
                return this;
            }
            T[] newData = (T[]) Algorithms.ARRAY_REVERSE.apply(data, 0, data.length);
            return of(newData);
        }

        public C.List<T> asList() {
            return List(data);
        }

        public C.List<T> asMutableList() {
            return Mutable.List(data);
        }

        public static <T> Array<T> of(T[] data) {
            return new Array<>(data);
        }

        public static <T> Array<T> copyOf(T[] data) {
            int len = data.length;
            T[] newData = $.newArray(data, len);
            System.arraycopy(data, 0, newData, 0, len);
            return new Array<>(newData);
        }

        public static <T> RefectionArrayIterator<T> toIterator(Object array) {
            return new RefectionArrayIterator(array);
        }

    }

    /**
     * Define a Range data structure which contains a discrete sequence of elements start from {@link #from()}
     * until {@link #to()}. The {@code from} element should be contained in the range, while the {@code to}
     * element should be exclusive from the range. While the {@code from} and {@code to} defines the boundary of
     * an range, the {@link #step()} defines how to step from one element to another in the range.
     *
     * @param <ELEMENT> the element type
     */
    public interface Range<ELEMENT> extends Sequence<ELEMENT> {
        /**
         * Returns the {@code from} value (inclusive) in the range
         *
         * @return {@code from}
         * @since 0.2
         */
        ELEMENT from();

        /**
         * Returns the {@code to} value (exclusive) of the range
         *
         * @return {@code to}
         * @since 0.2
         */
        ELEMENT to();

        /**
         * Check if an element is contained in this range
         *
         * @param element the element to be checked
         * @return {@code true} if the element specified is contained in the range
         * @since 0.2
         */
        boolean contains(ELEMENT element);

        /**
         * Check if this range contains all elements of another range of the same type (identified by
         * {@link #order()} and {@link #step()}).
         *
         * @param r2 the range to be tested
         * @return {@code true} if this range contains all elements of {@code r2}
         * @since 0.2
         */
        boolean containsAll(Range<ELEMENT> r2);

        /**
         * Returns a {@link $.Func2} function that takes two elements in the range domain and returns an integer to
         * determine the order of the two elements. See {@link java.util.Comparator#compare(Object, Object)} for
         * semantic of the function.
         * <p>If any one of the element applied is {@code null} the function should throw out
         * {@link NullPointerException}</p>
         *
         * @return a function implement the ordering logic
         * @since 0.2
         */
        Comparator<ELEMENT> order();

        /**
         * Returns a {@link $.Func2} function that applied to an element in this {@code Range} and
         * an integer {@code n} indicate the number of steps. The result of the function is an element in
         * the range or the range domain after moving {@code n} steps based on the element.
         * <p>If the element apply is {@code null}, the function should throw out
         * {@link NullPointerException}; if the resulting element is not defined in the range
         * domain, the function should throw out {@link NoSuchElementException}</p>
         *
         * @return a function implement the stepping logic
         * @since 0.2
         */
        $.Func2<ELEMENT, Integer, ELEMENT> step();

        /**
         * Returns an new range this range and another range {@code r2} merged together. The two ranges must have
         * the equal {@link #step()} and {@link #order()} operator to be merged, otherwise,
         * {@link org.osgl.exception.InvalidArgException} will be thrown out
         * <p>The two ranges must be either overlapped or immediately connected to each other as per
         * {@link #step()} definition. Otherwise an {@link org.osgl.exception.InvalidArgException}
         * will be throw out:
         * <ul>
         * <li>if one range contains another range entirely, then the larger range is returned</li>
         * <li>if the two ranges overlapped or immediately connected to each other, then an range
         * contains all elements of the two ranges will be returned</li>
         * <li>an {@link org.osgl.exception.InvalidArgException} will be thrown out if the two ranges does not connected
         * to each other</li>
         * </ul>
         *
         * @param r2 the range to be merged with this range
         * @return an new range contains all elements in this range and r2
         * @throws org.osgl.exception.InvalidArgException if the two ranges does not have
         *                                                the same {@link #step()} operator or does not connect to each other
         * @since 0.2
         */
        Range<ELEMENT> merge(Range<ELEMENT> r2);

        ELEMENT last();

        Range<ELEMENT> tail(int n);

        Range<ELEMENT> reverse();

        Iterator<ELEMENT> reverseIterator();

        @SuppressWarnings("unused")
        <R> R reduceRight(R identity, $.Func2<R, ELEMENT, R> accumulator);

        @SuppressWarnings("unused")
        $.Option<ELEMENT> reduceRight($.Func2<ELEMENT, ELEMENT, ELEMENT> accumulator);

        @SuppressWarnings("unused")
        $.Option<ELEMENT> findLast($.Function<? super ELEMENT, Boolean> predicate);

        /**
         * {@inheritDoc}
         *
         * @param visitor {@inheritDoc}
         * @return this Range instance
         * @since 0.2
         */
        @Override
        Range<ELEMENT> accept($.Visitor<? super ELEMENT> visitor);

        @Override
        Range<ELEMENT> each($.Visitor<? super ELEMENT> visitor);

        @Override
        Range<ELEMENT> forEach($.Visitor<? super ELEMENT> visitor);

        /**
         * {@inheritDoc}
         *
         * @param visitor {@inheritDoc}
         * @return this Range instance
         * @since 0.2
         */
        @Override
        Range<ELEMENT> acceptLeft($.Visitor<? super ELEMENT> visitor);

        /**
         * iterate through the range from tail to head
         *
         * @param visitor a function to visit elements in the range
         * @return this Range instance
         * @since 0.2
         */
        @SuppressWarnings("unused")
        Range<ELEMENT> acceptRight($.Visitor<? super ELEMENT> visitor);
    }

    /**
     * The osgl List interface is a mixture of {@link java.util.List} and osgl {@link Sequence}
     *
     * @param <T> the element type of the {@code List}
     * @since 0.2
     */
    public interface List<T> extends java.util.List<T>, ReversibleSequence<T> {

        /**
         * A cursor points to an element of a {@link List}. It performs like
         * {@link java.util.ListIterator} but differs in the following way:
         * <ul>
         * <li>Add insert, append method</li>
         * <li>Support method chain calling style for most methods</li>
         * <li>A clear get() method to get the element the cursor point to</li>
         * <li>Unlike next/previous method, the new forward/backward method
         * returns a Cursor reference</li>
         * </ul>
         *
         * @param <T>
         */
        interface Cursor<T> {

            /**
             * Returns true if the cursor is not obsolete and points to an element
             * in the list
             *
             * @return true if this cursor is not obsolete and point to an element
             */
            boolean isDefined();

            /**
             * Returns the index of the element to which the cursor pointed
             *
             * @return the cursor index
             */
            int index();

            /**
             * Returns if the cursor can be moved forward to get the
             * next element
             *
             * @return {@code true} if there are element after the cursor in the
             * underline list
             */
            boolean hasNext();

            /**
             * Returns if the cursor can be moved backward to get the previous
             * element
             *
             * @return {@code true} if there are element before the cursor in the
             * underline list
             */
            boolean hasPrevious();

            /**
             * Move the cursor forward to make it point to the next element to
             * the current element
             *
             * @return the cursor points to the next element
             * @throws UnsupportedOperationException if cannot move forward anymore
             */
            Cursor<T> forward() throws UnsupportedOperationException;

            /**
             * Move the cursor backward to make it point to the previous element to
             * the current element
             *
             * @return the cursor points to the previous element
             * @throws UnsupportedOperationException if cannot move backward anymore
             */
            Cursor<T> backward() throws UnsupportedOperationException;

            /**
             * Park the cursor at the position before the first element.
             * <p>After calling this method, {@link #isDefined()}
             * shall return {@code false}</p>
             *
             * @return this cursor
             */
            Cursor<T> parkLeft();

            /**
             * Park the cursor at the position after the last element
             * <p>After calling this method, {@link #isDefined()}
             * shall return {@code false}</p>
             *
             * @return this cursor
             */
            Cursor<T> parkRight();

            /**
             * Returns the element this cursor points to. If the cursor isn't point
             * to any element, calling to this method will trigger
             * {@code NoSuchElementException} been thrown out. The only case
             * the cursor doesn't point to any element is when it is initialized
             * in which case the cursor index is -1
             *
             * @return the current element
             * @throws NoSuchElementException if the cursor isn't point to any element
             */
            T get() throws NoSuchElementException;

            /**
             * Replace the element this cursor points to with the new element specified.
             *
             * @param t the new element to be set to this cursor
             * @return the cursor itself
             * @throws IndexOutOfBoundsException if the cursor isn't point to any element
             * @throws NullPointerException      if when passing null value to this method and
             *                                   the underline list does not allow null value
             */
            Cursor<T> set(T t) throws IndexOutOfBoundsException, NullPointerException;

            /**
             * Remove the current element this cursor points to. After the element
             * is removed, the cursor points to the next element if there is next,
             * or if there isn't next element, the cursor points to the previous
             * element, or if there is previous element neither, then the cursor
             * points to {@code -1} position and the current element is not defined
             *
             * @return the cursor itself
             * @throws UnsupportedOperationException if the operation is not supported
             *                                       by the underline container does not support removing elements
             * @throws NoSuchElementException        if the cursor is parked either left or
             *                                       right
             */
            Cursor<T> drop() throws NoSuchElementException, UnsupportedOperationException;

            /**
             * Add an element in front of the element this cursor points to.
             * After added, the cursor should still point to the current element
             *
             * @param t the element to be inserted
             * @return this cursor which is still pointing to the current element
             * @throws IndexOutOfBoundsException if the current element is undefined
             */
            Cursor<T> prepend(T t) throws IndexOutOfBoundsException;

            /**
             * Add an element after the element this cursor points to.
             * After added, the cursor should still point to the current element
             *
             * @param t the element to be added
             * @return this cursor which is still pointing to the current element
             */
            Cursor<T> append(T t);
        }


        /**
         * Returns this traversable and make sure {@link C.Feature#PARALLEL} is set
         *
         * @return this reference with parallel turned on
         */
        @Override
        List<T> parallel();

        /**
         * Returns this traversable and make sure {@link C.Feature#PARALLEL} is unset
         *
         * @return this reference with parallel turned off
         */
        @Override
        List<T> sequential();

        /**
         * Returns this traversable and make sure {@link C.Feature#LAZY} is set
         *
         * @return this reference with lazy turned on
         */
        @Override
        List<T> lazy();

        /**
         * Returns this traversable and make sure {@link C.Feature#LAZY} is unset
         *
         * @return this reference with lazy turned off
         */
        @Override
        List<T> eager();

        /**
         * Returns an immutable list contains all elements of the current list.
         * If the current list is immutable, then return the current list itself.
         *
         * @return an immutable list.
         * @see #readOnly()
         */
        List<T> snapshot();

        /**
         * Returns a view of this list that is readonly. If the current list is
         * readonly or immutable then return the current list itself
         *
         * @return a readonly view of this list
         */
        @SuppressWarnings("unused")
        List<T> readOnly();

        /**
         * Returns a mutable copy of this list
         *
         * @return a mutable list contains all elements of this list
         */
        List<T> copy();

        /**
         * Returns a sorted copy of this list.
         * <p>Note if the element type T is not a {@link java.lang.Comparable} then
         * this method returns a {@link #copy() copy} of this list without any order
         * changes</p>
         *
         * @return a sorted copy of this list
         */
        List<T> sorted();

        /**
         * Return a list that contains unique set of this list and keep the orders. If
         * this list doesn't have duplicated items, it could return this list directly
         * or choose to return an new copy of this list depends on the sub class
         * implementation
         *
         * @return a list contains only unique elements in this list
         */
        List<T> unique();

        /**
         * Return a list that contains unique set as per the comparator specified of
         * this list and keep the orders. If this list doesn't have duplicated items,
         * it could return this list directly or choose to return an new copy of this list
         * depends on the sub class implementation
         *
         * @param comp the comparator check the duplicate elements
         * @return a list contains unique element as per the comparator
         */
        List<T> unique(Comparator<T> comp);

        /**
         * Returns a sorted copy of this list. The order is specified by the comparator
         * provided
         *
         * @param comparator specify the order of elements in the result list
         * @return an ordered copy of this list
         */
        List<T> sorted(Comparator<? super T> comparator);

        @Override
        List<T> subList(int fromIndex, int toIndex);

        /**
         * Add all elements from an {@link Iterable} into this list.
         * Return {@code true} if the list has changed as a result of
         * call.
         * <p><b>Note</b> if this list is immutable or readonly, {@code UnsupportedOperationException}
         * will be thrown out with this call</p>
         *
         * @param iterable the iterable provides the elements to be
         *                 added into the list
         * @return {@code true} if this list changed as result of addd
         */
        boolean addAll(Iterable<? extends T> iterable);

        /**
         * {@inheritDoc}
         *
         * @param n specify the number of elements to be included in the return list
         * @return A List contains first {@code n} items in this List
         */
        @Override
        List<T> head(int n);

        /**
         * {@inheritDoc}
         * <p>This method does not alter the underline list</p>
         *
         * @param n {@inheritDoc}
         * @return A list contains first {@code n} items in this list
         */
        @Override
        List<T> take(int n);

        /**
         * {@inheritDoc}
         *
         * @return A list contains all elements in this list except
         * the first one
         */
        @Override
        List<T> tail();

        /**
         * {@inheritDoc}
         * <p>This method does not alter the underline list</p>
         *
         * @param n {@inheritDoc}
         * @return A list contains last {@code n} items in this list
         */
        @Override
        List<T> tail(int n);

        /**
         * {@inheritDoc}
         * <p>This method does not alter the underline list</p>
         *
         * @param n {@inheritDoc}
         * @return a List contains all elements of this list
         * except the first {@code n} number
         */
        List<T> drop(int n);

        /**
         * {@inheritDoc}
         * <p>This method does not alter the underline list</p>
         *
         * @param predicate the predicate function
         * @return {@inheritDoc}
         */
        @Override
        List<T> dropWhile($.Function<? super T, Boolean> predicate);

        /**
         * {@inheritDoc}
         * <p>This method does not alter the underline list</p>
         *
         * @param predicate {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        List<T> takeWhile($.Function<? super T, Boolean> predicate);

        /**
         * For mutable list, remove all element that matches the predicate
         * specified from this List and return this list once done.
         * <p>For immutable or readonly list, an new List contains all element from
         * this list that does not match the predicate specified is returned</p>
         *
         * @param predicate test whether an element should be removed frmo
         *                  return list
         * @return a list contains all element that does not match the
         * predicate specified
         */
        List<T> remove($.Function<? super T, Boolean> predicate);

        @Override
        <R> C.List<R> map($.Function<? super T, ? extends R> mapper);

        /**
         * {@inheritDoc}
         *
         * @param mapper {@inheritDoc}
         * @param <R>    {@inheritDoc}
         * @return a List of {@code R} type element that are mapped from this sequences
         * @since 0.2
         */
        @Override
        <R> List<R> flatMap($.Function<? super T, ? extends Iterable<? extends R>> mapper);

        @Override
        List<T> filter($.Function<? super T, Boolean> predicate);

        /**
         * Split this list into two list based on the predicate specified.
         * 
         * The function use the predicate to test all elements in this list. If test passed
         * then it add the element into {@link $.T2#_1 left side list}, otherwise the
         * element will be added into {@link $.T2#_2 right side list}. The result
         * is returned as a {@link org.osgl.Lang.Tuple tuple} contains the left and
         * right side lift
         * </p>
         *
         * @param predicate the function to test the elements in this list
         * @return a tuple of two lists
         */
        $.T2<List<T>, List<T>> split($.Function<? super T, Boolean> predicate);

        /**
         * Find the first element in this list that matches the predicate.
         * Return a cursor point to the location of the element. If no
         * such element is found then a cursor that point to {@code -1}
         * is returned.
         *
         * @param predicate test the element
         * @return the reference to the list itself or an new List without the
         * first element matches the predicate if this is a readonly
         * list
         */
        Cursor<T> locateFirst($.Function<T, Boolean> predicate);

        /**
         * Locate any one element in the list that matches the predicate.
         * Returns the cursor point to the element found, or a cursor
         * that is not defined if no such element found in the list. In
         * a parallel locating the element been found might not be the
         * first element matches the predicate
         *
         * @param predicate the function that used to check the element
         *                  at the cursor
         * @return the reference to the list itself or an new List without
         * and element matches the predicate if this is a readonly
         * list
         */
        Cursor<T> locate($.Function<T, Boolean> predicate);

        /**
         * Locate the first element in this list that matches the predicate.
         * Return a cursor point to the location of the element. If no
         * such element is found then a cursor that point to {@code -1}
         * is returned.
         *
         * @param predicate test the element
         * @return the reference to the list itself or an new List without the
         * first element matches the predicate if this is a readonly
         * list
         */
        Cursor<T> locateLast($.Function<T, Boolean> predicate);

        /**
         * Insert an element at the position specified by {@code index}.
         * <p>If this list is readonly or immutable, then an new
         * list should be created with all elements in this list
         * and the new element inserted at the specified position.
         * The new list should have the same feature as this list</p>
         * <p>If index is less than zero then it will insert at
         * {@code (size() + index)}</p>
         *
         * @param index specify the position where the element should be inserted
         * @param t     the element to be inserted
         * @return a list as specified above
         * @throws IndexOutOfBoundsException Math.abs(index) &gt; size()
         */
        List<T> insert(int index, T t) throws IndexOutOfBoundsException;

        /**
         * Insert an array of elements at the position specified by {@code index}.
         * <p>If this list is readonly or immutable, then an new
         * list should be created with all elements in this list
         * and the new element inserted at the specified position.
         * The new list should have the same feature as this list</p>
         * <p>If index is less than zero then it will insert at
         * {@code (size() + index)}</p>
         *
         * @param index specify the position where the element should be inserted
         * @param ta    the array of elements to be inserted
         * @return a list as specified above
         * @throws IndexOutOfBoundsException Math.abs(index) &gt; size()
         */
        List<T> insert(int index, T... ta) throws IndexOutOfBoundsException;

        /**
         * Insert a sub list at the position specified by {@code index}.
         * <p>If this list is readonly or immutable, then an new
         * list should be created with all elements in this list
         * and the elements of sub list inserted at the specified position.
         * The new list should have the same feature as this list</p>
         * <p>If index is less than zero then it will insert at
         * {@code (size() + index)}</p>
         *
         * @param index   specify the position where the element should be inserted
         * @param subList the sub list contains elements to be inserted
         * @return a list as specified above
         * @throws IndexOutOfBoundsException Math.abs(index) &gt; size()
         */
        List<T> insert(int index, java.util.List<T> subList) throws IndexOutOfBoundsException;

        /**
         * {@inheritDoc}
         *
         * @param t {@inheritDoc}
         * @return a list contains elements in this list followed
         * by {@code t}
         */
        @Override
        List<T> append(T t);

        /**
         * {@inheritDoc}
         *
         * @param iterable the iterable from which elements will be appended to this list
         * @return a List contains all elements of this list followed
         * by all elements in the iterable
         */
        List<T> append(Collection<? extends T> iterable);

        /**
         * Returns a List contains all elements in this List followed by
         * all elements in the specified List.
         * <p>A mutable List implementation might choose to add elements
         * from the specified list directly to this list and return this
         * list directly</p>
         * <p>For a read only or immutable list, it must create an new list
         * to avoid update this list</p>
         *
         * @param list the list in which elements will be appended
         *             to this list
         * @return a list contains elements of both list
         */
        List<T> append(List<T> list);

        @Override
        List<T> prepend(T t);

        List<T> prepend(Collection<? extends T> collection);

        List<T> prepend(List<T> list);

        @Override
        List<T> reverse();

        /**
         * Returns a List contains all elements in this List and not in
         * the {@code col} collection specified
         *
         * @param col the collection in which elements should
         *            be excluded from the result List
         * @return a List contains elements only in this list
         */
        List<T> without(Collection<? super T> col);

        /**
         * Returns a list contains all elements in the list except the
         * one specified
         *
         * @param element the element that should not be in the resulting list
         * @return a list without the element specified
         */
        List<T> without(T element);

        /**
         * Returns a list contains all elements in the list except the
         * ones specified
         *
         * @param element  the element that should not be in the resulting list
         * @param elements the array contains elements that should not be in the resulting list
         * @return a list without the element specified
         */
        List<T> without(T element, T... elements);

        @Override
        List<T> accept($.Visitor<? super T> visitor);

        @Override
        List<T> each($.Visitor<? super T> visitor);

        @Override
        List<T> forEach($.Visitor<? super T> visitor);

        @Override
        List<T> acceptLeft($.Visitor<? super T> visitor);

        @Override
        List<T> acceptRight($.Visitor<? super T> visitor);

        /**
         * Loop through the list and for each element, call on the
         * indexedVisitor function specified
         *
         * @param indexedVisitor the function to be called on each element along with the index
         * @return this list
         */
        List<T> accept($.IndexedVisitor<Integer, ? super T> indexedVisitor);

        /**
         * Alias of {@link #accept(Lang.Visitor)}
         *
         * @param indexedVisitor the function to be called on each element along with the index
         * @return this list
         */
        List<T> each($.IndexedVisitor<Integer, ? super T> indexedVisitor);

        /**
         * Alias of {@link #accept(Lang.Visitor)}
         *
         * @param indexedVisitor the function to be called on each element along with the index
         * @return this list
         */
        List<T> forEach($.IndexedVisitor<Integer, ? super T> indexedVisitor);

        /**
         * Loop through the list from {@code 0} to {@code size - 1}. Call the indexedVisitor function
         * on each element along with the index
         *
         * @param indexedVisitor the function to be called on each element along with the index
         * @return this list
         */
        @SuppressWarnings("unused")
        List<T> acceptLeft($.IndexedVisitor<Integer, ? super T> indexedVisitor);

        /**
         * Loop through the list from {@code size() - 1} to {@code 0}. Call the indexedVisitor function
         * on each element along with the index
         *
         * @param indexedVisitor the function to be called on each element along with the index
         * @return this list
         */
        @SuppressWarnings("unused")
        List<T> acceptRight($.IndexedVisitor<Integer, ? super T> indexedVisitor);

        /**
         * Returns a list formed from this list and another iterable
         * collection by combining corresponding elements in pairs.
         * If one of the two collections is longer than the other,
         * its remaining elements are ignored.
         *
         * @param list the part B to be zipped with this list
         * @param <T2> the type of the iterable
         * @return an new list containing pairs consisting of
         * corresponding elements of this sequence and that.
         * The length of the returned collection is the
         * minimum of the lengths of this sequence and that.
         */
        <T2> List<$.Binary<T, T2>> zip(java.util.List<T2> list);

        /**
         * Returns a list formed from this list and another iterable
         * collection by combining corresponding elements in pairs.
         * If one of the two collections is longer than the other,
         * placeholder elements are used to extend the shorter collection
         * to the length of the longer.
         *
         * @param list the part B to be zipped with this list
         * @param <T2> the type of the iterable
         * @param def1 the element to be used to fill up the result if
         *             this sequence is shorter than that iterable
         * @param def2 the element to be used to fill up the result if
         *             the iterable is shorter than this sequence
         * @return a new list containing pairs consisting of
         * corresponding elements of this list and that.
         * The length of the returned collection is the
         * maximum of the lengths of this list and that.
         */
        <T2> List<$.Binary<T, T2>> zipAll(java.util.List<T2> list, T def1, T2 def2);

        /**
         * Zip this sequence with its indices
         *
         * @return A new list containing pairs consisting of all
         * elements of this list paired with their index.
         * Indices start at 0.
         */
        Sequence<$.Binary<T, Integer>> zipWithIndex();

        /**
         * Create a {@link Map} from this list using a key extract function and a value extract function
         * 
         * The key extractor will take the element stored in this list and calculate a key,
         * and then store the element being used along with the key calculated into the Map
         * to be returned.
         * 
         * The value extractor will take the element stored in this list and calculate a value,
         * and then store the element as the key along with the outcome as the value
         *
         * @param keyExtractor the function that generate Map key from the element in this list
         * @param valExtractor the function that generate Map value from the element in this list
         * @param <K>          the generic type of key in the Map
         * @param <V>          the generic type of value in the Map
         * @return a Map as described above
         */
        <K, V> Map<K, V> toMap($.Function<? super T, ? extends K> keyExtractor, $.Function<? super T, ? extends V> valExtractor);


        /**
         * Create a {@link Map} from this list using a key extract function.
         * 
         * The key extractor will take the element stored in this list and calculate a key,
         * and then store the element being used along with the key calculated into the Map
         * to be returned.
         *
         * @param keyExtractor the function that generate Map key from the element in this list
         * @param <K>          the generic type of key in the Map
         * @return a Map that indexed by key generated by the function from the element in this list
         */
        <K> Map<K, T> toMapByVal($.Function<? super T, ? extends K> keyExtractor);

        /**
         * Create a {@link Map} from this list using a value extract function.
         * 
         * The value extractor will take the element stored in this list and calculate a value,
         * and then store the element as the key along with the outcome as the value
         *
         * @param valExtractor the function that generate Map value from the element in this list
         * @param <V>          the generic type of value in the Map
         * @return a Map that stores the value calculated along with the corresponding element as the key
         */
        <V> Map<T, V> toMapByKey($.Function<? super T, ? extends V> valExtractor);
    }


    public static class Map<K, V> implements java.util.Map<K, V>, Serializable {
        public static class Entry<K, V> extends $.T2<K, V> implements java.util.Map.Entry<K, V> {
            public Entry(K _1, V _2) {
                super(_1, _2);    //To change body of overridden methods use File | Settings | File Templates.
            }

            @Override
            public K getKey() {
                return _1;
            }

            @Override
            public V getValue() {
                return _2;
            }

            @Override
            public V setValue(V value) {
                throw E.unsupport();
            }

            public static <K, V> Entry<K, V> valueOf(K k, V v) {
                return new Entry<>(k, v);
            }
        }

        public class _Builder {
            private K key;

            private _Builder(K key) {
                this.key = $.ensureNotNull(key);
            }

            public <BV> Map<K, BV> to(V val) {
                Map<K, V> me = Map.this;
                if (me.ro) {
                    Map<K, V> newMap = Mutable.Map(me);
                    newMap.put(key, val);
                    return $.cast(newMap);
                }
                Map.this.put(key, val);
                return $.cast(Map.this);
            }
        }

        private java.util.Map<K, V> _m;

        private boolean ro;

        @SuppressWarnings("unchecked")
        protected Map(boolean readOnly, Object... args) {
            HashMap<K, V> map = new HashMap<>();
            int len = args.length;
            for (int i = 0; i < len; i += 2) {
                K k = (K) args[i];
                V v = null;
                if (i + 1 < len) {
                    v = (V) args[i + 1];
                }
                map.put(k, v);
            }
            ro = readOnly;
            if (readOnly) {
                _m = Collections.unmodifiableMap(map);
            } else {
                _m = map;
            }
        }

        protected Map(boolean readOnly, java.util.Map<? extends K, ? extends V> map) {
            E.NPE(map);
            boolean sorted = map instanceof SortedMap;
            java.util.Map<K, V> m = sorted ? new TreeMap<K, V>() : new HashMap<K, V>();
            for (K k : map.keySet()) {
                V v = map.get(k);
                m.put(k, v);
            }
            ro = readOnly;
            if (readOnly) {
                _m = Collections.unmodifiableMap(m);
            } else {
                _m = m;
            }
        }

        @Override
        public int size() {
            return _m.size();
        }

        @Override
        public boolean isEmpty() {
            return _m.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return _m.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return _m.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return _m.get(key);
        }

        @Override
        public V put(K key, V value) {
            E.unsupportedIf(ro, "The Map is read only");
            return _m.put(key, value);
        }

        @Override
        public V remove(Object key) {
            E.unsupportedIf(ro, "The Map is read only");
            return _m.remove(key);
        }

        @Override
        public void putAll(java.util.Map<? extends K, ? extends V> m) {
            E.unsupportedIf(ro, "The Map is read only");
            _m.putAll(m);
        }

        @Override
        public void clear() {
            E.unsupportedIf(ro, "The Map is read only");
            _m.clear();
        }

        @Override
        public java.util.Set<K> keySet() {
            return _m.keySet();
        }

        @Override
        public Collection<V> values() {
            return _m.values();
        }

        @Override
        public Set<java.util.Map.Entry<K, V>> entrySet() {
            Set<java.util.Map.Entry<K, V>> set = C.Mutable.Set();
            for (K k : _m.keySet()) {
                V v = _m.get(k);
                set.add(Entry.valueOf(k, v));
            }
            return set;
        }

        @Override
        public int hashCode() {
            return _m.hashCode();
        }

        @Override
        public String toString() {
            StringBuilder sb = S.builder(_m.toString());
            if (ro) {
                sb.append("[ro]");
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (!(o instanceof java.util.Map)) {
                return false;
            }

            if (o instanceof Map) {
                return o.equals(_m) && ((Map) o).ro == ro;
            }

            return o.equals(_m);
        }

        // --- extensions
        public _Builder map(K key) {
            return new _Builder(key);
        }

        /**
         * Check if this Map is read only
         * @return `true` if the Map is read only or `false` otherwise
         */
        @SuppressWarnings("unused")
        public boolean readOnly() {
            return ro;
        }

        /**
         * Return a Map with specified read only flag.
         *
         * If the current Map has different read only flag with the
         * specified read only flag, then it will return an new Map
         * contains all entries in this Map instance.
         *
         * Otherwise it returns this Map instance directly
         *
         * @param readOnly the read only flag
         * @return A map as described above
         */
        @SuppressWarnings("unused")
        public Map<K, V> readOnly(boolean readOnly) {
            if (ro ^ readOnly) {
                return new Map<>(readOnly, _m);
            } else {
                return this;
            }
        }

        /**
         * Return a Map contains all entries in this Map with entry
         * key applied to the predicate function specified
         *
         * @param predicate the key predicate function
         * @return A map as described above
         */
        public Map<K, V> filter($.Function<K, Boolean> predicate) {
            Map<K, V> filtered = new Map<>(false);
            for (java.util.Map.Entry<K, V> entry : entrySet()) {
                if (predicate.apply(entry.getKey())) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
            return filtered;
        }

        /**
         * Return a Map contains all entries in this Map with entry
         * value applied to the predicate function specified
         *
         * @param predicate the value predicate function
         * @return A map as described above
         */
        public Map<K, V> filterByValue($.Function<V, Boolean> predicate) {
            Map<K, V> filtered = new Map<>(false);
            for (java.util.Map.Entry<K, V> entry : entrySet()) {
                if (predicate.apply(entry.getValue())) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
            return filtered;
        }

        /**
         * Loop through this Map on each key/value pair, apply them to the function specified
         *
         * @param indexedVisitor the function that takes argument of (key, value) pair
         * @return this Map
         */
        public Map<K, V> forEach($.IndexedVisitor<? super K, ? super V> indexedVisitor) {
            for (java.util.Map.Entry<K, V> entry : entrySet()) {
                try {
                    indexedVisitor.apply(entry.getKey(), entry.getValue());
                } catch (NotAppliedException e) {
                    // ignore
                }
            }
            return this;
        }

        /**
         * Alias of {@link #forEach(Lang.IndexedVisitor)}
         *
         * @param indexedVisitor the visitor that can be applied on Key/Value pair stored in this Map
         * @return this Map
         */
        public Map<K, V> each($.IndexedVisitor<? super K, ? super V> indexedVisitor) {
            return forEach(indexedVisitor);
        }

        /**
         * Alias of {@link #forEach(Lang.IndexedVisitor)}
         *
         * @param indexedVisitor the visitor that can be applied on Key/Value pair stored in this Map
         * @return this Map
         */
        public Map<K, V> accept($.IndexedVisitor<? super K, ? super V> indexedVisitor) {
            return forEach(indexedVisitor);
        }

        public <NV> Map<K, NV> transformValues($.Function<V, NV> valueTransformer) {
            Map<K, NV> newMap = Mutable.Map();
            for (java.util.Map.Entry<K, V> entry : entrySet()) {
                newMap.put(entry.getKey(), valueTransformer.apply(entry.getValue()));
            }
            return newMap.readOnly(readOnly());
        }

        public <NK> Map<NK, V> transformKeys($.Function<K, NK> keyTransformer) {
            Map<NK, V> newMap = Mutable.Map();
            for (java.util.Map.Entry<K, V> entry : entrySet()) {
                newMap.put(keyTransformer.apply(entry.getKey()), entry.getValue());
            }
            return newMap.readOnly(readOnly());
        }

        public <NK, NV> Map<NK, NV> transform($.Function<K, NK> keyTransformer, $.Function<V, NV> valueTransformer) {
            Map<NK, NV> newMap = C.Mutable.Map();
            for (java.util.Map.Entry<K, V> entry : entrySet()) {
                newMap.put(keyTransformer.apply(entry.getKey()), valueTransformer.apply(entry.getValue()));
            }
            return newMap.readOnly(readOnly());
        }

        public Set<$.Binary<K, V>> zip() {
            C.Set<$.Binary<K, V>> zipped = C.Mutable.Set();
            for (java.util.Map.Entry<K, V> entry : entrySet()) {
                zipped.add($.T2(entry.getKey(), entry.getValue()));
            }
            return zipped;
        }

        private void writeObject(java.io.ObjectOutputStream s) throws IOException {
            s.defaultWriteObject();
            s.writeObject(_m);
            if (ro) s.writeInt(1);
            else s.writeInt(0);
        }

        private static final long serialVersionUID = 262498820763181265L;

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream s)
                throws IOException, ClassNotFoundException {
            s.defaultReadObject();
            _m = (java.util.Map) s.readObject();
            int i = s.readInt();
            ro = i != 0;
        }
    }

    public interface Set<T> extends java.util.Set<T>, Traversable<T> {
        @Override
        Set<T> parallel();

        @Override
        Set<T> sequential();

        @Override
        Set<T> lazy();

        @Override
        Set<T> eager();

        @Override
        Set<T> filter($.Function<? super T, Boolean> predicate);

        @Override
        Set<T> accept($.Visitor<? super T> visitor);

        @Override
        Set<T> each($.Visitor<? super T> visitor);

        @Override
        Set<T> forEach($.Visitor<? super T> visitor);

        /**
         * Returns a set contains all elements in the {@code col}
         * collection specified but not in this set
         *
         * @param col the collection in which elements should
         *            be included from the result set
         * @return a set contains elements only in the col
         */
        @SuppressWarnings("unused")
        Set<T> onlyIn(Collection<? extends T> col);

        /**
         * Returns a set contains only elements in both {@code col}
         * collection specified and this set
         *
         * @param col the collection in which elements should
         *            be included from the result set
         * @return a set contains elements in both col and this set
         */
        Set<T> withIn(Collection<? extends T> col);


        /**
         * Returns a set contains all elements in this set and not in
         * the {@code col} collection specified
         *
         * @param col the collection in which elements should
         *            be excluded from the result set
         * @return a set contains elements only in this set
         */
        Set<T> without(Collection<? super T> col);

        /**
         * Returns a set contains all elements in this set and all
         * elements in the {@code col} collection specified
         *
         * @param col the collection in which elements should be
         *            included in the result set
         * @return a set contains elements in both this set and the collection
         */
        Set<T> with(Collection<? extends T> col);

        /**
         * Returns a set contains all elements in this set plus the element
         * specified
         *
         * @param element the new element that will be contained in the returning set
         * @return a set as described above
         */
        Set<T> with(T element);

        /**
         * Returns a set contains all elements in this set plus all the elements
         * specified in the parameter list
         *
         * @param elements elements to be added into the returning set
         * @return a set as described above
         */
        Set<T> with(T... elements);

        /**
         * Returns a set contains all elements in the set except the
         * one specified
         *
         * @param element the element that should not be in the resulting set
         * @return a set without the element specified
         */
        Set<T> without(T element);

        /**
         * Returns a set contains all elements in the set except the
         * ones specified
         *
         * @param elements the array contains elements that should not be in the resulting set
         * @return a set without the element specified
         */
        Set<T> without(T... elements);

    }

    public interface ListOrSet<T> extends List<T>, Set<T> {
        @Override
        ListOrSet<T> parallel();

        @Override
        ListOrSet<T> sequential();

        @Override
        ListOrSet<T> lazy();

        @Override
        ListOrSet<T> eager();

        @Override
        ListOrSet<T> accept($.Visitor<? super T> visitor);

        @Override
        ListOrSet<T> each($.Visitor<? super T> visitor);

        @Override
        ListOrSet<T> forEach($.Visitor<? super T> visitor);

        @Override
        ListOrSet<T> filter($.Function<? super T, Boolean> predicate);

        @Override
        ListOrSet<T> without(Collection<? super T> col);

        @Override
        ListOrSet<T> without(T element);

        @Override
        ListOrSet<T> without(T... elements);

        @Override
        <R> ListOrSet<R> map($.Function<? super T, ? extends R> mapper);

    }

    /**
     * Defines a factory to create {@link java.util.List java List} instance
     * used by {@link DelegatingList} to create it's backing data structure
     *
     * @since 0.2
     */
    public interface ListFactory {
        /**
         * Create an empty <code>java.util.List</code> contains the generic type E
         *
         * @param <ET> the generic type of the list element
         * @return A java List instance contains elements of generic type E
         */
        <ET> java.util.List<ET> create();

        /**
         * Create a <code>java.util.List</code> pre populated with elements
         * of specified collection
         *
         * @param collection the collection whose elements are to be placed into this list
         * @param <ET>       the generic type of the list element
         * @return The List been created
         * @throws NullPointerException if the specified collection is null
         */
        <ET> java.util.List<ET> create(Collection<? extends ET> collection) throws NullPointerException;

        /**
         * Create a <code>java.util.List</code> with initial capacity
         *
         * @param initialCapacity the initial capacity of the new List
         * @param <ET>            the generic type of the list element
         * @return the list been created
         */
        <ET> java.util.List<ET> create(int initialCapacity);

        enum Predefined {
            ;
            static final ListFactory JDK_ARRAYLIST_FACT = new ListFactory() {
                @Override
                public <ET> java.util.List<ET> create() {
                    return new ArrayList<>();
                }

                @Override
                public <ET> java.util.List<ET> create(Collection<? extends ET> collection) {
                    return new ArrayList<>(collection);
                }

                @Override
                public <ET> java.util.List<ET> create(int initialCapacity) {
                    return new ArrayList<>(initialCapacity);
                }
            };
            static final ListFactory JDK_LINKEDLIST_FACT = new ListFactory() {
                @Override
                public <ET> java.util.List<ET> create() {
                    return new LinkedList<>();
                }

                @Override
                public <ET> java.util.List<ET> create(Collection<? extends ET> collection) {
                    return new LinkedList<>(collection);
                }

                @Override
                public <ET> java.util.List<ET> create(int initialCapacity) {
                    return new LinkedList<>();
                }
            };

            static ListFactory defLinked() {
                return JDK_LINKEDLIST_FACT;
            }

            static ListFactory defRandomAccess() {
                return JDK_ARRAYLIST_FACT;
            }
        }
    }

    /**
     * "osgl.List.factory", the property key to configure user defined
     * {@link ListFactory List factory}.
     * Upon loaded, osgl tried to get a class name string from system
     * properties use this configuration key. If osgl find the String
     * returned is not empty then it will initialize the List factory
     * use the class name configured. If any exception raised during the
     * initialization, then it might cause the JVM failed to boot up
     *
     * @since 0.2
     */
    public static final String CONF_LINKED_LIST_FACTORY = "osgl.linked_list.factory";

    /**
     * "osgl.random_access_list.factory", the property key to configure user defined {@link ListFactory
     * random access List factory}. See {@link #CONF_LINKED_LIST_FACTORY} for how osgl use this configuration
     *
     * @since 0.2
     */
    public static final String CONF_RANDOM_ACCESS_LIST_FACTORY = "osgl.random_access_list.factory";

    static ListFactory linkedListFact;

    static {
        String factCls = System.getProperty(CONF_LINKED_LIST_FACTORY);
        if (null == factCls) {
            linkedListFact = ListFactory.Predefined.defLinked();
        } else {
            $.Option<ListFactory> fact = $.safeNewInstance(factCls);
            if (fact.isDefined()) {
                linkedListFact = fact.get();
            } else {
                linkedListFact = ListFactory.Predefined.defLinked();
            }
        }
    }

    static ListFactory randomAccessListFact;

    static {
        String factCls = System.getProperty(CONF_RANDOM_ACCESS_LIST_FACTORY);
        if (null == factCls) {
            randomAccessListFact = ListFactory.Predefined.defRandomAccess();
        } else {
            $.Option<ListFactory> fact = $.safeNewInstance(factCls);
            if (fact.isDefined()) {
                randomAccessListFact = fact.get();
            } else {
                randomAccessListFact = ListFactory.Predefined.defRandomAccess();
            }
        }
    }

    public static boolean empty(Collection<?> col) {
        return null == col || col.isEmpty();
    }

    @SuppressWarnings("unused")
    public static boolean notEmpty(Collection<?> col) {
        return !empty(col);
    }

    public static boolean isEmpty(Collection<?> col) {
        return empty(col);
    }

    public static boolean empty(java.util.Map map) {
        return null == map || map.isEmpty();
    }

    public static boolean notEmpty(java.util.Map map) {
        return !empty(map);
    }

    public static boolean isEmpty(java.util.Map map) {
        return empty(map);
    }

    // --- conversion methods ---
    public static <T> Collection<T> asCollection(Iterable<T> iterable) {
        if (iterable instanceof Collection) {
            return $.cast(iterable);
        }
        return C.List(iterable);
    }
    // --- eof conversion methods ---

    // --- factory methods ---

    /**
     * Returns a {@link Range} of integer specified by {@code from} and {@code to}. {@code from}
     * can be less or larger than {@code to}.
     *
     * @param from specify the left side of the range (inclusive)
     * @param to   specify the right hand side of the range (exclusive)
     * @return a range of integer @{code [from .. to)}
     */
    public static Range<Integer> range(int from, int to) {
        return new LazyRange<Integer>(from, to, N.F.INT_RANGE_STEP);
    }

    /**
     * Returns a {@link Range} of byte specified by {@code from} and {@code to}. {@code from}
     * can be less or larger than {@code to}.
     *
     * @param from specify the left side of the range (inclusive)
     * @param to   specify the right hand side of the range (exclusive)
     * @return a range of byte @{code [from .. to)}
     */
    public static Range<Byte> range(byte from, byte to) {
        return new LazyRange<Byte>(from, to, N.F.BYTE_RANGE_STEP);
    }

    /**
     * Returns a {@link Range} of short specified by {@code from} and {@code to}. {@code from}
     * can be less or larger than {@code to}.
     *
     * @param from specify the left side of the range (inclusive)
     * @param to   specify the right hand side of the range (exclusive)
     * @return a range of short @{code [from .. to)}
     */
    public static Range<Short> range(short from, short to) {
        return new LazyRange<Short>(from, to, N.F.SHORT_RANGE_STEP);
    }

    /**
     * Returns a {@link Range} of long specified by {@code from} and {@code to}. {@code from}
     * can be less or larger than {@code to}.
     *
     * @param from specify the left side of the range (inclusive)
     * @param to   specify the right hand side of the range (exclusive)
     * @return a range of long @{code [from .. to)}
     */
    public static Range<Long> range(long from, long to) {
        return new LazyRange<Long>(from, to, N.F.LONG_RANGE_STEP);
    }

    /**
     * Returns a {@link Range} of non-negative integers start from {@code 0} to {@code Integer.MAX_VALUE}. Note
     * unlike traditional definition of natural number, zero is included in the range returned
     *
     * @return a range of non negative integers
     */
    @SuppressWarnings("unused")
    public static Range<Integer> naturalNumbers() {
        return new LazyRange<Integer>(1, Integer.MAX_VALUE, N.F.INT_RANGE_STEP);
    }

    /**
     * Returns a {@link Range} of non-negative even numbers starts from {@code 0} to
     * {@code Integer.MAX_VALUE}.
     *
     * @return a range of non-negative even numbers
     */
    @SuppressWarnings("unused")
    public static Range<Integer> evenNumbers() {
        return new LazyRange<Integer>(0, Integer.MAX_VALUE, N.F.intRangeStep(2));
    }

    /**
     * Returns a {@link Range} of positive odd numbers starts from {@code 1} to
     * {@code Integer.MAX_VALUE}.
     *
     * @return a range of positive odd numbers
     */
    @SuppressWarnings("unused")
    public static Range<Integer> oddNumbers() {
        return new LazyRange<Integer>(1, Integer.MAX_VALUE, N.F.intRangeStep(2));
    }

    /**
     * An immutable empty List
     */
    public static final List EMPTY_LIST = Nil.list();

    /**
     * An immutable empty Set
     */
    public static final Set EMPTY_SET = Nil.set();

    /**
     * An immutable empty map
     */
    public static final Map EMPTY_MAP = Nil.EMPTY_MAP;

    /**
     * An immutable empty collection that is both List and Set
     */
    public static final ListOrSet EMPTY = Nil.EMPTY;

    /**
     * Returns an empty collection that is both a List and a Set
     *
     * @param <T> the generic type of the collection element
     * @return the collection as described above
     */
    public static <T> ListOrSet<T> empty() {
        return EMPTY;
    }

    /**
     * Returns an empty collection that is both a List and a Set with
     * element type specified by the class `c`
     *
     * @param c   the class sepcify the collection element type
     * @param <T> the generic type of the collection element
     * @return the collection as described above
     */
    public static <T> ListOrSet<T> empty(Class<T> c) {
        return EMPTY;
    }

    /**
     * Returns an empty immutable list
     *
     * @param <T> the type of the list element
     * @return the list as described above
     */
    public static <T> List<T> List() {
        return Nil.list();
    }

    /**
     * Returns an empty immutable list of type specified by class `c`
     *
     * @param c   the class specify the list element type
     * @param <T> the generic type of the list element
     * @return the list as described above
     */
    public static <T> List<T> emptyList(Class<T> c) {
        return Nil.list();
    }

    /**
     * Creates an immutable list from an array of elements.
     *
     * Note the array will not be copied, instead it will
     * be used directly as the backing data for the list.
     * To create an list with a copy of the array specified.
     * Use the {@link #toList(Object[])} method
     *
     * @param elements an array of elements
     * @param <T>      the element type
     * @return the list as described above
     */
    public static <T> List<T> List(T... elements) {
        return ImmutableList.of(elements);
    }

    /**
     * Creates an immutable list from an array of elements.
     *
     * Note elements in the array will be copied into the
     * return list
     *
     * @param elements an array of elements
     * @param <T>      the element type
     * @return the list as described above
     */
    public static <T> List<T> toList(T... elements) {
        return ImmutableList.copyOf(elements);
    }

    /**
     * Create an immutable Boolean list from a boolean array.
     * 
     * Note the array will not be copied, instead it will
     * be used directly as the backing data for the list.
     * To create an list with a copy of the array specified.
     * Use the {@link #toList(boolean[])} method
     *
     * @param elements a boolean array
     * @return the list as described above
     */
    public static List<Boolean> List(boolean[] elements) {
        switch (elements.length) {
            case 0:
                return Nil.list();
            case 1:
                return $.val(elements[0]);
            default:
                Boolean[] ba = $.asWrapped(elements);
                return ImmutableList.of(ba);
        }
    }

    /**
     * Creates an immutable list from a boolean array
     * 
     * Note elements in the array will be copied into the
     * return list
     *
     * @param elements a boolean array
     * @return the list as described above
     */
    public static List<Boolean> toList(boolean[] elements) {
        switch (elements.length) {
            case 0:
                return Nil.list();
            case 1:
                return $.val(elements[0]);
            default:
                Boolean[] ba = $.asWrapped(elements);
                return ImmutableList.copyOf(ba);
        }
    }

    /**
     * Create an immutable Byte list from a byte array.
     * 
     * Note the array will not be copied, instead it will
     * be used directly as the backing data for the list.
     * To create an list with a copy of the array specified.
     * Use the {@link #toList(byte[])} method
     *
     * @param elements a byte array
     * @return the list as described above
     */
    public static List<Byte> List(byte[] elements) {
        switch (elements.length) {
            case 0:
                return Nil.list();
            case 1:
                return $.val(elements[0]);
            default:
                Byte[] ba = $.asWrapped(elements);
                return ImmutableList.of(ba);
        }
    }

    /**
     * Creates an immutable list from a byte array
     * 
     * Note elements in the array will be copied into the
     * return list
     *
     * @param elements a byte array
     * @return the List as described above
     */
    public static List<Byte> toList(byte[] elements) {
        switch (elements.length) {
            case 0:
                return Nil.list();
            case 1:
                return $.val(elements[0]);
            default:
                Byte[] ba = $.asWrapped(elements);
                return ImmutableList.copyOf(ba);
        }
    }

    /**
     * Create a {@link FastStr} from a char array.
     *
     * Note the array will not be copied, instead it will
     * be used directly as the backing data for the list.
     * To create an list with a copy of the array specified.
     * Use the {@link #toList(char[])} method
     *
     * @param elements a char array
     * @return a {@link FastStr} instance
     */
    public static FastStr List(char[] elements) {
        switch (elements.length) {
            case 0:
                return FastStr.EMPTY_STR;
            default:
                return FastStr.unsafeOf(elements);
        }
    }

    /**
     * Create a {@link FastStr} from a char array.
     *
     * Note the elements in array is copied into the return
     * list
     *
     * @param elements a char array
     * @return a {@link FastStr} instance
     */
    public static FastStr toList(char[] elements) {
        switch (elements.length) {
            case 0:
                return FastStr.EMPTY_STR;
            default:
                return FastStr.of(elements);
        }
    }

    /**
     * Create a {@link FastStr} from a Character array.
     * 
     * Note the array will not be copied, instead it will
     * be used directly as the backing data for the list.
     * To create an list with a copy of the array specified.
     * Use the {@link #toList(Character[])} method
     *
     * @param elements a Character array
     * @return a {@link FastStr} instance
     */
    public static FastStr List(Character... elements) {
        switch (elements.length) {
            case 0:
                return FastStr.EMPTY_STR;
            default:
                return FastStr.unsafeOf($.asPrimitive(elements));
        }
    }

    /**
     * Create a {@link FastStr} from a Character array.
     * 
     * Note the elements in array is copied into the return
     * list
     *
     * @param elements a Character array
     * @return a {@link FastStr} instance
     */
    public static FastStr toList(Character... elements) {
        switch (elements.length) {
            case 0:
                return FastStr.EMPTY_STR;
            default:
                return FastStr.of(elements);
        }
    }

    /**
     * Create an immutable Short list from a short array.
     *
     * Note the array will not be copied, instead it will
     * be used directly as the backing data for the list.
     * To create an list with a copy of the array specified.
     * Use the {@link #toList(short[])} method
     *
     * @param elements a short array
     * @return a List as described above
     */
    public static List<Short> List(short[] elements) {
        switch (elements.length) {
            case 0:
                return Nil.list();
            case 1:
                return $.val(elements[0]);
            default:
                return ImmutableList.of($.asWrapped(elements));
        }
    }

    /**
     * Create an immutable List from a short array.
     *
     * Note the elements in array is copied into the return
     * list
     *
     * @param elements a short array
     * @return a List as described above
     */
    public static List<Short> toList(short[] elements) {
        switch (elements.length) {
            case 0:
                return Nil.list();
            case 1:
                return $.val(elements[0]);
            default:
                return ImmutableList.copyOf($.asWrapped(elements));
        }
    }

    /**
     * Create an immutable Integer list from a int array.
     *
     * Note the array will not be copied, instead it will
     * be used directly as the backing data for the list.
     * To create an list with a copy of the array specified.
     * Use the {@link #toList(int[])} method
     *
     * @param elements a int array
     * @return a List as described above
     */
    public static List<Integer> List(int[] elements) {
        switch (elements.length) {
            case 0:
                return Nil.list();
            case 1:
                return $.val(elements[0]);
            default:
                Integer[] wrapped = $.asWrapped(elements);
                return ImmutableList.of(wrapped);
        }
    }

    /**
     * Create an immutable Integer List from a int array.
     *
     * Note the elements in array is copied into the return
     * list
     *
     * @param elements a int array
     * @return a List as described above
     */
    public static List<Integer> toList(int[] elements) {
        switch (elements.length) {
            case 0:
                return Nil.list();
            case 1:
                return $.val(elements[0]);
            default:
                Integer[] wrapped = $.asWrapped(elements);
                return ImmutableList.copyOf(wrapped);
        }
    }

    /**
     * Create an immutable Long list from a long array.
     *
     * Note the array will not be copied, instead it will
     * be used directly as the backing data for the list.
     * To create an list with a copy of the array specified.
     * Use the {@link #toList(long[])} method
     *
     * @param elements a long array
     * @return a List as described above
     */
    public static List<Long> List(long[] elements) {
        switch (elements.length) {
            case 0:
                return Nil.list();
            case 1:
                return $.val(elements[0]);
            default:
                Long[] wrapped = $.asWrapped(elements);
                return ImmutableList.of(wrapped);
        }
    }

    /**
     * Create an immutable Long List from a long array.
     *
     * Note the elements in array is copied into the return
     * list
     *
     * @param elements a long array
     * @return a List as described above
     */
    public static List<Long> toList(long[] elements) {
        switch (elements.length) {
            case 0:
                return Nil.list();
            case 1:
                return $.val(elements[0]);
            default:
                Long[] wrapped = $.asWrapped(elements);
                return ImmutableList.copyOf(wrapped);
        }
    }

    /**
     * Create an immutable Float list from a float array.
     *
     * Note the array will not be copied, instead it will
     * be used directly as the backing data for the list.
     * To create an list with a copy of the array specified.
     * Use the {@link #toList(float[])} method
     *
     * @param elements a float array
     * @return a List as described above
     */
    public static List<Float> List(float[] elements) {
        switch (elements.length) {
            case 0:
                return Nil.list();
            case 1:
                return $.val(elements[0]);
            default:
                Float[] wrapped = $.asWrapped(elements);
                return ImmutableList.of(wrapped);
        }
    }

    /**
     * Create an immutable Float List from a float array.
     *
     * Note the elements in array is copied into the return
     * list
     *
     * @param elements a float array
     * @return a List as described above
     */
    public static List<Float> toList(float[] elements) {
        switch (elements.length) {
            case 0:
                return Nil.list();
            case 1:
                return $.val(elements[0]);
            default:
                Float[] wrapped = $.asWrapped(elements);
                return ImmutableList.copyOf(wrapped);
        }
    }

    /**
     * Create an immutable Double list from a double array.
     *
     * Note the array will not be copied, instead it will
     * be used directly as the backing data for the list.
     * To create an list with a copy of the array specified.
     * Use the {@link #toList(double[])} method
     *
     * @param elements a long array
     * @return a List as described above
     */
    public static List<Double> List(double[] elements) {
        switch (elements.length) {
            case 0:
                return Nil.list();
            case 1:
                return $.val(elements[0]);
            default:
                Double[] wrapped = $.asWrapped(elements);
                return ImmutableList.of(wrapped);
        }
    }

    /**
     * Create an immutable Double List from a double array.
     *
     * Note the elements in array is copied into the return
     * list
     *
     * @param elements a double array
     * @return a List as described above
     */
    public static List<Double> toList(double[] elements) {
        switch (elements.length) {
            case 0:
                return Nil.list();
            case 1:
                return $.val(elements[0]);
            default:
                Double[] wrapped = $.asWrapped(elements);
                return ImmutableList.copyOf(wrapped);
        }
    }

    /**
     * Create an immutable List from an iterable
     * @param iterable the iterable
     * @param <T> the generic type of the iterable
     * @return an List that contains all elements in the iterable
     */
    public static <T> List<T> List(Iterable<? extends T> iterable) {
        return ListBuilder.toList(iterable);
    }

    /**
     * Create an immutable List from an iterator
     * @param iterator the iterator
     * @param <T> the generic type of the iterator
     * @return an List that contains all elements the iterator iterated
     */
    public static <T> List<T> List(Iterator<? extends T> iterator) {
        return ListBuilder.toList(iterator);
    }

    /**
     * Create an immutable List from an enumeration
     * @param enumeration the enumeration
     * @param <T> the generic type of the enumeration
     * @return an List that contains all elements in the enumeration
     */
    public static <T> List<T> List(Enumeration<? extends T> enumeration) {
        return ListBuilder.toList(enumeration);
    }

    /**
     * Create an immutable List from an collection
     * @param col the collection
     * @param <T> the generic type of the collection
     * @return an List that contains all elements in the collection
     */
    public static <T> List<T> List(Collection<? extends T> col) {
        return ListBuilder.toList(col);
    }

    /**
     * Create an immutable List from a JDK List
     * @param javaList the JDK list
     * @param <T> the generic type of the JDK list
     * @return an List that contains all elements in the JDK list
     */
    public static <T> List<T> List(java.util.List<? extends T> javaList) {
        if (javaList instanceof List) {
            List<T> list = $.cast(javaList);

            if (list.is(Feature.IMMUTABLE)) {
                return list;
            } else {
                return new ReadOnlyDelegatingList<>(list);
            }
        }
        return new ReadOnlyDelegatingList<>(javaList);
    }

    /**
     * Returns a single element immutable List
     * @param t the element
     * @param <T> the generic type of the element
     * @return the List as described above
     */
    public static <T> List<T> singletonList(T t) {
        return $.val(t);
    }


    /**
     * Return a {@link Sequence} consists of all elements in the
     * iterable specified
     *
     * @param iterable the iterable in which elements will be used to fill into the sequence
     * @param <T>      the element type
     * @return the sequence
     */
    @SuppressWarnings("unchecked")
    public static <T> Sequence<T> seq(Iterable<? extends T> iterable) {
        if (iterable instanceof Sequence) {
            return ((Sequence<T>) iterable);
        }
        return IterableSeq.of(iterable);
    }

    public static <T> Sequence<T> seq(Iterator<? extends T> iterator) {
        return IteratorSeq.of(iterator);
    }

    public static <T> Sequence<T> seq(Enumeration<? extends T> enumeration) {
        return IteratorSeq.of(new EnumerationIterator<T>(enumeration));
    }


    public static <PROPERTY> C.List<PROPERTY> extract(java.util.Collection<?> collection, final String propertyPath) {
        if (collection.isEmpty()) {
            return C.List();
        }
        $.Transformer<Object, PROPERTY> extractor = new $.Transformer<Object, PROPERTY>() {
            @Override
            public PROPERTY transform(Object element) {
                return (PROPERTY) $.getProperty(element, propertyPath);
            }
        };
        return C.List(collection).map(extractor);
    }

    public static <PROPERTY> Sequence<PROPERTY> lazyExtract(Iterable<?> iterable, final String propertyPath) {
        $.Transformer<Object, PROPERTY> extractor = new $.Transformer<Object, PROPERTY>() {
            @Override
            public PROPERTY transform(Object element) {
                return (PROPERTY) $.getProperty(element, propertyPath);
            }
        };
        return transform(iterable, extractor);
    }

    public static <T, R> Sequence<R> transform(Iterable<T> seq, $.Function<? super T, ? extends R> mapper) {
        if (seq instanceof ReversibleSequence) {
            return transform((ReversibleSequence<T>) seq, mapper);
        }
        return new MappedSeq<>(seq, mapper);
    }

    public static <T, R> ReversibleSequence<R> transform(ReversibleSequence<T> seq, $.Function<? super T, ? extends R> mapper
    ) {
        return new ReversibleMappedSeq<>(seq, mapper);
    }

    public static <T> Sequence<T> filter(Sequence<T> seq, $.Function<? super T, Boolean> predicate) {
        return new FilteredSeq<>(seq, predicate);
    }

    @SuppressWarnings("unchecked")
    public static <T> Sequence<T> prepend(T t, Sequence<T> sequence) {
        if (sequence instanceof ReversibleSequence) {
            return prepend(t, (ReversibleSequence) sequence);
        } else {
            return concat(C.List(t), sequence);
        }
    }

    /**
     * Concatenate two {@link Sequence} into one
     *
     * @param s1  the first sequence
     * @param s2  the second sequence
     * @param <T> the element type
     * @return the concatenated sequence
     */
    public static <T> Sequence<T> concat(Sequence<T> s1, Sequence<T> s2) {
        return s1.append(s2);
    }

    /**
     * Concatenate two {@link ReversibleSequence} into one
     *
     * @param s1  the first reversible sequence
     * @param s2  the second reversible sequence
     * @param <T> the element type
     * @return the concatenated reversible sequence
     */
    @SuppressWarnings("unused")
    public static <T> ReversibleSequence<T> concat(ReversibleSequence<T> s1, ReversibleSequence<T> s2) {
        return s1.append(s2);
    }

    /**
     * Concatenate two {@link List} into one.
     *
     * <b>Note</b> if the first list is readonly or immutable an new list instance
     * will be created with elements in both list 1 and list 2 filled in. Otherwise
     * all elemnets from list 2 will be appended to list 1 and return list 1 instance
     *
     * @param l1  list 1
     * @param l2  list 2
     * @param <T> the element type
     * @return a list with elements of both list 1 and list 2
     */
    public static <T> List<T> concat(List<T> l1, List<T> l2) {
        return l1.append(l2);
    }

    /**
     * Concatenate two JDK list into one list
     *
     * <b>Note</b> if the first list is readonly or immutable an new list instance
     * will be created with elements in both list 1 and list 2 filled in. Otherwise
     * all elemnets from list 2 will be appended to list 1 and return list 1 instance
     *
     * @param l1  list 1
     * @param l2  list 2
     * @param <T> the element type
     * @return a list with elements of both list 1 and list 2
     */
    public static <T> List<T> concat(java.util.List<T> l1, java.util.List<T> l2) {
        return C.List(l1).append(l2);
    }

    /**
     * Create an empty immutable set
     *
     * @param <T> the generic type
     * @return the empty set
     */
    public static <T> Set<T> Set() {
        return Nil.set();
    }

    /**
     * Create an immutable set of an array of elements
     *
     * @param ta  the array from which all elements will be added into
     *            the result set
     * @param <T> the element type
     * @return the set contains all elements in the array
     */
    public static <T> Set<T> Set(T... ta) {
        java.util.Set<T> set = new HashSet<>();
        Collections.addAll(set, ta);
        return ImmutableSet.of(set);
    }

    /**
     * Create an immutable set of all elements contained in the collection specified
     *
     * @param col the collection from which elements will be added into the
     *            result set
     * @param <T> the element type
     * @return the set contains all elements in the collection
     */
    public static <T> Set<T> Set(Collection<? extends T> col) {
        return ImmutableSet.of(col);
    }

    /**
     * Create an immutable set from existing set.
     *
     * If the set specified is immutable or readonly then return the set directly
     *
     * @param set the set
     * @param <T> the generic type of set element
     * @return a set as described above
     */
    public static <T> Set<T> Set(Set<? extends T> set) {
        if (set.is(Feature.IMMUTABLE) || set.is(Feature.READONLY)) {
            return $.cast(set);
        }
        return ImmutableSet.of(set);
    }

    /**
     * Create an immutable set of all elements supplied by the iterable specified
     *
     * @param itr the iterable from where elements will be added into the result set
     * @param <T> the element type
     * @return the set contains all elements supplied by the iterable
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> Set(Iterable<? extends T> itr) {
        if (itr instanceof Collection) {
            return Set((Collection<T>) itr);
        }
        java.util.Set<T> set = new HashSet<T>();
        for (T t : itr) set.add(t);
        return ImmutableSet.of(set);
    }

    /**
     * Returns a set that contains all elements in `col1` and all elements in `col2`
     * @param col1 the first collection
     * @param col2 the second collection
     * @param <T> the collection element type
     * @return a set as described above
     */
    public static <T> Set<T> unionOf(Collection<? extends T> col1, Collection<? extends T> col2) {
        return C.Set(col1).with(col2);
    }

    /**
     * Returns a set that contains all elements in `col1`, all elements in `col2`, all elements in `col3` and
     * all elements in rest collections
     * @param col1 the first collection
     * @param col2 the second collection
     * @param col3 the third collection
     * @param otherCols the rest collections
     * @param <T> the collection element type
     * @return a set as described above
     */
    public static <T> Set<T> unionOf(Collection<? extends T> col1, Collection<? extends T> col2, Collection<? extends T> col3, Collection<? extends T>... otherCols) {
        Set<T> union = C.Mutable.Set(col1);
        union.addAll(col2);
        union.addAll(col3);
        for (Collection<? extends T> col : otherCols) {
            union.addAll(col);
        }
        return C.Set(union);
    }

    /**
     * Returns a set that contains elements exists in both `col1` and `col2`
     * @param col1 the first collection
     * @param col2 the second collection
     * @param <T> the collection element type
     * @return a set as described above
     */
    public static <T> Set<T> intercectionOf(Collection<? extends T> col1, Collection<? extends T> col2) {
        return C.Set(col1).withIn(col2);
    }

    /**
     * Returns a set that contains elements in both `col1`, `col2`, `col3` and　rest collections
     * @param col1 the first collection
     * @param col2 the second collection
     * @param col3 the third collection
     * @param otherCols the rest collections
     * @param <T> the collection element type
     * @return a set as described above
     */
    public static <T> Set<T> interceptionOf(Collection<? extends T> col1, Collection<? extends T> col2, Collection<? extends T> col3, Collection<? extends T>... otherCols) {
        Set<T> interception = C.Mutable.Set(col1);
        interception.retainAll(col2);
        interception.retainAll(col3);
        for (Collection<? extends T> col : otherCols) {
            interception.retainAll(col);
        }
        return C.Set(interception);
    }

    /**
     * Returns a {@link Map._Builder} with the first key specified
     *
     * A general usage of this method
     *
     * ```
     * C.Map<String, Integer> map = C.map("one").to(1)
     *      .map("two").to(2)
     *      .map("three").to(3);
     * ```
     *
     * @param key the key
     * @param <K> the generic type of key
     * @return the map builder
     */
    public static <K, V> Map<K, V>._Builder map(K key) {
        Map<K, V> map = Map();
        return map.map(key);
    }

    /**
     * Create a immutable {@link Map} from elements specified in an array.
     * <p>Example</p>
     * <pre>
     *     Map&lt;String, Integer&gt; scores = C.Map("Tom", 80, "Peter", 93, ...);
     * </pre>
     * <p>The above code will create an immutable Map with the following entries</p>
     * <ul>
     * <li>(Tom, 80)</li>
     * <li>(Peter, 93)</li>
     * <li>...</li>
     * </ul>
     * <p><b>Note</b> the array size must be an even number, otherwise {@link IndexOutOfBoundsException}
     * will be thrown out</p>
     *
     * @param args the argument array specifies the entries
     * @param <K>  the key type
     * @param <V>  the value type
     * @return an immutable Map contains of specified entries
     */
    public static <K, V> Map<K, V> Map(Object... args) {
        if (null == args || args.length == 0) {
            return Nil.EMPTY_MAP;
        }
        return new Map<>(true, args);
    }

    public static <K, V> Map<K, V> Map(Collection<$.Tuple<K, V>> kvCol) {
        Map<K, V> map = Mutable.Map();
        for ($.Tuple<K, V> entry : kvCol) {
            map.put(entry._1, entry._2);
        }
        return new Map<>(true, map);
    }

    /**
     * Create an immutable {@link java.util.Map} from existing {@link java.util.Map}
     *
     * @param map the Map from which entries will be put into the new immutable Map
     * @param <K> the key type
     * @param <V> the value type
     * @return an immutable Map of the existing Map
     */
    public static <K, V> Map<K, V> Map(java.util.Map<? extends K, ? extends V> map) {
        if (null == map) {
            return Nil.EMPTY_MAP;
        }
        return new Map(true, map);
    }
    // --- eof factory methods ---

    // --- utility methods ---

    /**
     * Check if a {@link Traversable} structure is read only. A
     * Traversable is considered to be read only structure when
     * {@code is(Feature.READONLY) || is(Feature.IMMUTABLE}
     * evaluate to {@code true}
     *
     * @param t the structure to be checked
     * @return {@code true} if the structure is read only
     * or immutable
     */
    @SuppressWarnings("unused")
    public static boolean isReadOnly(Traversable<?> t) {
        return t.is(Feature.READONLY) || t.is(Feature.IMMUTABLE);
    }

    /**
     * Check if a {@link Traversable} structure is immutable.
     *
     * @param t the traversable strucure to be checked
     * @return {@code true} if the traversable is immutable
     */
    @SuppressWarnings("unused")
    public static boolean isImmutable(Traversable<?> t) {
        return t.is(Feature.IMMUTABLE);
    }

    /**
     * Run visitor function on each element supplied by the iterable. The visitor function can throw out
     * {@link org.osgl.Lang.Break} if it need to break the loop.
     * <p>Note if {@link NotAppliedException} thrown out by visitor function, it will be ignored
     * and keep looping through the Map entry set. It is kind of {@code continue} mechanism in a funcitonal
     * way</p>
     *
     * @param iterable supply the element to be applied to the visitor function
     * @param visitor  the function called on element provided by the iterable
     * @param <T>      the generic type of the iterable elements
     * @throws $.Break break the loop
     */
    //TODO: implement forEach iteration in parallel
    public static <T> void forEach(Iterable<? extends T> iterable, $.Visitor<? super T> visitor) throws $.Break {
        for (T t : iterable) {
            try {
                visitor.apply(t);
            } catch (NotAppliedException e) {
                // ignore
            }
        }
    }

    /**
     * Run visitor function on each element supplied by the iterator. The visitor function can throw out
     * {@link org.osgl.Lang.Break} if it need to break the loop.
     * <p>Note if {@link NotAppliedException} thrown out by visitor function, it will be ignored
     * and keep looping through the Map entry set. It is kind of {@code continue} mechanism in a funcitonal
     * way</p>
     *
     * @param iterator iterator provides elements to be applied to the visitor function
     * @param visitor  the function applied on the element
     * @param <T>      the generic type of the element
     */
    public static <T> void forEach(Iterator<? extends T> iterator, $.Visitor<? super T> visitor) {
        while (iterator.hasNext()) {
            T t = iterator.next();
            visitor.apply(t);
        }
    }

    /**
     * Run indexedVisitor function on all key/value pair in a given Map. The indexedVisitor function can
     * throw out {@link org.osgl.Lang.Break} if it need to break the loop.
     * <p>Note if {@link NotAppliedException} thrown out by indexedVisitor function, it will be ignored
     * and keep looping through the Map entry set. It is kind of {@code continue} mechanism in a funcitonal
     * way</p>
     *
     * @param map            the Map in which enties will be applied to the indexedVisitor function
     * @param indexedVisitor the function that takes (key,value) pair
     * @param <K>            the generic type of Key
     * @param <V>            the generic type of Value
     * @throws $.Break the {@link org.osgl.Lang.Break} with payload throwed out by indexedVisitor function to break to loop
     */
    public static <K, V> void forEach(java.util.Map<K, V> map, $.IndexedVisitor<? super K, ? super V> indexedVisitor) throws $.Break {
        for (java.util.Map.Entry<K, V> entry : map.entrySet()) {
            try {
                indexedVisitor.apply(entry.getKey(), entry.getValue());
            } catch (NotAppliedException e) {
                // ignore
            }
        }
    }

    /**
     * Convert an {@link Enumeration} to {@link Iterable}
     * @param enumeration the enumeration
     * @param <T> the generic type of the element
     * @return an Iterable that backed by the enumeration
     */
    public static <T> Iterable<T> toIterable(final Enumeration<T> enumeration) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new EnumerationIterator<>(enumeration);
            }
        };
    }

    /**
     * Convert an {@link Iterator} to {@link Iterable}
     * @param iterator the iterator
     * @param <T> the element generic type
     * @return the iterable backed by the iterator
     */
    public static <T> Iterable<T> toIterable(final Iterator<T> iterator) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return iterator;
            }
        };
    }

    /**
     * Convert an {@link Enumeration} to {@link Iterator}
     * @param enumeration the enumeration
     * @param <T> the element generic type
     * @return the Iterator backed by the enumeration
     */
    public static <T> Iterator<T> toIterator(Enumeration<T> enumeration) {
        return new EnumerationIterator<>(enumeration);
    }

    /**
     * Convert a Map entry into a {@link $.Binary}
     * @param mapEntry the map entry
     * @param <K> the generic type of entry key
     * @param <V> the generic type of entry value
     * @return a binary pair of key and value
     */
    public static <K, V> $.Binary<K, V> toBinary(java.util.Map.Entry<K, V> mapEntry) {
        return $.T2(mapEntry.getKey(), mapEntry.getValue());
    }

    // --- eof utility methods ---

    // --- Mutable collection/Map constructors
    public enum Mutable {
        ;

        /**
         * Create a mutable list
         *
         * @param <T> the generic type of the list element
         * @return the List as described above
         */
        public static <T> List<T> List() {
            return new DelegatingList<>(10);
        }

        /**
         * Create a mutable list with initial capacity specified
         * @param initialCapacity the initial capacity hint
         * @param <T>
         * @return the list as described
         */
        public static <T> List<T> sizedList(int initialCapacity) {
            return new DelegatingList<>(initialCapacity);
        }

        /**
         * Returns an empty mutable list of type specified by class `c`
         *
         * @param c   the class specify the list element type
         * @param <T> the generic type of the list element
         * @return the list as described above
         */
        public static <T> List<T> emptyList(Class<T> c) {
            return new DelegatingList<>(10);
        }


        /**
         * Creates an mutable list from an array of elements.
         *
         * @param elements an array of elements
         * @param <T>      the element type
         * @return the list as described above
         */
        public static <T> List<T> List(T... elements) {
            List<T> list = new DelegatingList<>(elements.length);
            for (T element : elements) {
                list.add(element);
            }
            return list;
        }

        /**
         * Create an mutable Boolean list from a boolean array.
         *
         * @param elements a boolean array
         * @return the list as described above
         */
        public static List<Boolean> List(boolean... elements) {
            List<Boolean> list = new DelegatingList<>(elements.length);
            for (Boolean element : elements) {
                list.add(element);
            }
            return list;
        }

        /**
         * Create an mutable Byte list from a byte array.
         *
         * @param elements a byte array
         * @return the list as described above
         */
        public static List<Byte> List(byte... elements) {
            List<Byte> list = new DelegatingList<>(elements.length);
            for (Byte element : elements) {
                list.add(element);
            }
            return list;
        }

        /**
         * Create an mutable Character list from a char array.
         *
         * @param elements a char array
         * @return the list as described above
         */
        public static List<Character> List(char... elements) {
            List<Character> list = new DelegatingList<>(elements.length);
            for (Character element : elements) {
                list.add(element);
            }
            return list;
        }

        /**
         * Create an mutable Short list from a short array.
         *
         * @param elements a short array
         * @return a List as described above
         */
        public static List<Short> List(short... elements) {
            List<Short> list = new DelegatingList<>(elements.length);
            for (Short element : elements) {
                list.add(element);
            }
            return list;
        }

        /**
         * Create an mutable Integer list from a int array.
         *
         * @param elements a int array
         * @return a List as described above
         */
        public static List<Integer> List(int... elements) {
            List<Integer> list = new DelegatingList<>(elements.length);
            for (Integer element : elements) {
                list.add(element);
            }
            return list;
        }

        /**
         * Create an mutable Long list from a long array.
         *
         *
         * @param elements a long array
         * @return a List as described above
         */
        public static List<Long> List(long... elements) {
            List<Long> list = new DelegatingList<>(elements.length);
            for (Long element : elements) {
                list.add(element);
            }
            return list;
        }

        /**
         * Create an mutable Float list from a float array.
         *
         * Note the array will not be copied, instead it will
         * be used directly as the backing data for the list.
         * To create an list with a copy of the array specified.
         * Use the {@link #toList(float[])} method
         *
         * @param elements a float array
         * @return a List as described above
         */
        public static List<Float> List(float... elements) {
            List<Float> list = new DelegatingList<>(elements.length);
            for (Float element : elements) {
                list.add(element);
            }
            return list;
        }

        /**
         * Create an mutable Double list from a double array.
         *
         * @param elements a long array
         * @return a List as described above
         */
        public static List<Double> List(double... elements) {
            List<Double> list = new DelegatingList<>(elements.length);
            for (Double element : elements) {
                list.add(element);
            }
            return list;
        }

        /**
         * Create an mutable List from an iterable
         * @param iterable the iterable
         * @param <T> the generic type of the iterable
         * @return an List that contains all elements in the iterable
         */
        public static <T> List<T> List(Iterable<? extends T> iterable) {
            return List(iterable.iterator());
        }

        /**
         * Create an mutable List from an iterator
         * @param iterator the iterator
         * @param <T> the generic type of the iterator
         * @return an List that contains all elements the iterator iterated
         */
        public static <T> List<T> List(Iterator<? extends T> iterator) {
            List<T> list = List();
            while (iterator.hasNext()) {
                list.add(iterator.next());
            }
            return list;
        }

        /**
         * Create an mutable List from an enumeration
         * @param enumeration the enumeration
         * @param <T> the generic type of the enumeration
         * @return an List that contains all elements in the enumeration
         */
        public static <T> List<T> List(Enumeration<? extends T> enumeration) {
            return List(toIterator(enumeration));
        }

        /**
         * Create an mutable List from an collection
         * @param col the collection
         * @param <T> the generic type of the collection
         * @return an List that contains all elements in the collection
         */
        public static <T> List<T> List(Collection<? extends T> col) {
            return new DelegatingList<>(col);
        }

        /**
         * Create a mutable empty set
         *
         * @param <T> the element type
         * @return a set as described above
         */
        public static <T> Set<T> Set(T ... elements) {
            return new DelegatingSet<T>().with(elements);
        }

        /**
         * Create a mutable set with all elements contained in the collection
         * specified
         *
         * @param col the collection from which all elements will be added into
         *            the result set
         * @param <T> the element type
         * @return a set as described above
         */
        public static <T> Set<T> Set(Collection<? extends T> col) {
            return new DelegatingSet<>(col);
        }

        /**
         * Create an mutable Map from JDK map
         * @param map the JDK map
         * @param <K> generic type of key
         * @param <V> generic type of value
         * @return the map as described above
         */
        public static <K, V> Map<K, V> Map(java.util.Map<? extends K, ? extends V> map) {
            return new Map(false, map);
        }


        /**
         * Create a mutable {@link Map} from elements specified in an array.
         * <p>Example</p>
         * <pre>
         *     Map&lt;String, Integer&gt; scores = C.Map("Tom", 80, "Peter", 93, ...);
         * </pre>
         * <p>The above code will create an immutable Map with the following entries</p>
         * <ul>
         * <li>(Tom, 80)</li>
         * <li>(Peter, 93)</li>
         * <li>...</li>
         * </ul>
         * <p><b>Note</b> the array size must be an even number, otherwise {@link IndexOutOfBoundsException}
         * will be thrown out</p>
         *
         * @param args the argument array specifies the entries
         * @param <K>  the key type
         * @param <V>  the value type
         * @return an mutable Map contains of specified entries
         */
        public static <K, V> Map<K, V> Map(Object... args) {
            return new Map<>(false, args);
        }
    }

    /**
     * the namespace of function definitions relevant to Collection manipulation
     */
    public enum F {
        ;

        public static <T> $.Transformer<Iterable<T>, Collection<T>> asCollection() {
            return new Lang.Transformer<Iterable<T>, Collection<T>>() {
                @Override
                public Collection<T> transform(Iterable<T> iterable) {
                    return C.asCollection(iterable);
                }
            };
        }

        /**
         * Returns a predicate function that check if the argument is contained in
         * the collection specified
         *
         * @param collection the collection to be checked on against the argument when applying the prediate
         * @param <T>        the generic type of the element of the collection
         * @return a predicate function
         * @see Collection#contains(Object)
         * @see #contains(Object)
         */
        public static <T> $.Predicate<T> containsIn(final Collection<? super T> collection) {
            return new $.Predicate<T>() {
                @Override
                public boolean test(T t) throws NotAppliedException, $.Break {
                    return collection.contains(t);
                }
            };
        }

        /**
         * Returns a predicate function that check if the argument (collection) contains the
         * element specified
         *
         * @param element the element to be checked
         * @param <T>     the type of the element
         * @return the function that do the check
         * @see Collection#contains(Object)
         * @see #containsIn(Collection)
         */
        public static <T> $.Predicate<Collection<? super T>> contains(final T element) {
            return new $.Predicate<Collection<? super T>>() {
                @Override
                public boolean test(Collection<? super T> collection) {
                    return collection.contains(element);
                }
            };
        }

        /**
         * Returns a predicate function that check if all element in the argument (a collection) contained
         * in the collection specified
         *
         * @param collection the collection to be checked on against all elements in the argument when
         *                   applying the function
         * @param <T>        the generic type of the element of the collection or argument
         * @return the function that do the check
         * @see Collection#containsAll(Collection)
         * @see #containsAll(Collection)
         */
        @SuppressWarnings("unused")
        public static <T> $.Predicate<Collection<? extends T>> allContainsIn(final Collection<? super T> collection) {
            return new $.Predicate<Collection<? extends T>>() {
                @Override
                public boolean test(Collection<? extends T> theCollection) {
                    return collection.containsAll(theCollection);
                }
            };
        }

        /**
         * Returns a predicate function that check if all element in the collection specified are contained in
         * the argument collection
         *
         * @param collection the collection in which all elements will be checked if contained in the argument
         *                   collection when applying the function
         * @param <T>        the element type
         * @return the function that do the check
         * @see Collection#containsAll(Collection)
         * @see #allContainsIn(Collection)
         */
        @SuppressWarnings("unused")
        public static <T> $.Predicate<Collection<? super T>> containsAll(final Collection<? extends T> collection) {
            return new $.Predicate<Collection<? super T>>() {
                @Override
                public boolean test(Collection<? super T> theCollection) {
                    return theCollection.contains(collection);
                }
            };
        }

        /**
         * Returns a function that add the argument to a collection specified and returns
         * {@code true} if added successfully or {@code false} otherwise
         *
         * @param destination the collection into which the argument to be added
         * @param <T>         the generic type of the collection elements
         * @return a function that do the add operation
         * @see Collection#add(Object)
         * @see #add(Object)
         */
        public static <T> $.Predicate<T> addTo(final Collection<? super T> destination) {
            return new $.Predicate<T>() {
                @Override
                public boolean test(T t) throws NotAppliedException, $.Break {
                    return destination.add(t);
                }
            };
        }

        /**
         * Returns a function that add the specified element into the argument collection and
         * return {@code true} if add successfully or {@code false} otherwise
         *
         * @param element the element to be added when applying the function
         * @param <T>     the element type
         * @return the function
         * @see Collection#add(Object)
         * @see #addTo(Collection)
         */
        public static <T> $.Predicate<Collection<? super T>> add(final T element) {
            return new $.Predicate<Collection<? super T>>() {
                @Override
                public boolean test(Collection<? super T> collection) {
                    return collection.add(element);
                }
            };
        }

        /**
         * Returns a function that add the argument into the specified list at specified position.
         * the function returns {@code true} if added successfully or {@code false} otherwise
         *
         * @param destination a list into which the argument to be added
         * @param index       specify the position where the argument can be added
         * @param <L>         the generic type of the list
         * @param <T>         the generic type of the list element
         * @return the function that do the add operation
         * @see java.util.List#add(int, Object)
         * @see #add(int, Object)
         */
        @SuppressWarnings("unused")
        public static <L extends List<? super T>, T> $.F1<T, L> addTo(final int index, final L destination) {
            return new $.F1<T, L>() {
                @Override
                public L apply(T t) throws NotAppliedException, $.Break {
                    destination.add(index, t);
                    return destination;
                }
            };
        }

        /**
         * Returns a function that add specified element into the argument list at specified position. The
         * function returns the argument list after element added
         *
         * @param index   the location at where the element should be added to
         * @param element the element the be added to the argument list
         * @param <L>     the list type
         * @param <T>     the element type
         * @return the function
         * @see java.util.List#add(int, Object)
         * @see #addTo(int, List)
         */
        public static <L extends List<? super T>, T> $.F1<L, L> add(final int index, final T element) {
            return new $.F1<L, L>() {
                @Override
                public L apply(L list) throws NotAppliedException, Lang.Break {
                    list.add(index, element);
                    return list;
                }
            };
        }

        /**
         * Returns a function that takes argument of type {@link Collection} and add all elements inside
         * into the specified collection. The function returns {@code true} if the collection specified
         * has been changed as a result of adding elements
         *
         * @param destination the collection into which all elements in the argument collection will be added
         *                    when applying the function
         * @param <T>         the generic type of the collection element and the argument collection element
         * @return the function that add all elements from iterable argument into the collection specified
         * @see Collection#addAll(Collection)
         * @see #addAll(Collection)
         */
        @SuppressWarnings({"unchecked"})
        public static <T> $.Predicate<Iterable<? extends T>> addAllTo(final Collection<? super T> destination) {
            return new $.Predicate<Iterable<? extends T>>() {
                @Override
                public boolean test(Iterable<? extends T> source) throws NotAppliedException, $.Break {
                    if (source instanceof Collection) {
                        return destination.addAll((Collection) (source));
                    }
                    return destination.addAll(C.List(source));
                }
            };
        }

        /**
         * Returns a function that add all elements in the source collection specified into the destination
         * collection as argument. The function returns {@code true} if the argument collection has been
         * changes as a result of call.
         *
         * @param source the collection from which the elements will be added into the argument collection
         *               when applying the function
         * @param <T>    the element type
         * @return the function the perform the add operation
         * @see Collection#addAll(Collection)
         * @see #addAllTo(Collection)
         */
        @SuppressWarnings({"unchecked"})
        public static <T> $.Predicate<Collection<? super T>> addAll(final Collection<? extends T> source) {
            return new $.Predicate<Collection<? super T>>() {
                @Override
                public boolean test(Collection<? super T> destination) {
                    return destination.addAll(source);
                }
            };
        }

        /**
         * Returns a function that add all elements from the argument collection into the destination list specified
         * at the position specified
         *
         * @param index       the position at where the element shall be inserted into the destination list
         * @param destination the list into which the elements will be added
         * @param <T>         the element type
         * @return the function that do the add operation
         * @see java.util.List#addAll(int, Collection)
         * @see #addAll(int, Collection)
         */
        @SuppressWarnings({"unused"})
        public static <T> $.Predicate<Collection<? extends T>> addAllTo(final int index, final List<? super T> destination) {
            if (0 > index || destination.size() < index) {
                throw new IndexOutOfBoundsException();
            }
            return new $.Predicate<Collection<? extends T>>() {
                @Override
                public boolean test(Collection<? extends T> collection) throws NotAppliedException, $.Break {
                    return destination.addAll(index, collection);
                }
            };
        }

        /**
         * Returns a function that add all elements from the source collection specified into the argument list at
         * the position specified
         *
         * @param index  the position where the element should be insert in the argument list
         * @param source the collection from which the elements to be get to added into the argument list
         * @param <T>    the element type
         * @return the function that do the add operation
         * @see java.util.List#addAll(int, Collection)
         * @see #addAllTo(int, List)
         */
        @SuppressWarnings({"unused"})
        public static <T> $.Predicate<List<? super T>> addAll(final int index, final Collection<? extends T> source) {
            return new $.Predicate<List<? super T>>() {
                @Override
                public boolean test(List<? super T> destination) {
                    return destination.addAll(index, source);
                }
            };
        }


        /**
         * Returns a function that remove the argument from a collection specified.
         * <p>The function returns {@code true} if argument removed successfully or
         * {@code false} otherwise</p>
         *
         * @param collection the collection from which the argument to be removed
         *                   when applying the function returned
         * @return the function that remove element from the collection
         * @see Collection#remove(Object)
         * @see #remove(Object)
         */
        @SuppressWarnings("unused")
        public static <T> $.Predicate<T> removeFrom(final Collection<? super T> collection) {
            return new $.Predicate<T>() {
                @Override
                public boolean test(T t) throws NotAppliedException, $.Break {
                    return collection.remove(t);
                }
            };
        }

        /**
         * Returns a function that remove the element specified from the argument collection. The
         * function returns {@code true} if the argument collection changed as a result of the call.
         *
         * @param toBeRemoved the element to be removed from the argument when applying the function
         * @param <T>         the element type
         * @return the function that do removing
         * @see Collection#remove(Object)
         * @see #removeFrom(Collection)
         */
        @SuppressWarnings("unused")
        public static <T> $.Predicate<Collection<? super T>> remove(final T toBeRemoved) {
            return new $.Predicate<Collection<? super T>>() {
                @Override
                public boolean test(Collection<? super T> collection) {
                    return collection.remove(toBeRemoved);
                }
            };
        }

        /**
         * Returns a function that remove all elements in the argument collection from
         * the {@code fromCollection} specified. The function returns {@code true} if
         * the fromCollection changed as a result of call
         *
         * @param fromCollection the collection from which elements will be removed
         * @param <T>            the element type
         * @return the function
         * @see Collection#removeAll(Collection)
         * @see #removeAll(Collection)
         */
        @SuppressWarnings("unused")
        public static <T> $.Predicate<Collection<? extends T>> removeAllFrom(final Collection<? super T> fromCollection) {
            return new Lang.Predicate<Collection<? extends T>>() {
                @Override
                public boolean test(Collection<? extends T> theCollection) {
                    return fromCollection.removeAll(theCollection);
                }
            };
        }

        /**
         * Returns a function that remove all elements in the {@code source} collection from the
         * argument collection. The function returns {@code true} if the argument collection changed
         * as a result of call
         *
         * @param source the collection in which elements will be used to remove from argument collection
         * @param <T>    the element type
         * @return the function
         * @see Collection#removeAll(Collection)
         * @see #removeAllFrom(Collection)
         */
        public static <T> $.Predicate<Collection<? super T>> removeAll(final Collection<? extends T> source) {
            return new Lang.Predicate<Collection<? super T>>() {
                @Override
                public boolean test(Collection<? super T> collection) {
                    return collection.removeAll(source);
                }
            };
        }


        /**
         * Returns a function that retains only elements contained in the argument collection in the
         * collection specified. The function returns {@code true} if the collection specified
         * changed as a result of call
         *
         * @param collection the collection in which elements will be retained/removed
         * @param <T>        the element type
         * @return the function as described
         * @see Collection#retainAll(Collection)
         * @see #retainAll(Collection)
         */
        @SuppressWarnings({"unused"})
        public static <T> $.Predicate<Collection<? extends T>> retainAllIn(final Collection<? super T> collection) {
            return new $.Predicate<Collection<? extends T>>() {
                @Override
                public boolean test(Collection<? extends T> theCollection) {
                    return collection.retainAll(theCollection);
                }
            };
        }

        /**
         * Returns a function that retains only elements contained in the specified collection in
         * the argument collection. The function returns {@code true} if argument collection changes
         * as a result of the call
         *
         * @param collection the collection in which elements will be used to check if argument collection
         *                   element shall be retained or not
         * @param <T>        the element type
         * @return the function as described above
         * @see Collection#retainAll(Collection)
         * @see #retainAllIn(Collection)
         */
        @SuppressWarnings({"unused"})
        public static <T> $.Predicate<Collection<? super T>> retainAll(final Collection<? extends T> collection) {
            return new $.Predicate<Collection<? super T>>() {
                @Override
                public boolean test(Collection<? super T> theCollection) {
                    return theCollection.retainAll(collection);
                }
            };
        }

        /**
         * Returns a function that prepend an element to a deque specified and return the
         * deque instance
         *
         * @param deque the deque to which the element argument will be prepend to
         * @param <T>   the element type
         * @return the function as described
         * @see Deque#addFirst(Object)
         * @see #dequePrepend(Object)
         */
        @SuppressWarnings({"unused"})
        public static <T> $.F1<T, Deque<? super T>> prependTo(final Deque<? super T> deque) {
            return new $.F1<T, Deque<? super T>>() {
                @Override
                public Deque<? super T> apply(T t) throws NotAppliedException, $.Break {
                    deque.addFirst(t);
                    return deque;
                }
            };
        }

        /**
         * Returns a function that prepend specified element to argument deque
         *
         * @param element the element to be added to the head of the argument (deque type)
         * @param <T>     the element type
         * @return the function as described
         * @see Deque#addFirst(Object)
         * @see #prependTo(Deque)
         */
        @SuppressWarnings("unused")
        public static <T> $.Processor<Deque<? super T>> dequePrepend(final T element) {
            return new $.Processor<Deque<? super T>>() {
                @Override
                public void process(Deque<? super T> deque) throws Lang.Break, NotAppliedException {
                    deque.addFirst(element);
                }
            };
        }

        /**
         * Returns a function that append the argument to a {@link Deque} specified
         *
         * @param deque the deque to which the argument shall be append when applying the function returned
         * @param <T>   the generic type of the argument/deque element
         * @return the function that do the append operation
         * @see Deque#add(Object)
         * @see #dequeAppend(Object)
         */
        @SuppressWarnings("unused")
        public static <T> $.F1<T, Deque<? super T>> appendTo(final Deque<? super T> deque) {
            return new $.F1<T, Deque<? super T>>() {
                @Override
                public Deque<? super T> apply(T t) throws NotAppliedException, $.Break {
                    deque.addLast(t);
                    return deque;
                }
            };
        }

        /**
         * Returns a function that append specified element to argument deque
         *
         * @param element the element to be added to the tail of the argument (deque type)
         * @param <T>     the element type
         * @return the function as described
         * @see Deque#add(Object)
         * @see #appendTo(Deque)
         */
        @SuppressWarnings("unused")
        public static <T> $.Processor<Deque<? super T>> dequeAppend(final T element) {
            return new $.Processor<Deque<? super T>>() {
                @Override
                public void process(Deque<? super T> deque) throws Lang.Break, NotAppliedException {
                    deque.add(element);
                }
            };
        }

        /**
         * Returns a function that prepend the argument to a {@link Sequence} specified
         *
         * @param sequence the sequence to which the argument shall be prepend whene applying the function
         * @param <T>      the generic type of the argument/sequence element
         * @return the function that do the prepend operation
         * @see Sequence#prepend(Object)
         * @see #sequencePrepend(Object)
         */
        @SuppressWarnings("unused")
        public static <T> $.F1<T, Sequence<? super T>> prependTo(final Sequence<? super T> sequence) {
            return new $.F1<T, Sequence<? super T>>() {
                @Override
                public Sequence<? super T> apply(T t) throws NotAppliedException, $.Break {
                    sequence.prepend(t);
                    return sequence;
                }
            };
        }

        /**
         * Returns a function that preppend specified element to argument sequence
         *
         * @param element the element to be added to the head of the argument (sequence type)
         * @param <T>     the element type
         * @return the function as described
         * @see Sequence#prepend(Object)
         * @see #prependTo(Sequence)
         */
        @SuppressWarnings("unused")
        public static <T> $.Processor<Sequence<? super T>> sequencePrepend(final T element) {
            return new Lang.Processor<Sequence<? super T>>() {
                @Override
                public void process(Sequence<? super T> sequence) throws Lang.Break, NotAppliedException {
                    sequence.prepend(element);
                }
            };
        }

        /**
         * Returns a function that append the argument to a {@link Sequence} specified
         * <p><b>Note</b> the function returns the sequence with the argument been removed</p>
         *
         * @param sequence the sequence to which the argument shall be append when applying the function
         * @param <T>      the generic type of the argument/sequence element
         * @return the function that do the append operation
         * @see Sequence#append(Iterable)
         * @see #sequenceAppend(Object)
         */
        @SuppressWarnings("unused")
        public static <T> $.F1<T, Sequence<? super T>> appendTo(final Sequence<? super T> sequence) {
            return new $.F1<T, Sequence<? super T>>() {
                @Override
                public Sequence<? super T> apply(T t) throws NotAppliedException, $.Break {
                    sequence.append(t);
                    return sequence;
                }
            };
        }


        /**
         * Returns a function that append specified element to argument sequence
         *
         * @param element the element to be added to the tail of the argument (sequence type)
         * @param <T>     the element type
         * @return the function as described
         * @see Sequence#append(Iterable)
         * @see #appendTo(Sequence)
         */
        @SuppressWarnings("unused")
        public static <T> $.Processor<Sequence<? super T>> sequenceAppend(final T element) {
            return new Lang.Processor<Sequence<? super T>>() {
                @Override
                public void process(Sequence<? super T> sequence) throws Lang.Break, NotAppliedException {
                    sequence.append(element);
                }
            };
        }

        /**
         * Returns a function that apply the visitor function specified on the argument (iterable)
         *
         * @param visitor the function to be used to loop through the argument
         * @param <T>     the element type
         * @return the function as described
         * @see C#forEach(Iterable, Lang.Visitor)
         */
        @SuppressWarnings("unused")
        public static <T> $.F1<Iterable<? extends T>, Void> forEachIterable(final $.Visitor<? super T> visitor) {
            return new $.F1<Iterable<? extends T>, Void>() {
                @Override
                public Void apply(Iterable<? extends T> iterable) throws NotAppliedException, $.Break {
                    C.forEach(iterable, visitor);
                    return null;
                }
            };
        }

        /**
         * Defines the function that apply to a Map and return the values collection of the map
         */
        public static final $.Function<java.util.Map, Collection> MAP_VALUES = new $.Transformer<java.util.Map, Collection>() {
            @Override
            public Collection transform(java.util.Map map) {
                return map.values();
            }
        };

        /**
         * A type cast version of {@link #MAP_VALUES}
         * @param <K> the generic type of map key
         * @param <V> the generic type of map value
         * @return the function that apply to a Map and return the values collection of the map
         */
        public static <K, V> $.Function<java.util.Map<K, V>, Collection<V>> mapValues() {
            return $.cast(MAP_VALUES);
        }

        /**
         * Defines the function that apply to a Map and return the key set of the map
         */
        public static final $.Function<java.util.Map, Set> MAP_KEYS = new $.Transformer<java.util.Map, Set>() {
            @Override
            public Set transform(java.util.Map map) {
                return C.Set(map.keySet());
            }
        };

        /**
         * A type cast version of {@link #MAP_KEYS}
         * @param <K> the generic type of map key
         * @param <V> the generic type of map value
         * @return the function that apply to a Map and return the key set of the map
         */
        public static <K, V> $.Function<java.util.Map<K, V>, Set<V>> mapKeys() {
            return $.cast(MAP_KEYS);
        }

        /**
         * A function that apply to {@link java.util.Map.Entry} and returns a {@link Lang.Binary}
         */
        public static final $.Function<java.util.Map.Entry, $.Binary> MAP_ENTRY_TO_BINARY = new $.Transformer<java.util.Map.Entry, $.Binary>() {
            @Override
            public $.Binary transform(java.util.Map.Entry entry) {
                return C.toBinary(entry);
            }
        };

        /**
         * A type cast version of {@link #MAP_ENTRY_TO_BINARY}
         * @param <K> the map entry key type
         * @param <V> the map entry value type
         * @return a function apply to {@link java.util.Map.Entry} and returns a {@link Lang.Binary}
         */
        public static <K, V> $.Function<java.util.Map.Entry<K, V>, $.Binary<K, V>> mapEntryToBinary() {
            return $.cast(MAP_ENTRY_TO_BINARY);
        }

    }

}
