package org.osgl.util;

import org.junit.Test;
import org.osgl.TestBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MapTest extends TestBase {

    @Test
    public void testSerialize() throws Exception {
        C.Map map = C.Map("foo", 1, "bar", 0);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(map);

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        C.Map map2 = (C.Map)ois.readObject();
        eq(map, map2);
    }

    @Test
    public void testFilter() throws Exception {
        C.Map map = C.Map(1, "One", 2, "Two");
        C.Map filtered = map.filter(N.F.IS_EVEN);
        eq(1, filtered.size());
        eq("Two", filtered.get(2));
    }

    @Test
    public void testC_map() throws Exception {
C.Map<String, Integer> map = C.map("one").to(1)
        .map("two").to(2)
        .map("three").to(3);
        eq(3, map.size());
        eq(2, map.get("two"));
    }

}
