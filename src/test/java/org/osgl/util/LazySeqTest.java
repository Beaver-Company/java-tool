package org.osgl.util;

/*-
 * #%L
 * Java Tool
 * %%
 * Copyright (C) 2014 - 2017 OSGL (Open Source General Library)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.osgl.$;
import org.osgl.exception.NotAppliedException;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: luog
 * Date: 10/11/13
 * Time: 11:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class LazySeqTest extends SequenceTestBase {

    private static class MyLazySeq<T> extends LazySeq<T> {
        MyLazySeq(final List<T> data,  final int cursor) {
            super(data.get(cursor), new $.F0<C.Sequence<T>>() {
                @Override
                public C.Sequence<T> apply() throws NotAppliedException, $.Break {
                    if (cursor < data.size() - 1) {
                        return new MyLazySeq<T>(data, cursor + 1);
                    }
                    return Nil.seq();
                }
            });
        }
    }

    @Override
    protected C.Sequence<Integer> prepareData(final int... ia) {
        return new MyLazySeq<Integer>(Arrays.asList($.asObject(ia)), 0);
    }

    @Override
    protected C.Sequence<Integer> prepareEmptyData() {
        return Nil.list();
    }

    @Override
    protected <T> C.Sequence<T> prepareTypedData(T... ta) {
        return new MyLazySeq<T>(Arrays.asList(ta), 0);
    }
}
