package org.osgl.util.algo;

import org.osgl.$;

import java.util.Comparator;

public interface ArraySort<T> extends ArrayAlgorithm, $.Func4<T[], Integer, Integer, Comparator<T>, T[]> {
    T[] sort(T[] ts, int from, int to, Comparator<T> comp);
}
