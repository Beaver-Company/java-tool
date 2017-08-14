package org.osgl.util.algo;

public interface ArrayAlgorithm extends Algorithm {
    enum Util {
        ;
        public static void checkIndex(Object[] array, int index) {
            if (index < 0 || index >= array.length) {
                throw new IndexOutOfBoundsException();
            }
        }

        public static void checkIndex(Object[] array, int from, int to) {
            if (array.length == 0) return;
            if (from < 0 || from >= array.length || to < 0 || to > array.length) {
                throw new IndexOutOfBoundsException();
            }
        }
    }
}
