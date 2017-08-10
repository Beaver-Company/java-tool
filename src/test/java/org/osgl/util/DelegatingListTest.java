package org.osgl.util;

public class DelegatingListTest extends ListTestBase {

    @Override
    protected C.List<Integer> prepareData(int... ia) {
        C.List<Integer> l = prepareEmptyData();
        l.append(C.List(ia));
        return l;
    }

    @Override
    protected C.List<Integer> prepareEmptyData() {
        return C.Mutable.List();
    }

    @Override
    protected <T> C.List<T> prepareTypedData(T... ta) {
        C.List<T> l = C.Mutable.sizedList(ta.length);
        for (T t : ta) {
            l.add(t);
        }
        return l;
    }
}
