package org.osgl.util;

import org.osgl.$;

import java.util.Iterator;

class FlatMappedReversibleSeq<T, R> extends ReversibleSeqBase<R> {
    private C.ReversibleSequence<? extends T> data;
    private $.F1<? super T, ? extends Iterable<? extends R>> mapper;

    FlatMappedReversibleSeq(C.ReversibleSequence<? extends T> rseq, $.Function<? super T, ? extends Iterable<? extends R>> mapper) {
        E.NPE(rseq, mapper);
        this.data = rseq;
        this.mapper = $.f1(mapper);
    }

    @Override
    public int size() throws UnsupportedOperationException {
        return data.size();
    }

    @Override
    public Iterator<R> iterator() {
        return Iterators.flatMap(data.iterator(), mapper);
    }

    @Override
    public Iterator<R> reverseIterator() {
        return Iterators.flatMap(data.reverseIterator(), mapper);
    }

    static <T, R> C.ReversibleSequence<R>
    of(C.ReversibleSequence<? extends T> data, $.Function<? super T, ? extends Iterable<? extends R>> mapper) {
        return new FlatMappedReversibleSeq<T, R>(data, mapper);
    }
}
